/*
 * VMAlloc, Copyright (c) 2018, Miguel Terra-Neves, Vasco Manquinho, Ines Lynce
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package vmalloc.algorithm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Population;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.util.TypedProperties;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.CPUSumAggregator;
import vmalloc.domain.MachineProcessor;
import vmalloc.domain.Mapping;
import vmalloc.domain.MappingVec;
import vmalloc.domain.MemorySumAggregator;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.utils.IOUtils;
import vmalloc.utils.MathUtils;
import vmalloc.utils.MiscUtils;

/**
 * Virtual Machine Consolidation solver that uses the grouping genetic algorithm. Refer to:
 * Xu, Jing, and Jose AB Fortes. "Multi-objective virtual machine placement in virtualized data center
 * environments." Proceedings of the 2010 IEEE/ACM Int'l Conference on Green Computing and Communications &
 * Int'l Conference on Cyber, Physical and Social Computing. IEEE Computer Society, 2010.
 * @author Miguel Terra-Neves
 */
public class GGAAlloc extends EvolutionaryAllocAlgorithm {
    
    /**
     * Class that implements the grouping genetic algorithm for Virtual Machine Consolidation.
     * @author Miguel Terra-Neves
     */
    private class GGA extends AbstractEvolutionaryAlgorithm {
        
        /**
         * Integer constant that represents the null physical machine index.
         */
        private static final int NULL_PM_IDX = -1;
        
        /**
         * Decimal constant that represents the 'beta' factor in the fuzzy evaluation function.
         * @see #fuzzyEvaluation(Solution)
         */
        private static final double BETA = 0.5;
        
        /**
         * Key for the solution attribute used to store the result of the fuzzy evaluation of a solution.
         * @see Solution#setAttribute(String, java.io.Serializable)
         */
        private static final String FUZZY_EVAL_ATR = "e";
        
        /**
         * Key for the solution attribute used to store the groups of a solution.
         * @see #getGroups(Solution)
         */
        private static final String GROUP_ATR = "g";
  
        /**
         * The grouping genetic algorithm performs crossover and mutation on groups instead of variables. A
         * group is composed by a set of variables which have the same value assigned to them in a given
         * solution.
         * @author Miguel Terra-Neves
         */
        private class Group {
            
            /**
             * The index of the group. Variables with an assignment value equal to this index are part of this
             * group.
             */
            private int group_idx;
            
            /**
             * The indexes of the variables with an assigned value equal to {@link #group_idx}.
             */
            private IVecInt var_idxs;
            
            /**
             * The group's score. Represents a combined measure of overall quality of the group considering
             * all 2 or 3 objective functions.
             */
            private double group_eval;
            
            /**
             * Creates an instance of a group.
             * @param group_idx The group's index.
             * @param var_idxs The indexes of the variables with an assigned value equal to {@code group_idx}.
             */
            Group(int group_idx, IVecInt var_idxs) {
                this.var_idxs = var_idxs;
                this.group_idx = group_idx;
                // Compute energy efficiency
                PhysicalMachine group_pm = instance.getPhysicalMachines().get(group_idx);
                VirtualMachineVec group_vms = new VirtualMachineVec();
                for (int i = 0; i < var_idxs.size(); ++i) {
                    group_vms.push(instance.getVirtualMachines().get(i));
                }
                CPUSumAggregator cpu_agr = new CPUSumAggregator();
                group_vms.accept(cpu_agr);
                double cpu_usage = MathUtils.divideBigIntegers(cpu_agr.getCPUSum(),
                                                               group_pm.getCPU(),
                                                               RoundingMode.HALF_EVEN).doubleValue();
                double energy_efficiency = cpu_usage*group_pm.getMaxConsumption();
                energy_efficiency /= cpu_usage * (group_pm.getMaxConsumption() -
                                                  group_pm.getIdleConsumption()) +
                                     group_pm.getIdleConsumption();
                // Compute resource usage efficiency
                MemorySumAggregator mem_agr = new MemorySumAggregator();
                group_vms.accept(mem_agr);
                double mem_usage = MathUtils.divideBigIntegers(mem_agr.getMemorySum(),
                                                               group_pm.getMemory(),
                                                               RoundingMode.HALF_EVEN).doubleValue();
                double usage_efficiency = cpu_usage * mem_usage;
                this.group_eval = energy_efficiency + usage_efficiency;
                // Compute migration efficiency if required
                if (problem.getNumberOfObjectives() > 2) {
                    BigInteger inside_migged_mem = BigInteger.ZERO;
                    for (int i = 0; i < var_idxs.size(); ++i) {
                        int var_idx = var_idxs.get(i), mapped_pm_idx = mapped_pm_idxs.get(var_idx);
                        if (mapped_pm_idx != NULL_PM_IDX && mapped_pm_idx != group_idx) {
                            inside_migged_mem = inside_migged_mem.add(group_vms.get(i).getMemory());
                        }
                    }
                    BigInteger mapped_outside_mem =
                            total_mapped_mem.subtract(mapped_mems.get(group_idx));
                    double mig_efficiency =
                            MathUtils.divideBigIntegers(mapped_outside_mem.subtract(inside_migged_mem),
                                                        mapped_outside_mem,
                                                        RoundingMode.HALF_EVEN).doubleValue();
                    assert(mig_efficiency >= 0.0 && mig_efficiency <= 1.0);
                    this.group_eval += mig_efficiency;
                    this.group_eval /= 3;
                }
                else {
                    this.group_eval /= 2;
                }
            }
            
            /**
             * Retrieves the number of variables in the group.
             * @return The number of variables in the group.
             */
            public int getGroupSize() { return this.var_idxs.size(); }

            /**
             * Retrieves the index of a variable that is a part of the group.
             * @param i The position of the variable's index in the group.
             * @return The variable's index.
             */
            public int getVariableIndex(int i) { return this.var_idxs.get(i); }
            
            /**
             * Retrieves the group's index, i.e., the value assigned to all variables in the group.
             * @return The group's index.
             */
            public int getGroupIndex() { return this.group_idx; }
            
            /**
             * Retrieves the group's score.
             * @return The group's score.
             */
            public double getEvaluation() { return this.group_eval; }
            
        }
        
        /**
         * The mutation rate. In the grouping genetic algorithm, the mutation rate is the fraction of the
         * population that is randomly mutated at each generation.
         */
        private double mutation_rate;
        
        /**
         * The crossover rate. In the grouping genetic algorithm, the crossover rate is the number of
         * crossover operations, applied at each generation, as a fraction of the population size.
         */
        private double crossover_rate;
        
        /**
         * A lower bound of the energy consumption costs. Used in fuzzy membership computation.
         * @see #lowEnergyMembership(Solution)
         */
        private final double energy_lb;
        
        /**
         * An upper bound of the energy consumption costs. Used in fuzzy membership computation.
         * @see #lowEnergyMembership(Solution)
         */
        private final double energy_ub;
        
        /**
         * A lower bound of the resource wastage costs. Used in fuzzy membership computation.
         * @see #lowWastageMembership(Solution)
         */
        private final double wastage_lb;
        
        /**
         * An upper bound of the resource wastage costs. Used in fuzzy membership computation.
         * @see #lowWastageMembership(Solution)
         */
        private final double wastage_ub;
        
        /**
         * A lower bound of the migration costs. Used in fuzzy membership computation.
         * @see #fewMigrationsMembership(Solution)
         */
        private final BigInteger migration_lb;
        
        /**
         * An upper bound of the migration costs. Used in fuzzy membership computation.
         * @see #fewMigrationsMembership(Solution)
         */
        private final BigInteger migration_ub;
        
        /**
         * Vector that stores, for each virtual machine, the index of the physical machine mapped to it in the
         * pre-existing allocation. If a virtual machine is not mapped in the pre-existing allocation, then
         * its entry is filled with {@link #NULL_PM_IDX}.
         */
        private IVecInt mapped_pm_idxs;
        
        /**
         * Vector that stores, for each physical machine, the sum of the memory requirements of the virtual
         * machines mapped to it in the pre-existing allocation.
         */
        private IVec<BigInteger> mapped_mems;
        
        /**
         * Stores the sum of the memory requirements of the virtual machines mapped to some physical machine
         * in the pre-existing allocation.
         */
        private BigInteger total_mapped_mem;
        
        /**
         * A vector of vectors of physical machine indexes. Each sub-vector stores a set of indexes that
         * correspond to physical machines with equal resource capacities (both CPU and memory).
         */
        private IVec<IVecInt> pm_groups; // FIXME: Map would be more efficient, but should work fine for few distinct PM types
        
        /**
         * Bin-packing allocation algorithm used as a sub-routine in the mutation and crossover operators of
         * the grouping genetic algorithm.
         * @see {@link #mutate(Solution)} {@link #rankingCrossover(Solution, Solution)}
         */
        private BinPackingAllocAlgorithm bp_alloc;
        
        /**
         * Creates an instance of the grouping genetic algorithm.
         * @param problem The problem instance.
         * @param population The container for the population of each generation.
         * @param mutation_rate The mutation rate.
         * @param crossover_rate The crossover rate.
         * @param initialization The population initialization operator.
         */
        public GGA(Problem problem,
                   Population population,
                   double mutation_rate,
                   double crossover_rate,
                   Initialization initialization) {
            super(problem, population, null, initialization);
            assert(mutation_rate >= 0.0 && mutation_rate <= 1.0);
            this.mutation_rate = mutation_rate;
            this.crossover_rate = crossover_rate;
            this.bp_alloc = new FirstFitDecreasingAlloc(instance);
            this.bp_alloc.setMaxMemoryMigrationPercentile(0.0);
            this.bp_alloc.enableVirtualMachineShuffling();
            // Compute lower bounds and upper bounds for fuzzy membership computation
            this.energy_lb = energyLowerBound();
            this.energy_ub = energyUpperBound();
            this.wastage_lb = wastageLowerBound();
            this.wastage_ub = wastageUpperBound();
            assert(this.energy_lb < this.energy_ub);
            assert(this.wastage_lb < this.wastage_ub);
            if (problem.getNumberOfObjectives() > 2) {
                this.migration_lb = migrationLowerBound();
                this.migration_ub = migrationUpperBound();
                assert(this.migration_lb.compareTo(this.migration_ub) < 0);
            }
            else {
                this.migration_lb = this.migration_ub = null;
            }
            // Initialize auxiliary structure with sets of VMs mapped to each PM if required
            if (problem.getNumberOfObjectives() > 2) {
                this.mapped_pm_idxs = new VecInt();
                for (int i = 0; i < instance.getVirtualMachines().size(); ++i) {
                    this.mapped_pm_idxs.push(NULL_PM_IDX);
                }
                this.mapped_mems = new Vec<BigInteger>();
                this.total_mapped_mem = BigInteger.ZERO;
                for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
                    this.mapped_mems.push(BigInteger.ZERO);
                }
                for (int i = 0; i < instance.getMappings().size(); ++i) {
                    PhysicalMachine pm = instance.getMappings().get(i).getPhysicalMachine();
                    VirtualMachine vm = instance.getMappings().get(i).getVirtualMachine();
                    int vm_idx = instance.getVirtualMachineIndex(vm);
                    int pm_idx = instance.getPhysicalMachineIndex(pm);
                    assert(this.mapped_pm_idxs.get(vm_idx) == NULL_PM_IDX);
                    this.mapped_pm_idxs.set(vm_idx, pm_idx);
                    this.mapped_mems.set(pm_idx, this.mapped_mems.get(pm_idx).add(vm.getMemory()));
                    this.total_mapped_mem = this.total_mapped_mem.add(vm.getMemory());
                }
            }
            // Split PM indexes into groups with equal resource capacities
            this.pm_groups = new Vec<IVecInt>();
            for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
                PhysicalMachine pm = instance.getPhysicalMachines().get(i);
                int j;
                for (j = 0; j < this.pm_groups.size(); ++j) {
                    PhysicalMachine rep_pm =
                            instance.getPhysicalMachines().get(this.pm_groups.get(j).get(0));
                    if (pm.getCPU().equals(rep_pm.getCPU()) && pm.getMemory().equals(rep_pm.getMemory())) {
                        break;
                    }
                }
                if (j >= this.pm_groups.size()) {
                    this.pm_groups.push(new VecInt());
                }
                assert(j < this.pm_groups.size());
                this.pm_groups.get(j).push(i);
            }
        }
        
        /**
         * Computes the resource efficiency in terms of energy consumption.
         * @param cap The resource capacity value of a physical machine.
         * @param max_consumpt The maximum energy consumption of a physical machine.
         * @param idle_consumpt The idle energy consumption of a physical machine.
         * @return The resource efficiency.
         */
        private BigDecimal computeEfficiency(BigInteger cap, int max_consumpt, int idle_consumpt) {
            return MathUtils.divideBigIntegers(cap,
                                               BigInteger.valueOf(max_consumpt - idle_consumpt),
                                               RoundingMode.HALF_DOWN);
        }
        
        /**
         * Computes the CPU efficiency of a physical machine in terms of energy consumption.
         * @param pm The physical machine.
         * @return The physical machine's CPU efficiency.
         */
        private BigDecimal cpuEfficiencyOf(PhysicalMachine pm) {
            return computeEfficiency(pm.getCPU(), pm.getMaxConsumption(), pm.getIdleConsumption());
        }
        
        /**
         * Computes the memory efficiency of a physical machine in terms of energy consumption.
         * @param pm The physical machine.
         * @return The physical machine's memory efficiency.
         */
        private BigDecimal memEfficiencyOf(PhysicalMachine pm) {
            return computeEfficiency(pm.getMemory(), pm.getMaxConsumption(), pm.getIdleConsumption());
        }
        
        /**
         * Makes a resource efficiency comparator for physical machines.
         * @param compare_mem True if a memory efficiency comparator is required, false if a CPU efficiency
         * comparator is needed instead.
         * @param decreasing True if sorting using the comparator should produce a decreasing order, false
         * if it should produce an increasing order.
         * @return The resource efficiency comparator.
         */
        private Comparator<PhysicalMachine> makePhysicalMachineEfficiencyComparator(boolean compare_mem,
                                                                                    boolean decreasing) {
            return new Comparator<PhysicalMachine>() {
                private boolean compare_mem;
                private boolean decreasing;
                public Comparator<PhysicalMachine> init(boolean compare_mem, boolean decreasing) {
                    this.compare_mem = compare_mem;
                    this.decreasing = decreasing;
                    return this;
                }
                private BigDecimal efficiencyOf(PhysicalMachine pm) {
                    return (compare_mem) ? memEfficiencyOf(pm) : cpuEfficiencyOf(pm);
                }
                public int compare(PhysicalMachine pm1, PhysicalMachine pm2) {
                    int diff = efficiencyOf(pm1).compareTo(efficiencyOf(pm2));
                    return (decreasing) ? -diff : diff;
                }
            }.init(compare_mem, decreasing);
        }
        
        /**
         * Makes an increasing order CPU efficiency comparator for physical machines.
         * @return The increasing CPU efficiency comparator.
         */
        private Comparator<PhysicalMachine> makeIncreasingPhysicalMachineCPUEfficiencyComparator() {
            return makePhysicalMachineEfficiencyComparator(false, false);
        }
        
        /**
         * Makes a decreasing order CPU efficiency comparator for physical machines.
         * @return The decreasing CPU efficiency comparator.
         */
        private Comparator<PhysicalMachine> makeDecreasingPhysicalMachineCPUEfficiencyComparator() {
            return makePhysicalMachineEfficiencyComparator(false, true);
        }
        
        /**
         * Makes an increasing order memory efficiency comparator for physical machines.
         * @return The increasing memory efficiency comparator.
         */
        private Comparator<PhysicalMachine> makeIncreasingPhysicalMachineMemoryEfficiencyComparator() {
            return makePhysicalMachineEfficiencyComparator(true, false);
        }
        
        /**
         * Makes a decreasing order memory efficiency comparator for physical machines.
         * @return The decreasing memory efficiency comparator.
         */
        private Comparator<PhysicalMachine> makeDecreasingPhysicalMachineMemoryEfficiencyComparator() {
            return makePhysicalMachineEfficiencyComparator(true, true);
        }
        
        /**
         * Retrieves the smallest physical machine from a vector according to a given comparison criteria. 
         * @param pms The physical machines.
         * @param comp The comparator that implements the criteria.
         * @return The smallest physical machine in {@code pms} according to {@code comp}.
         */
        private PhysicalMachine getSmallestPhysicalMachine(PhysicalMachineVec pms,
                                                           Comparator<PhysicalMachine> comp) {
            PhysicalMachine smallest_pm = pms.get(0);
            for (int i = 1; i < pms.size(); ++i) {
                if (comp.compare(pms.get(i), smallest_pm) < 0) {
                    smallest_pm = pms.get(i);
                }
            }
            return smallest_pm;
        }
        
        /**
         * Retrieves the most CPU efficient physical machine from a vector.
         * @param pms The physical machines.
         * @return The most CPU efficient physical machine.
         */
        private PhysicalMachine getMostCPUEfficientPhysicalMachine(PhysicalMachineVec pms) {
            return getSmallestPhysicalMachine(pms, makeDecreasingPhysicalMachineCPUEfficiencyComparator());
        }
        
        /**
         * Retrieves the least CPU efficient physical machine from a vector.
         * @param pms The physical machines.
         * @return The least CPU efficient physical machine.
         */
        private PhysicalMachine getLeastCPUEfficientPhysicalMachine(PhysicalMachineVec pms) {
            return getSmallestPhysicalMachine(pms, makeIncreasingPhysicalMachineCPUEfficiencyComparator());
        }
        
        /**
         * Retrieves the most memory efficient physical machine from a vector.
         * @param pms The physical machines.
         * @return The most memory efficient physical machine.
         */
        private PhysicalMachine getMostMemoryEfficientPhysicalMachine(PhysicalMachineVec pms) {
            return getSmallestPhysicalMachine(pms, makeDecreasingPhysicalMachineMemoryEfficiencyComparator());
        }
        
        /**
         * Retrieves the least memory efficient physical machine from a vector.
         * @param pms The physical machines.
         * @return The least memory efficient physical machine.
         */
        private PhysicalMachine getLeastMemoryEfficientPhysicalMachine(PhysicalMachineVec pms) {
            return getSmallestPhysicalMachine(pms, makeIncreasingPhysicalMachineMemoryEfficiencyComparator());
        }
        
        /**
         * Makes an idle consumption comparator for physical machines.
         * @param decreasing True if sorting using the comparator should produce a decreasing order, false
         * if it should produce an increasing order.
         * @return The idle consumption comparator.
         */
        private Comparator<PhysicalMachine> makePhysicalMachineIdleConsumptionComparator(boolean decreasing) {
            return new Comparator<PhysicalMachine>() {
                private boolean decreasing;
                public Comparator<PhysicalMachine> init(boolean decreasing) {
                    this.decreasing = decreasing;
                    return this;
                }
                public int compare(PhysicalMachine pm1, PhysicalMachine pm2) {
                    int diff = pm1.getIdleConsumption() - pm2.getIdleConsumption();
                    return (decreasing) ? -diff : diff;
                }
            }.init(decreasing);
        }
        
        /**
         * Makes an increasing order idle consumption comparator for physical machines.
         * @return The increasing idle consumption comparator.
         */
        private Comparator<PhysicalMachine> makeIncreasingPhysicalMachineIdleConsumptionComparator() {
            return makePhysicalMachineIdleConsumptionComparator(false);
        }
        
        /**
         * Makes a decreasing order idle consumption comparator for physical machines.
         * @return The decreasing idle consumption comparator.
         */
        private Comparator<PhysicalMachine> makeDecreasingPhysicalMachineIdleConsumptionComparator() {
            return makePhysicalMachineIdleConsumptionComparator(true);
        }
        
        /**
         * Estimates the number of copies of a given reference physical machine needed to accommodate the
         * full virtual machine requirements of a given resource.
         * @param ref_pm The reference physical machine.
         * @param overestimate True if the estimate should be an overestimation, false if it should be an
         * underestimation.
         * @param use_mem True if an estimate based on memory is desired, false if CPU is desired instead.
         * @return An estimate of the number of copies of {@code ref_pm} needed.
         */
        private int estimatePhysicalMachineCopiesNeeded(PhysicalMachine ref_pm,
                                                        boolean overestimate, boolean use_mem) {
            RoundingMode round_mode = (overestimate) ? RoundingMode.CEILING : RoundingMode.FLOOR;
            BigDecimal estimate = MathUtils.divideBigIntegers((use_mem) ? instance.getTotalMemoryRequirements() :
                                                                          instance.getTotalCPURequirements(),
                                                              (use_mem) ? ref_pm.getMemory() : ref_pm.getCPU(),
                                                              round_mode);
            return (overestimate) ? (int)MathUtils.ceil(estimate).longValueExact() :
                                    (int)MathUtils.floor(estimate).longValueExact();
        }
        
        /**
         * Underestimates the number of copies of a given reference physical machine needed to accommodate the
         * full virtual machine CPU requirements.
         * @param ref_pm The reference physical machine.
         * @return An underestimation of the number of copies of {@code ref_pm} needed.
         */
        private int underEstimatePhysicalMachineCopiesNeededForCPU(PhysicalMachine ref_pm) {
            return estimatePhysicalMachineCopiesNeeded(ref_pm, false, false);
        }
        
        /**
         * Overestimates the number of copies of a given reference physical machine needed to accommodate the
         * full virtual machine CPU requirements.
         * @param ref_pm The reference physical machine.
         * @return An overestimation of the number of copies of {@code ref_pm} needed.
         */
        private int overEstimatePhysicalMachineCopiesNeededForCPU(PhysicalMachine ref_pm) {
            return estimatePhysicalMachineCopiesNeeded(ref_pm, true, false);
        }
        
        /**
         * Underestimates the number of copies of a given reference physical machine needed to accommodate the
         * full virtual machine memory requirements.
         * @param ref_pm The reference physical machine.
         * @return An underestimation of the number of copies of {@code ref_pm} needed.
         */
        private int underEstimatePhysicalMachineCopiesNeededForMemory(PhysicalMachine ref_pm) {
            return estimatePhysicalMachineCopiesNeeded(ref_pm, false, true);
        }
        
        /**
         * Overestimates the number of copies of a given reference physical machine needed to accommodate the
         * full virtual machine memory requirements.
         * @param ref_pm The reference physical machine.
         * @return An overestimation of the number of copies of {@code ref_pm} needed.
         */
        private int overEstimatePhysicalMachineCopiesNeededForMemory(PhysicalMachine ref_pm) {
            return estimatePhysicalMachineCopiesNeeded(ref_pm, true, true);
        }
        
        /**
         * A machine processor class that aggregates the sum of the idle consumptions of the physical machines
         * that it processes.
         * @author Miguel Terra-Neves
         */
        class IdleConsumptionAggregator extends MachineProcessor {
            
            /**
             * The sum of the idle consumptions.
             */
            private long sum = 0;
            
            @Override
            public void processPhysicalMachine(PhysicalMachine pm) {
                this.sum += pm.getIdleConsumption();
            }
            
            /**
             * Retrieves the sum of the idle consumptions of the physical machines that were processed.
             * @return The idle consumption sum.
             */
            public long getIdleConsumptionSum() { return this.sum; }
            
        }
        
        /**
         * Computes the sum of the idle consumptions of the physical machines in an array.
         * @param pms The physical machines.
         * @return The sum of the idle consumptions of the physical machines in {@code pms}.
         */
        private long idleConsumptionSum(PhysicalMachine[] pms) {
            IdleConsumptionAggregator idle_aggr = new IdleConsumptionAggregator();
            new PhysicalMachineVec(pms).accept(idle_aggr);
            return idle_aggr.getIdleConsumptionSum();
        }
        
        /**
         * Computes a bound of energy consumption costs.
         * @param is_upper True if an upper bound is desired, false if a lower bound is needed instead.
         * @return The energy consumption bound.
         */
        private double energyBound(boolean is_upper) {
            PhysicalMachineVec pms = instance.getPhysicalMachines();
            PhysicalMachine cpu_ref_pm = null, mem_ref_pm = null;
            PhysicalMachine[] sorted_pms = null;
            int cpu_npms, mem_npms;
            if (is_upper) {
                cpu_ref_pm = getLeastCPUEfficientPhysicalMachine(pms);
                mem_ref_pm = getLeastMemoryEfficientPhysicalMachine(pms);
                sorted_pms =
                        sortedPhysicalMachines(pms, makeDecreasingPhysicalMachineIdleConsumptionComparator());
                cpu_npms = Math.min(pms.size(), overEstimatePhysicalMachineCopiesNeededForCPU(cpu_ref_pm));
                mem_npms = Math.min(pms.size(), overEstimatePhysicalMachineCopiesNeededForMemory(mem_ref_pm));
            }
            else {
                cpu_ref_pm = getMostCPUEfficientPhysicalMachine(pms);
                mem_ref_pm = getMostMemoryEfficientPhysicalMachine(pms);
                sorted_pms =
                        sortedPhysicalMachines(pms, makeIncreasingPhysicalMachineIdleConsumptionComparator());
                cpu_npms = underEstimatePhysicalMachineCopiesNeededForCPU(cpu_ref_pm);
                mem_npms = underEstimatePhysicalMachineCopiesNeededForMemory(mem_ref_pm);
            }
            int cpu_consumpt_diff = cpu_ref_pm.getMaxConsumption() - cpu_ref_pm.getIdleConsumption();
            double cpu_bound = cpu_npms * (double)cpu_consumpt_diff +
                               (double)idleConsumptionSum(Arrays.copyOfRange(sorted_pms, 0, cpu_npms));
            int mem_consumpt_diff = mem_ref_pm.getMaxConsumption() - mem_ref_pm.getIdleConsumption();
            double mem_bound = mem_npms * (double)mem_consumpt_diff +
                               (double)idleConsumptionSum(Arrays.copyOfRange(sorted_pms, 0, mem_npms));
            return (is_upper) ? Math.min(Math.max(cpu_bound, mem_bound), MathUtils.sum(pms.getMaxConsumptions())) :
                                Math.max(cpu_bound, mem_bound);
        }
        
        /**
         * Computes a lower bound of energy consumption costs.
         * @return The energy consumption lower bound.
         */
        private double energyLowerBound() { return energyBound(false); }
        
        /**
         * Computes an upper bound of energy consumption costs.
         * @return The energy consumption upper bound.
         */
        private double energyUpperBound() { return energyBound(true); }
        
        /**
         * Makes a resource allocation imbalance comparator for physical machines. A physical machine's
         * resource allocation imbalance is a measure of how unbalanced the the use of its resources would be
         * if only copies of that physical machine were available to accommodate the virtual machines in the
         * instance.
         * @param decreasing True if sorting using the comparator should produce a decreasing order, false
         * if it should produce an increasing order.
         * @return The imbalance comparator.
         */
        private Comparator<PhysicalMachine> makePhysicalMachineAllocationImbalanceComparator(
                boolean decreasing) {
            return new Comparator<PhysicalMachine>() {
                private boolean decreasing;
                public Comparator<PhysicalMachine> init(boolean decreasing) {
                    this.decreasing = decreasing;
                    return this;
                }
                public int compare(PhysicalMachine pm1, PhysicalMachine pm2) {
                    BigDecimal pm1_imbalance =
                            MathUtils.divideBigIntegers(instance.getTotalCPURequirements(),
                                                        pm1.getCPU(),
                                                        RoundingMode.HALF_EVEN).subtract(
                            MathUtils.divideBigIntegers(instance.getTotalMemoryRequirements(),
                                                        pm1.getMemory(),
                                                        RoundingMode.HALF_EVEN)).abs();
                    BigDecimal pm2_imbalance =
                            MathUtils.divideBigIntegers(instance.getTotalCPURequirements(),
                                                        pm2.getCPU(),
                                                        RoundingMode.HALF_EVEN).subtract(
                            MathUtils.divideBigIntegers(instance.getTotalMemoryRequirements(),
                                                        pm2.getMemory(),
                                                        RoundingMode.HALF_EVEN)).abs();
                    int diff = pm1_imbalance.compareTo(pm2_imbalance);
                    return (decreasing) ? -diff : diff;
                }
            }.init(decreasing);
        }
        
        /**
         * Makes a decreasing resource allocation imbalance comparator for physical machines. A physical
         * machine's resource allocation imbalance is a measure of how unbalanced the the use of its resources
         * would be if only copies of that physical machine were available to accommodate the virtual machines
         * in the instance.
         * @return The decreasing imbalance comparator.
         */
        private Comparator<PhysicalMachine> makeDecreasingPhysicalMachineAllocationImbalanceComparator() {
            return makePhysicalMachineAllocationImbalanceComparator(true);
        }
        
        /**
         * Makes an increasing resource allocation imbalance comparator for physical machines. A physical
         * machine's resource allocation imbalance is a measure of how unbalanced the the use of its resources
         * would be if only copies of that physical machine were available to accommodate the virtual machines
         * in the instance.
         * @return The increasing imbalance comparator.
         */
        private Comparator<PhysicalMachine> makeIncreasingPhysicalMachineAllocationImbalanceComparator() {
            return makePhysicalMachineAllocationImbalanceComparator(false);
        }
        
        /**
         * Retrieves the least resource allocation unbalanced physical machine from a vector.
         * @param pms The physical machine.
         * @return The least unbalanced physical machine.
         */
        private PhysicalMachine getLeastAllocationImbalancedPhysicalMachine(PhysicalMachineVec pms) {
            return getSmallestPhysicalMachine(
                    pms, makeIncreasingPhysicalMachineAllocationImbalanceComparator());
        }
        
        /**
         * Retrieves the most resource allocation unbalanced physical machine from a vector.
         * @param pms The physical machine.
         * @return The most unbalanced physical machine.
         */
        private PhysicalMachine getMostAllocationImbalancedPhysicalMachine(PhysicalMachineVec pms) {
            return getSmallestPhysicalMachine(
                    pms, makeDecreasingPhysicalMachineAllocationImbalanceComparator());
        }
        
        /**
         * Computes a lower bound of resource wastage costs.
         * @return The resource wastage lower bound.
         */
        private double wastageLowerBound() {
            PhysicalMachine best_pm =
                    getLeastAllocationImbalancedPhysicalMachine(instance.getPhysicalMachines());
            double[] cpu_reqs = getNormalizedCPURequirements(instance.getVirtualMachines(), best_pm);
            double[] mem_reqs = getNormalizedMemoryRequirements(instance.getVirtualMachines(), best_pm);
            return Math.abs(MathUtils.sum(cpu_reqs) - MathUtils.sum(mem_reqs));
        }
        
        /**
         * Computes an upper bound of resource wastage costs.
         * @return The resource wastage upper bound.
         */
        private double wastageUpperBound() {
            PhysicalMachine worst_pm =
                    getMostAllocationImbalancedPhysicalMachine(instance.getPhysicalMachines());
            double[] cpu_reqs = getNormalizedCPURequirements(instance.getVirtualMachines(), worst_pm);
            double[] mem_reqs = getNormalizedMemoryRequirements(instance.getVirtualMachines(), worst_pm);
            double ub = 0.0;
            for (int i = 0; i < instance.getVirtualMachines().size(); ++i) {
                ub += Math.abs(cpu_reqs[i] - mem_reqs[i]);
            }
            return ub;
        }
        
        /**
         * Computes a lower bound of migration costs.
         * @return The migration cost lower bound.
         */
        private BigInteger migrationLowerBound() { return BigInteger.ZERO; }
        
        /**
         * Computes an upper bound of migration costs.
         * @return The migration cost upper bound.
         */
        private BigInteger migrationUpperBound() {
            VirtualMachineVec mapped_vms = new VirtualMachineVec();
            for (int i = 0; i < instance.getMappings().size(); ++i) {
                mapped_vms.push(instance.getMappings().get(i).getVirtualMachine());
            }
            MemorySumAggregator mem_agr = new MemorySumAggregator();
            mem_agr.processVirtualMachines(mapped_vms);
            return mem_agr.getMemorySum();
        }
        
        /**
         * Converts a vector of groups to an array.
         * @param groups The group vector.
         * @return The same groups as an array.
         */
        private Group[] groupVecToArray(IVec<Group> groups) {
            Group[] group_array = new Group[groups.size()];
            for (int i = 0; i < groups.size(); ++i) {
                group_array[i] = groups.get(i);
            }
            return group_array;
        }
        
        /**
         * Computes the set of groups of a solution.
         * @param solution The solution.
         * @return The solution's groups in an array.
         */
        private Group[] computeGroups(Solution solution) {
            int[] x = instance.getVirtualMachineAssignment(solution);
            IVec<IVecInt> var_idxs_per_group = new Vec<IVecInt>();
            for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
                var_idxs_per_group.push(new VecInt());
            }
            for (int i = 0; i < solution.getNumberOfVariables(); ++i) {
                int pm_idx = x[i];
                var_idxs_per_group.get(pm_idx).push(i);
            }
            IVec<Group> groups = new Vec<Group>();
            for (int i = 0; i < var_idxs_per_group.size(); ++i) {
                IVecInt var_idxs = var_idxs_per_group.get(i);
                if (var_idxs.size() > 0) {
                    groups.push(new Group(i, var_idxs));
                }
            }
            return groupVecToArray(groups);
        }
        
        /**
         * Builds a solution from the allocation produced by the execution of a bin-packing algorithm. If a
         * partial allocation was found, the leftover virtual machines are placed randomly. It is assumed that
         * the {@link BinPackingAllocAlgorithm#allocate()} method was called prior to this method.
         * @param bp The bin-packing algorithm.
         * @return The solution for the allocation produced by {@code bp}.
         */
        private Solution binPackingResultToSolution(BinPackingAllocAlgorithm bp) {
            MappingVec allocation = null;
            VirtualMachineVec leftover_vms = null;
            if (bp.foundSolution()) {
                allocation = bp.getAllocation();
                leftover_vms = new VirtualMachineVec();
            }
            else {
                allocation = bp.getPartialAllocation();
                leftover_vms = bp.getLeftoverVirtualMachines();
            }
            assert(allocation.size() + leftover_vms.size() == instance.getVirtualMachines().size());
            Solution solution = problem.newSolution();
            MiscUtils.injectPartialAllocationInSolution(instance, solution, allocation, leftover_vms);
            return solution;
        }
        
        /**
         * Performs mutation on a solution, producing a new solution. Mutation in the grouping genetic
         * algorithm deletes one of the groups of the solution and re-packs the virtual machines in that
         * group.
         * @param parent The solution.
         * @return The mutated solution.
         */
        private Solution mutate(Solution parent) {
            Group[] groups = getGroups(parent);
            int del_idx = PRNG.nextInt(groups.length);
            MappingVec placement = new MappingVec();
            for (int i = 0; i < groups.length; ++i) {
                if (i != del_idx) {
                    Group group = groups[i];
                    for (int j = 0; j < group.getGroupSize(); ++j) {
                        placement.push(
                                new Mapping(instance.getVirtualMachines().get(group.getVariableIndex(j)),
                                            instance.getPhysicalMachines().get(group.getGroupIndex())));
                    }
                }
            }
            this.bp_alloc.setMappings(placement);
            IOUtils.stdoutDisable();
            this.bp_alloc.allocate();
            IOUtils.stdoutEnable();
            return binPackingResultToSolution(this.bp_alloc);
        }
        
        /**
         * Performs crossover on two solutions, producing a new solution. Crossover in the grouping genetic
         * algorithm is performed by considering the parents' groups in increasing order of their scores and
         * injecting them into the new solution.
         * @param parent1 A solution.
         * @param parent2 Another solution.
         * @return The new solution.
         */
        private Solution rankingCrossover(Solution parent1, Solution parent2) {
            // Sort group vectors in decreasing order of overall efficiency
            Group[] groups1 = getGroups(parent1), groups2 = getGroups(parent2);
            Comparator<Group> comp = new Comparator<Group>() {
                public int compare(Group g1, Group g2) {
                    return Double.compare(g2.getEvaluation(), g1.getEvaluation());
                }
            };
            Arrays.sort(groups1, comp);
            Arrays.sort(groups2, comp);
            // Perform group crossover in decreasing order of efficiency
            Set<Integer> placed_var_idxs = new HashSet<Integer>();
            Set<Integer> used_group_idxs = new HashSet<Integer>();
            IVec<Group> offspring_groups = new Vec<Group>();
            for (int i = 0, j = 0; i < groups1.length || j < groups2.length;) {
                // Select next group to inject into offspring
                Group group_to_inject = null;
                if (    i < groups1.length &&
                        (j >= groups2.length ||
                         groups1[i].getEvaluation() > groups2[j].getEvaluation())) {
                    group_to_inject = groups1[i++];
                }
                else {
                    group_to_inject = groups2[j++];
                }
                // Inject group
                int new_group_idx = group_to_inject.getGroupIndex();
                if (used_group_idxs.contains(new Integer(new_group_idx))) {
                    // Retrieve PM group with equal capacities
                    IVecInt pm_group = null;
                    PhysicalMachine pm = instance.getPhysicalMachines().get(new_group_idx);
                    for (int k = 0; pm_group == null; ++k) {
                        PhysicalMachine rep_pm =
                                instance.getPhysicalMachines().get(this.pm_groups.get(k).get(0));
                        if (    pm.getCPU().equals(rep_pm.getCPU()) &&
                                pm.getMemory().equals(rep_pm.getMemory())) {
                            pm_group = this.pm_groups.get(k);
                        }
                    }
                    // Select an equal empty PM
                    int k;
                    for (k = 0; k < pm_group.size(); ++k) {
                        int candidate_idx = pm_group.get(k);
                        if (!used_group_idxs.contains(new Integer(candidate_idx))) {
                            new_group_idx = candidate_idx;
                            break;
                        }
                    }
                }
                if (!used_group_idxs.contains(new Integer(new_group_idx))) {
                    IVecInt new_var_idxs = new VecInt();
                    for (int k = 0; k < group_to_inject.getGroupSize(); ++k) {
                        int var_idx = group_to_inject.getVariableIndex(k);
                        if (!placed_var_idxs.contains(var_idx)) {
                            new_var_idxs.push(var_idx);
                        }
                    }
                    if (new_var_idxs.size() > 0) {
                        offspring_groups.push(new Group(new_group_idx, new_var_idxs));
                        used_group_idxs.add(new Integer(new_group_idx));
                    }
                }
                
            }
            // Insert leftover VMs using first fit
            MappingVec placement = new MappingVec();
            for (int i = 0; i < offspring_groups.size(); ++i) {
                Group group = offspring_groups.get(i);
                for (int j = 0; j < group.getGroupSize(); ++j) {
                    placement.push(
                            new Mapping(instance.getVirtualMachines().get(group.getVariableIndex(j)),
                                        instance.getPhysicalMachines().get(group.getGroupIndex())));
                }
            }
            this.bp_alloc.setMappings(placement);
            IOUtils.stdoutDisable();
            this.bp_alloc.allocate();
            IOUtils.stdoutEnable();
            return binPackingResultToSolution(this.bp_alloc);
        }
        
        /**
         * Computes the degree of membership (the higher the better) of a solution in the low energy
         * consumption category.
         * It assumes that the solution has been evaluated.
         * @param sol The solution.
         * @return The degree of membership in low energy consumption.
         * @see VMCwMProblem#evaluate(Solution)
         */
        private double lowEnergyMembership(Solution sol) {
            double energy_cost = Math.min(VMCwMProblem.getEnergyCost(sol), this.energy_ub);
            assert(energy_cost >= this.energy_lb); // energy_cost may be higher than UB if constraints are violated
            return 1.0 - ((energy_cost - this.energy_lb) / (this.energy_ub - this.energy_lb));
        }
        
        /**
         * Computes the degree of membership (the higher the better) of a solution in the low resource
         * wastage category. Note that resource wastage in the grouping genetic algorithm is computed
         * as in the algorithm's paper, and is different from {@link VMCwMProblem#getWastageCost(Solution)}.
         * @param sol The solution.
         * @return The degree of membership in low resource wastage.
         */
        private double lowWastageMembership(Solution sol) {
            BigInteger[] used_cpu_caps = new BigInteger[instance.getPhysicalMachines().size()];
            BigInteger[] used_mem_caps = new BigInteger[instance.getPhysicalMachines().size()];
            for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
                used_cpu_caps[i] = BigInteger.ZERO;
                used_mem_caps[i] = BigInteger.ZERO;
            }
            int[] x = instance.getVirtualMachineAssignment(sol);
            for (int i = 0; i < instance.getVirtualMachines().size(); ++i) {
                int pm_idx = x[i];
                used_cpu_caps[pm_idx] =
                        used_cpu_caps[pm_idx].add(instance.getVirtualMachines().get(i).getCPU());
                used_mem_caps[pm_idx] =
                        used_mem_caps[pm_idx].add(instance.getVirtualMachines().get(i).getMemory());
            }
            double total_wastage = 0.0;
            for (int i = 0; i < instance.getPhysicalMachines().size(); ++i) {
                if (    !used_cpu_caps[i].equals(BigInteger.ZERO) && 
                        !used_mem_caps[i].equals(BigInteger.ZERO)) {
                    double used_cpu = used_cpu_caps[i].doubleValue()/
                                      instance.getPhysicalMachines().get(i).getCPU().doubleValue();
                    double used_mem = used_mem_caps[i].doubleValue()/
                                      instance.getPhysicalMachines().get(i).getMemory().doubleValue();
                    double cpu_left = 1.0 - used_cpu;
                    double mem_left = 1.0 - used_mem;
                    total_wastage += Math.abs(cpu_left-mem_left);
                }
            }
            total_wastage = Math.min(total_wastage, this.wastage_ub);
            assert(total_wastage >= this.wastage_lb); // wastage may be higher than UB if constraints are violated
            return 1.0 - ((total_wastage - this.wastage_lb) / (this.wastage_ub - this.wastage_lb));
        }
        
        /**
         * Computes the degree of membership (the larger the better) of a solution in the low migration
         * cost category.
         * It assumes that the solution has been evaluated.
         * @param sol The solution.
         * @return The degree of membership in low migration cost.
         * @see VMCwMProblem#evaluate(Solution)
         */
        private double fewMigrationsMembership(Solution sol) {
            BigDecimal mig_cost = new BigDecimal(VMCwMProblem.getMigrationCost(sol));
            assert(mig_cost.compareTo(new BigDecimal(this.migration_lb)) >= 0 &&
                   mig_cost.compareTo(new BigDecimal(this.migration_ub)) <= 0);
            BigDecimal mig_lb_diff = mig_cost.subtract(new BigDecimal(this.migration_lb));
            BigDecimal mig_range = new BigDecimal(this.migration_ub.subtract(this.migration_lb));
            return 1.0 - MathUtils.divideBigDecimals(mig_lb_diff,
                                                     mig_range,
                                                     RoundingMode.HALF_EVEN).doubleValue();
        }
        
        /**
         * Performs fuzzy evaluation of the quality of a solution (the larger the better).
         * It assumes that the solution has been evaluated.
         * @param sol The solution.
         * @return The fuzzy evaluation score for {@code sol}.
         * @see VMCwMProblem#evaluate(Solution)
         */
        private double fuzzyEvaluation(Solution sol) {
            double[] membership_values = (problem.getNumberOfObjectives() > 2) ? new double[3] :
                                                                                 new double[2];
            membership_values[0] = lowEnergyMembership(sol);
            membership_values[1] = lowWastageMembership(sol);
            if (problem.getNumberOfObjectives() > 2) {
                membership_values[2] = fewMigrationsMembership(sol);
            }
            return BETA * MathUtils.min(membership_values) + (1.0 - BETA) * MathUtils.avg(membership_values);
        }
        
        /**
         * Ensures that a solution has the fuzzy evaluation attribute set.
         * It assumes that the solution has been evaluated.
         * @param sol
         * @see VMCwMProblem#evaluate(Solution)
         */
        private void ensureFuzzyEvaluation(Solution sol) {
            if (!sol.hasAttribute(FUZZY_EVAL_ATR)) {
                sol.setAttribute(FUZZY_EVAL_ATR, new Double(fuzzyEvaluation(sol)));
            }
        }
        
        /**
         * Gets the fuzzy evaluation score of a solution.
         * It assumes that the solution has been evaluated.
         * @param sol The solution.
         * @return The fuzzy evaluation score of {@code sol}.
         * @see VMCwMProblem#evaluate(Solution)
         */
        private double getFuzzyEvaluation(Solution sol) {
            ensureFuzzyEvaluation(sol);
            return ((Double)sol.getAttribute(FUZZY_EVAL_ATR)).doubleValue();
        }
        
        /**
         * Updates the population for the next generation given a set of offspring solutions.
         * @param offspring The offspring solutions.
         */
        private void updatePopulation(Population offspring) {
            evaluateAll(offspring);
            Comparator<Solution> comp = new Comparator<Solution>() {
                public int compare(Solution sol1, Solution sol2) {
                    double violation1 = VMCwMProblem.getConstraintViolation(sol1);
                    double violation2 = VMCwMProblem.getConstraintViolation(sol2);
                    if (violation1 != violation2) {
                        return (violation1 > violation2) ? 1 : -1;
                    }
                    double eval1 = getFuzzyEvaluation(sol1), eval2 = getFuzzyEvaluation(sol2);
                    return Double.compare(eval2, eval1);
                }
            };
            offspring.sort(comp);
            Population next_gen = new Population();
            for (int i = 0; i < offspring.size(); ++i) {
                Solution offspring_sol = offspring.get(i);
                if (    next_gen.size() == 0 ||
                        getFuzzyEvaluation(next_gen.get(next_gen.size()-1)) !=
                                getFuzzyEvaluation(offspring_sol) ||
                        !solutionEquals(next_gen.get(next_gen.size()-1), offspring.get(i))) {
                    next_gen.add(offspring.get(i));
                }
            }
            assert(next_gen.size() >= this.population.size());
            next_gen.truncate(this.population.size(), comp);
            assert(next_gen.size() == this.population.size());
            this.population.clear();
            this.population.addAll(next_gen);
        }
        
        /**
         * Ensures that a set of solutions has the groups attribute set.
         * @param solutions The solutions.
         */
        private void ensureGroups(Solution... solutions) {
            for (int i = 0; i < solutions.length; ++i) {
                Solution sol = solutions[i];
                if (!sol.hasAttribute(GROUP_ATR)) {
                    sol.setAttribute(GROUP_ATR, computeGroups(sol));
                }
            }
        }
        
        /**
         * Gets the groups of a solution.
         * @param sol The solution.
         * @return The groups of {@code sol}.
         */
        private Group[] getGroups(Solution sol) {
            ensureGroups(sol);
            return (Group[])sol.getAttribute(GROUP_ATR);
        }
        
        /**
         * Initializes the population with the initialization operator provided in {@link #GGA}.
         */
        @Override
        protected void initialize() {
            initialized = true;
            Solution[] initial_solutions = initialization.initialize();
            evaluateAll(initial_solutions);
            population.addAll(initial_solutions);
        }

        /**
         * Performs an iteration of the grouping genetic algorithm. Replaces the current population with a
         * new population for the next generation.
         */
        @Override
        protected void iterate() {
            Population offspring = new Population();
            offspring.addAll(this.population);
            // Perform ranking crossover
            for (int i = 0; i < crossover_rate*this.population.size(); ++i) {
                int first_idx = PRNG.nextInt(this.population.size()), second_idx;
                for (second_idx = first_idx;
                     second_idx == first_idx;
                     second_idx = PRNG.nextInt(this.population.size()));
                ensureGroups(this.population.get(first_idx), this.population.get(second_idx));
                offspring.add(rankingCrossover(population.get(first_idx), population.get(second_idx)));
            }
            // Perform mutation
            for (int i = 0; i < mutation_rate*this.population.size(); ++i) {
                int idx = PRNG.nextInt(this.population.size());
                ensureGroups(this.population.get(idx));
                offspring.add(mutate(this.population.get(idx)));
            }
            updatePopulation(offspring);
        }
        
        /**
         * Retrieves the current population.
         * @return The current population.
         */
        @Override
        public Population getPopulation() {
            Population ret_pop = new Population();
            ret_pop.addAll(population);
            return ret_pop;
        }
        
        /**
         * Retrieves the non-dominated solutions in the current population.
         * @return The non-dominated population.
         */
        @Override
        public NondominatedPopulation getResult() {
            return new NondominatedPopulation(getPopulation());
        }
        
    }
    
    /**
     * Factory class for the Virtual Machine Consolidation algorithm that uses the grouping genetic algorithm.
     * Required in order to instantiate the algorithm.
     * @author Miguel Terra-Neves
     */
    private class GGAAllocAlgorithmProvider extends AlgorithmProvider {
        
        /**
         * If the name of the grouping genetic algorithm ("GGA") is given as input, produces an instance of
         * it for Virtual Machine Consolidation. Otherwise, returns null.
         * @param name The name of the algorithm.
         * @param properties A set of configuration properties, such as population size ("populationSize"),
         * crossover rate ("gga.crossoverRate") and mutation rate ("gga.mutationRate").
         * @param problem The problem instance.
         * @return An instance of the grouping genetic algorithm if provided with the name "GGA", null
         * otherwise.
         */
        @Override
        public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
            if (name.equals("GGA")) {
                TypedProperties typed_props = new TypedProperties(properties);
                int pop_size = typed_props.getInt("populationSize", 100);
                Population population = new Population();
                Initialization initialization = makeInitializer(problem, pop_size);
                double mutation_rate = typed_props.getDouble("gga.mutationRate", 0.0);
                double crossover_rate = typed_props.getDouble("gga.crossoverRate", 0.8);
                return new GGA(problem, population, mutation_rate, crossover_rate, initialization);
            }
            return null;
        }
        
    }

    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on the grouping
     * genetic algorithm.
     * @param instance The problem instance.
     */
    public GGAAlloc(VMCwMProblem instance) {
        super(instance, "GGA", VMCwMProblem.Encoding.INTEGER);
        AlgorithmFactory.getInstance().addProvider(new GGAAllocAlgorithmProvider());
    }

    /**
     * Sets the crossover rate to be used by the algorithm. In the grouping genetic algorithm, the crossover
     * rate is the number of crossover operations, applied at each generation, as a fraction of the population
     * size.
     * @param rate The crossover rate.
     */
    public void setCrossoverRate(double rate) { exec = exec.withProperty("gga.crossoverRate", rate); }
    
    /**
     * Sets the mutation rate to be used by the algorithm. In the grouping genetic algorithm, the mutation
     * rate is the fraction of the population that is randomly mutated at each generation.
     * @param rate The mutation rate.
     */
    public void setMutationRate(double rate) { exec = exec.withProperty("gga.mutationRate", rate); }

}
