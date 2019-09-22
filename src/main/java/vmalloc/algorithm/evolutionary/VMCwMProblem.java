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

package vmalloc.algorithm.evolutionary;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.moeaframework.Analyzer;
import org.moeaframework.analysis.sensitivity.ResultFileReader;
import org.moeaframework.core.FrameworkException;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.PopulationIO;
import org.moeaframework.core.Settings;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.variable.BinaryVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;
import org.moeaframework.util.ReferenceSetMerger;
import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;

import com.google.common.collect.Multimap;

import vmalloc.algorithm.AllocAlgorithm;
import vmalloc.domain.CPUSumAggregator;
import vmalloc.domain.Job;
import vmalloc.domain.JobVec;
import vmalloc.domain.Mapping;
import vmalloc.domain.MappingVec;
import vmalloc.domain.MemorySumAggregator;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.exception.InvalidEncodingException;
import vmalloc.utils.CollectionUtils;
import vmalloc.utils.IOUtils;

/**
 * The {@link VMCwMProblem} class extends the {@link AbstractProblem} class, which is mandatory in order to
 * make and evaluate possible solutions when using the MOEA Framework for multi-objective evolutionary
 * optimization. It also implements methods for setting the variables of {@link Solution} objects, and
 * retrieving their values and objective function costs. In order to avoid incorrect behavior, it is highly
 * advised that this class be used when performing operations on solutions instead of the methods provided by
 * the {@link Solution} class.
 * This class is further extended with conversion methods that allow for different representations of virtual
 * machine allocations to be used depending on the algorithm (evolutionary or not), and solution set dumping
 * and analysis.
 * It also functions as a container for an instance of the Virtual Machine Consolidation (VMC) problem.
 * @author Miguel Terra-Neves
 */
public class VMCwMProblem extends AbstractProblem {
    
    /**
     * Variable encoding abstract class used for representing values of variables in solutions.
     * @author Miguel Terra-Neves
     */
    private abstract class VariableEncoding {
        
        /**
         * Makes a fresh variable with the corresponding encoding.
         * @return The variable
         */
        abstract Variable makeVariable();
        
        /**
         * Encodes a given value, provided as an integer, and assigns it to a variable.
         * @param x The variable.
         * @param val The integer value.
         */
        abstract void setVariableValue(Variable x, int val);
        
        /**
         * Retrieves the variable assignments of a given solution as integers.
         * @param solution The solution.
         * @return An integer array with the values assigned to each variable.
         */
        abstract int[] getVariableAssignment(Solution solution);
        
    }
    
    /**
     * Variable encoding class that encodes values as plain integers.
     * @author Miguel Terra-Neves
     */
    private class IntegerEncoding extends VariableEncoding {
        
        /**
         * Makes a fresh integer variable, with domain ranging from 0 to the number of physical machines
         * (exclusive).
         * @return The variable.
         */
        @Override
        Variable makeVariable() { return EncodingUtils.newInt(0, pms.size()-1); }
        
        /**
         * Assigns a value to an integer variable.
         * @param x The integer variable.
         * @param val The value.
         */
        @Override
        void setVariableValue(Variable x, int val) { EncodingUtils.setInt(x, val); }
        
        /**
         * Retrieves the variable assignments of a given solution as integers.
         * @param solution A solution composed of integer variables.
         * @return An array with the values assigned to each variable.
         */
        @Override
        int[] getVariableAssignment(Solution solution) { return EncodingUtils.getInt(solution); }
        
    }
    
    /**
     * Variable encoding class that encodes integer values as bit-strings.
     * @author Miguel Terra-Neves
     */
    private class BinaryIntegerEncoding extends VariableEncoding {
        
        /**
         * Makes a fresh integer bit-string variable, with domain ranging from 0 to the number of physical
         * machines (exclusive).
         * @return The variable.
         */
        @Override
        Variable makeVariable() { return EncodingUtils.newBinaryInt(0, pms.size()-1); }
        
        /**
         * Assigns a value to an integer bit-string variable.
         * @param x The integer bit-string variable.
         * @param val The integer value.
         */
        @Override
        void setVariableValue(Variable x, int val) { EncodingUtils.encode(val, (BinaryVariable)x); }
        
        /**
         * Retrieves the variable assignments of a given solution and decodes them to integers.
         * @param solution A solution composed of integer bit-string variables.
         * @return An array with the values assigned to each variable, decoded to integers.
         */
        @Override
        int[] getVariableAssignment(Solution solution) {
            int[] x = new int[vms.size()];
            for (int i = 0; i < vms.size(); ++i) {
                x[i] = (int)EncodingUtils.decode((BinaryVariable)solution.getVariable(i));
            }
            return x;
        }
    }
    
    /**
     * Enumerator that represents variable encoding types.
     * @author Miguel Terra-Neves
     */
    public static enum Encoding {
        
        /**
         * The null encoding.
         */
        NONE,
        
        /**
         * Integer encoding.
         */
        INTEGER,
        
        /**
         * Integers encoded as bit-strings.
         */
        BINARY_INTEGER;
        
    }
    
    /**
     * Default encoding to use in case one is not specified.
     */
    private static final Encoding DEFAULT_ENCODING = Encoding.INTEGER;
    
    /**
     * Value of the epsilon constant used in the resource wastage objective function. Refer to:
     * Zheng, Qinghua, et al. "Virtual machine consolidated placement based on multi-objective
     * biogeography-based optimization." Future Generation Computer Systems 54 (2016): 95-122.
     */
    private static final double WASTAGE_EPSILON = 0.0001;
    
    /**
     * Index of the energy consumption objective cost of a solution.
     * @see {@link Solution#getObjective(int)}, {@link Solution#getObjectives()}
     */
    private static final int ENERGY_OBJ_INDEX = 0;
    
    /**
     * Index of the resource wastage objective cost of a solution.
     * @see {@link Solution#getObjective(int)}, {@link Solution#getObjectives()}
     */
    private static final int WASTAGE_OBJ_INDEX = 1;
    
    /**
     * Index of the migrations objective cost of a solution.
     * @see {@link Solution#getObjective(int)}, {@link Solution#getObjectives()}
     */
    private static final int MIGRATION_OBJ_INDEX = 2;
    
    /**
     * Name of the problem. Required by the MOEA Framework.
     * @see VMCwMProblemProvider
     */
    public static final String NAME = "VMCwMProblem";
    
    /**
     * Retrieves the energy consumption cost of a solution.
     * It assumes that the solution has been evaluated.
     * @param sol The solution.
     * @return The energy consumption cost.
     * @see {@link #evaluate(Solution)}
     */
    public static double getEnergyCost(Solution sol) {
        return sol.getObjective(ENERGY_OBJ_INDEX);
    }
    
    /**
     * Retrieves the resource wastage cost of a solution.
     * It assumes that the solution has been evaluated.
     * @param sol The solution.
     * @return The resource wastage cost.
     * @see {@link #evaluate(Solution)}
     */
    public static double getWastageCost(Solution sol) {
        return sol.getObjective(WASTAGE_OBJ_INDEX);
    }
    
    /**
     * Retrieves the migration cost of a solution.
     * It assumes that the solution has been evaluated.
     * @param sol The solution.
     * @return The solution cost.
     * @see {@link #evaluate(Solution)}
     */
    public static double getMigrationCost(Solution sol) {
        return sol.getObjective(MIGRATION_OBJ_INDEX);
    }
    
    /**
     * Retrieves the individual costs of a given objective function of a population of solutions.
     * It assumes that all solutions have been evaluated.
     * @param pop The population.
     * @param obj_idx The index of the objective function.
     * @return An array with the solutions' costs of the given objective in order.
     * @see {@link #evaluate(Solution)}
     */
    private static double[] getObjectiveCosts(NondominatedPopulation pop, int obj_idx) {
        double[] costs = new double[pop.size()];
        for (int i = 0; i < pop.size(); ++i) {
            costs[i] = pop.get(i).getObjective(obj_idx);
        }
        return costs;
    }
    
    /**
     * Retrieves the individual energy consumption costs of a population of solutions.
     * It assumes that all solutions have been evaluated.
     * @param pop The population.
     * @return An array with the solutions' energy consumption costs in order.
     * @see {@link #evaluate(Solution)}
     */
    public static double[] getEnergyCosts(NondominatedPopulation pop) {
        return getObjectiveCosts(pop, ENERGY_OBJ_INDEX);
    }
    
    /**
     * Retrieves the individual resource wastage costs of a population of solutions.
     * It assumes that all solutions have been evaluated.
     * @param pop The population.
     * @return An array with the solutions' resource wastage costs in order.
     * @see {@link #evaluate(Solution)}
     */
    public static double[] getWastageCosts(NondominatedPopulation pop) {
        return getObjectiveCosts(pop, WASTAGE_OBJ_INDEX);
    }
    
    /**
     * Retrieves the individual migration costs of a population of solutions.
     * It assumes that all solutions have been evaluated.
     * @param pop The population.
     * @return An array with the solutions' migration costs in order.
     * @see {@link #evaluate(Solution)}
     */
    public static double[] getMigrationCosts(NondominatedPopulation pop) {
        return getObjectiveCosts(pop, MIGRATION_OBJ_INDEX);
    }
    
    /**
     * Gets the constraint violation of a solution.
     * It assumes that the solution has been evaluated.
     * @param sol The solution.
     * @return The solution's constraint violation value.
     * @see {@link #evaluate(Solution)}
     */
    public static double getConstraintViolation(Solution sol) { return sol.getConstraint(0); }
    
    /**
     * Vector with the instance's physical machines.
     */
    private final PhysicalMachineVec pms;
    
    /**
     * Vector with the instance's jobs.
     */
    private final JobVec jobs;
    
    /**
     * Vector with the instance's virtual machines.
     */
    private final VirtualMachineVec vms;
    
    /**
     * Vector with the instance's pre-existing virtual machine to server mappings.
     */
    private final MappingVec mappings;
    
    /**
     * The budget percentile of the total memory capacity that can be used for migrations.
     */
    private double max_mig_percentile = 1.0;

    /**
     * The total CPU requirements of the virtual machines.
     */
    private final BigInteger total_cpu_req;
    
    /**
     * The total memory requirements of the virtual machines.
     */
    private final BigInteger total_mem_req;
    
    /**
     * The total CPU capacity of the physical machines.
     */
    private final BigInteger total_cpu_cap;
    
    /**
     * The total memory capacity of the physical machines.
     */
    private final BigInteger total_mem_cap;
    
    /**
     * The absolute memory budget for migrations.
     */
    private final BigInteger max_mig_mem;
    
    /**
     * A vector with the sets of virtual machines that cannot be colocated. Each vector position contains a
     * vector of virtual machines that must be mapped to distinct physical machines.
     */
    private final IVec<VirtualMachineVec> anti_coloc_vms;
    
    /**
     * Vector containing the virtual machines with platform constraints, i.e., virtual machines that can
     * only be mapped to a subset of the physical machines.
     */
    private final VirtualMachineVec plat_constrained_vms;
    
    /**
     * Mapping of virtual machine ids to the corresponding indexes in the {@link #vms} vector.
     */
    private final Map<String, Integer> vm_id_to_idx;
    
    /**
     * Mapping of physical machine ids to the corresponding indexes in the {@link #pms} vector.
     */
    private final Map<Integer, Integer> pm_id_to_idx;
    
    /**
     * Instance of the variable encoding to use.
     */
    private VariableEncoding encoding;
    
    /**
     * Boolean indicating if objective function computation should include the denominators in the expression.
     */
    private boolean include_denominators;
    
    /**
     * Given an encoding type, makes the corresponding instance of a {@link VariableEncoding} object.
     * @param encoding The encoding type.
     * @return A variable encoding object for the given type.
     */
    private VariableEncoding makeEncoding(Encoding encoding) {
        if (encoding == Encoding.INTEGER) {
            return new IntegerEncoding();
        }
        else if (encoding == Encoding.BINARY_INTEGER) {
            return new BinaryIntegerEncoding();
        }
        throw new InvalidEncodingException();
    }
    
    /**
     * Makes a fresh variable.
     * @return The variables.
     */
    private Variable makeVariable() { return encoding.makeVariable(); }
    
    /**
     * Computes the number of variables required in a solution of the VMC problem, which depends on the
     * virtual machines in the instance.
     * @param vms The virtual machines.
     * @return The number of variables required.
     */
    private static int calculateNVariables(VirtualMachineVec vms) { return vms.size(); }
    
    /**
     * Computes the number of objective functions in a VMC problem instance, which depends on whether there
     * exists a set of pre-existing virtual machine to server mappings.
     * @param mapping_exists True if a set of pre-existing mappings exists, false otherwise.
     * @return The number of objective functions.
     */
    private static int calculateNObjectives(boolean mapping_exists) {
        return mapping_exists ? 3 : 2;
    }

    /**
     * Retrieves the total CPU capacity of a set of physical machines.
     * @param pms The physical machines.
     * @return The total CPU capacity of the physical machines.
     */
    private static BigInteger cpuCapacitySum(PhysicalMachineVec pms) {
        CPUSumAggregator cpu_agr = new CPUSumAggregator();
        cpu_agr.processPhysicalMachines(pms);
        return cpu_agr.getCPUSum();
    }
    
    /**
     * Retrieves the total memory capacity of a set of physical machines.
     * @param pms The physical machines.
     * @return The total memory capacity of the physical machines.
     */
    private static BigInteger memoryCapacitySum(PhysicalMachineVec pms) {
        MemorySumAggregator mem_agr = new MemorySumAggregator();
        mem_agr.processPhysicalMachines(pms);
        return mem_agr.getMemorySum();
    }
    
    /**
     * Retrieves the total CPU requirements of a set of virtual machines.
     * @param vms The virtual machines.
     * @return The total CPU requirements of the virtual machines.
     */
    private static BigInteger cpuRequirementSum(VirtualMachineVec vms) {
        CPUSumAggregator cpu_agr = new CPUSumAggregator();
        cpu_agr.processVirtualMachines(vms);
        return cpu_agr.getCPUSum();
    }
    
    /**
     * Retrieves the total memory requirements of a set of virtual machines.
     * @param vms The virtual machines.
     * @return The total memory requirements of the virtual machines.
     */
    private static BigInteger memoryRequirementSum(VirtualMachineVec vms) {
        MemorySumAggregator mem_agr = new MemorySumAggregator();
        mem_agr.processVirtualMachines(vms);
        return mem_agr.getMemorySum();
    }
    
    /**
     * Computes the sets of virtual machines that cannot be colocated, from a given set of jobs.
     * @param jobs The jobs.
     * @return A vector containing, in each position, a vector with a set of virtual machines that must be
     * mapped to distinct physical machines.
     */
    private static IVec<VirtualMachineVec> retrieveAntiColocatableVirtualMachines(JobVec jobs) {
        IVec<VirtualMachineVec> anti_coloc_vms = new Vec<VirtualMachineVec>();
        for (int i = 0; i < jobs.size(); ++i) {
            Job job = jobs.get(i);
            VirtualMachineVec job_anti_coloc_vms = job.getAntiColocatableVirtualMachines();
            if (job_anti_coloc_vms.size() > 1) {
                anti_coloc_vms.push(job_anti_coloc_vms);
            }
        }
        return anti_coloc_vms;
    }
    
    /**
     * Computes, from a given set of virtual machines, the subset of virtual machines with platform
     * constraints, i.e., virtual machines that can only be mapped to a subset of the physical machine.
     * @param vms The virtual machines.
     * @return The subset of virtual machines that can only be mapped to a subset of physical machines.
     */
    private static VirtualMachineVec retrievePlatformConstrainedVirtualMachines(VirtualMachineVec vms) {
        VirtualMachineVec plat_constrained_vms = new VirtualMachineVec();
        for (int i = 0; i < vms.size(); ++i) {
            if (vms.get(i).getUnallowedPhysicalMachines().size() > 0) {
                plat_constrained_vms.push(vms.get(i));
            }
        }
        return plat_constrained_vms;
    }
    
    /**
     * Sets the value of a variable.
     * @param var The variable.
     * @param val The value as an integer.
     */
    private void setVariableValue(Variable var, int val) {
        this.encoding.setVariableValue(var, val);
    }
    
    /**
     * Creates an instance of the Virtual Machine Consolidation problem with the default variable encoding.
     * @param pms The set of physical machines.
     * @param jobs The set of jobs.
     * @param mappings The set of pre-existing virtual machine to physical machine mappings.
     */
    public VMCwMProblem(PhysicalMachineVec pms, JobVec jobs, MappingVec mappings) {
        this(pms, jobs, mappings, 1.0);
    }
    
    /**
     * Creates an instance of the Virtual Machine Consolidation problem with a given variable encoding type.
     * @param pms The set of physical machines.
     * @param jobs The set of jobs.
     * @param mappings The set of pre-existing virtual machine to physical machine mappings.
     * @param encoding The variable encoding type.
     */
    public VMCwMProblem(PhysicalMachineVec pms, JobVec jobs, MappingVec mappings, Encoding encoding) {
        this(pms, jobs, mappings, 1.0, encoding);
    }
    
    /**
     * Creates an instance of the Virtual Machine Consolidation problem with the default variable encoding.
     * @param pms The set of physical machines.
     * @param jobs The set of jobs.
     * @param mappings The set of pre-existing virtual machine to physical machine mappings.
     * @param max_mig_percentile The migration budget percentile, i.e., the percentile of the total memory
     * capacity that can be used for migrations.
     */
    public VMCwMProblem(PhysicalMachineVec pms,
                        JobVec jobs,
                        MappingVec mappings,
                        double max_mig_percentile) {
        this(pms, jobs, mappings, max_mig_percentile, DEFAULT_ENCODING);
    }
    
    /**
     * Creates an instance of the Virtual Machine Consolidation problem with a given variable encoding type.
     * @param pms The set of physical machines.
     * @param jobs The set of jobs.
     * @param mappings The set of pre-existing virtual machine to physical machine mappings.
     * @param max_mig_percentile The migration budget percentile, i.e., the percentile of the total memory
     * capacity that can be used for migrations.
     * @param encoding The variable encoding type.
     */
    public VMCwMProblem(PhysicalMachineVec pms,
                        JobVec jobs,
                        MappingVec mappings,
                        double max_mig_percentile,
                        Encoding encoding) {
        super(calculateNVariables(jobs.flattenJobs()), calculateNObjectives(mappings.size() > 0), 1);
        this.pms = pms;
        this.jobs = jobs;
        this.vms = this.jobs.flattenJobs();
        this.mappings = mappings;
        assert(max_mig_percentile >= 0.0 && max_mig_percentile <= 1.0);
        this.max_mig_percentile = max_mig_percentile;
        this.anti_coloc_vms = retrieveAntiColocatableVirtualMachines(this.jobs);
        this.plat_constrained_vms = retrievePlatformConstrainedVirtualMachines(this.vms);
        this.pm_id_to_idx = CollectionUtils.makePhysicalMachineIDtoIndexMap(this.pms);
        this.vm_id_to_idx = CollectionUtils.makeVirtualMachineIDtoIndexMap(this.vms);
        this.total_cpu_cap = cpuCapacitySum(this.pms);
        this.total_mem_cap = memoryCapacitySum(this.pms);
        this.total_cpu_req = cpuRequirementSum(this.vms);
        this.total_mem_req = memoryRequirementSum(this.vms);
        this.max_mig_mem =
                new BigDecimal(this.total_mem_cap).multiply(
                        new BigDecimal(this.max_mig_percentile)).toBigInteger();
        this.encoding = makeEncoding(encoding);
        this.include_denominators = true;
    }
    
    /**
     * Discards the denominators in the objective functions.
     */
    public void discardDenominators() { this.include_denominators = false; }
    
    /**
     * Retrieves the instance's set of physical machines. The actual inner vector is returned, modify at
     * your own risk.
     * @return The physical machines.
     */
    public PhysicalMachineVec getPhysicalMachines() { return this.pms; }
    
    /**
     * Retrieves the instance's set of jobs. The actual inner vector is returned, modify at your own risk.
     * @return The jobs.
     */
    public JobVec getJobs() { return this.jobs; }
    
    /**
     * Retrieves the instance's set of virtual machines. The actual inner vector is returned, modify at your
     * own risk.
     * @return The virtual machines.
     */
    public VirtualMachineVec getVirtualMachines() { return this.vms; }
    
    /**
     * Retrieves the instance's set of pre-existing virtual machine to physical machine mappings. The actual
     * inner vector is returned, modify at your own risk.
     * @return The virtual machine to physical machine mappings.
     */
    public MappingVec getMappings() { return this.mappings; }
    
    /**
     * Retrieves the migration budget percentile, i.e., the percentile of the total memory capacity that can
     * be used for migrations.
     * @return The migration budget percentile.
     */
    public double getMaxMigrationPercentile() { return this.max_mig_percentile; }
    
    /**
     * Retrieves the maximum total memory of migrated virtual machines.
     * @return The maximum total memory of migrated virtual machines.
     */
    public BigInteger getMaxMigrationMemory() { return this.max_mig_mem; }
    
    /**
     * Retrieves the total CPU capacity of the physical machines.
     * @return The total CPU capacity.
     */
    public BigInteger getTotalCPUCapacity() { return this.total_cpu_cap; }
    
    /**
     * Retrieves the total memory capacity of the physical machines.
     * @return The total memory capacity.
     */
    public BigInteger getTotalMemoryCapacity() { return this.total_mem_cap; }
    
    /**
     * Retrieves the total CPU requirements of the virtual machines.
     * @return The total CPU requirements.
     */
    public BigInteger getTotalCPURequirements() { return this.total_cpu_req; }
    
    /**
     * Retrieves the total memory requirements of the virtual machines.
     * @return The total memory requirements.
     */
    public BigInteger getTotalMemoryRequirements() { return this.total_mem_req; }
    
    /**
     * Retrieves a vector with the sets of virtual machines that cannot be colocated. Each vector position
     * contains a vector of virtual machines that must be mapped to distinct physical machines. An inner
     * vector is returned, modify at your own risk.
     * @return A vector with the sets of anti-colocatable virtual machines.
     */
    public IVec<VirtualMachineVec> getAntiColocatableVirtualMachines() { return this.anti_coloc_vms; }
    
    /**
     * Retrieves a vector containing the virtual machines with platform constraints, i.e., virtual machines
     * that can only be mapped to a subset of the physical machines. An inner vector is returned, modify at
     * your own risk.
     * @return The set of virtual machines with platform constraints.
     */
    public VirtualMachineVec getPlatformConstrainedVirtualMachines() {
        return this.plat_constrained_vms;
    }
    
    /**
     * Computes an upper bound of the energy consumption objective.
     * @return An energy consumption upper bound.
     */
    public double getMaxEnergyConsumption() {
        double val = 0.0;
        for (int i = 0; i < pms.size(); ++i) {
            val += (double)pms.get(i).getMaxConsumption();
        }
        return val;
    }
    
    /**
     * Computes an upper bound of the resource wastage objective.
     * @return A resource wastage upper bound.
     */
    public double getMaxResourceWastage() {
        return (this.include_denominators) ? (double)pms.size()*(1.0+WASTAGE_EPSILON) / 2.0
                                           : (double)pms.size();
    }
    
    /**
     * Computes an upper bound of the migration cost objective.
     * @return A migration cost upper bound.
     */
    public double getMaxMigrationCost() {
        double val = 0.0;
        for (int i = 0; i < mappings.size(); ++i) {
            val += mappings.get(i).getVirtualMachine().getMemory().doubleValue();
        }
        return val;
    }

    /**
     * Retrieves the virtual machine to physical machine mapping encoded in a solution as an array of
     * integers. Each position in the array corresponds to the virtual machine in the same position in the
     * vector returned by {@link #getVirtualMachines()}, and the corresponding value stored in that position
     * is an index of the physical machine in the vector returned by {@link #getPhysicalMachines()}.
     * @param solution The solution.
     * @return The virtual machine to physical machine mapping as an array of integers.
     */
    public int[] getVirtualMachineAssignment(Solution solution) {
        return this.encoding.getVariableAssignment(solution);
    }
    
    /**
     * Sets the mapping of a virtual machine to a physical machine in a given solution.
     * @param solution The solution.
     * @param vm_idx The index of the virtual machine in the vector returned by
     * {@link #getVirtualMachines()}.
     * @param pm_idx The index of the physical machine, in the vector returned by
     * {@link #getPhysicalMachines()}, to which to map the virtual machine.
     */
    public void setVirtualMachineMapping(Solution solution, int vm_idx, int pm_idx) {
        setVariableValue(solution.getVariable(vm_idx), pm_idx);
    }
    
    /**
     * Sets the mapping of a virtual machine to a physical machine in a given solution.
     * @param solution The solution.
     * @param vm The virtual machine.
     * @param pm The physical machine.
     */
    public void setVirtualMachineMapping(Solution solution, VirtualMachine vm, PhysicalMachine pm) {
        setVirtualMachineMapping(solution, getVirtualMachineIndex(vm), getPhysicalMachineIndex(pm));
    }
    
    /**
     * Retrieves the index of a virtual machine in the vector returned by {@link #getVirtualMachines()}.
     * @param vm The virtual machine.
     * @return The virtual machine's index.
     */
    public int getVirtualMachineIndex(VirtualMachine vm) {
        return this.vm_id_to_idx.get(vm.getID()).intValue();
    }
    
    /**
     * Retrieves the index of a physical machine in the vector returned by {@link #getPhysicalMachines()}.
     * @param pm The physical machine.
     * @return The physical machine's index.
     */
    public int getPhysicalMachineIndex(PhysicalMachine pm) {
        return this.pm_id_to_idx.get(new Integer(pm.getID())).intValue();
    }
    
    /**
     * Sets the variable encoding.
     * @param enc The variable encoding type.
     */
    public void setEncoding(VMCwMProblem.Encoding enc) {
        this.encoding = makeEncoding(enc);
    }
    
    /**
     * Converts a solution object to an equivalent allocation, i.e., a vector of mappings of virtual
     * machines to physical machines.
     * @param solution The solution.
     * @return An equivalent allocation.
     */
    public MappingVec solutionToAllocation(Solution solution) {
        MappingVec allocation = new MappingVec();
        int[] x = getVirtualMachineAssignment(solution);
        for (int i = 0; i < x.length; ++i) {
            allocation.push(new Mapping(getVirtualMachines().get(i),
                                        getPhysicalMachines().get(x[i])));
        }
        return allocation;
    }
    
    /**
     * Converts an allocation, i.e., a vector of mappings of virtual machines to physical machines, to an
     * equivalent solution object.
     * @param allocation The allocation.
     * @return An equivalent solution.
     */
    public Solution allocationToSolution(MappingVec allocation) {
        Solution solution = newSolution();
        for (int i = 0; i < allocation.size(); ++i) {
            PhysicalMachine pm = allocation.get(i).getPhysicalMachine();
            VirtualMachine vm = allocation.get(i).getVirtualMachine();
            setVirtualMachineMapping(solution, vm, pm);
        }
        return solution;
    }
    
    /**
     * Converts a population of solutions to a vector of equivalent allocations, i.e., vectors of mappings
     * of virtual machines to physical machines.
     * @param pop The population of solutions.
     * @return A vector of equivalent allocations.
     */
    public IVec<MappingVec> populationToAllocations(NondominatedPopulation pop) {
        IVec<MappingVec> allocations = new Vec<MappingVec>();
        for (int i = 0; i < pop.size(); ++i) {
            allocations.push(solutionToAllocation(pop.get(i)));
        }
        return allocations;
    }
    
    /**
     * Converts a vector of allocations, i.e., vectors of mappings of virtual machines to physical machines,
     * to an equivalent population of solutions.
     * @param allocations The allocations.
     * @return A population of equivalent solutions.
     */
    public NondominatedPopulation allocationsToPopulation(IVec<MappingVec> allocations) {
        NondominatedPopulation pop = new NondominatedPopulation();
        for (int i = 0; i < allocations.size(); ++i) {
            pop.add(allocationToSolution(allocations.get(i)));
        }
        return pop;
    }
    
    /**
     * Converts a solution to the default variable encoding.
     * @param sol The solution.
     * @return An equivalent solution in the default variable encoding.
     */
    private Solution convertToDefaultEncoding(Solution sol) {
        int[] assign = getVirtualMachineAssignment(sol); // must do this first before changing encoding
        VariableEncoding enc_tmp = this.encoding;
        setEncoding(DEFAULT_ENCODING);
        Solution converted_sol = newSolution();
        for (int i = 0; i < getNumberOfVariables(); ++i) {
            setVirtualMachineMapping(converted_sol, i, assign[i]);
        }
        evaluate(converted_sol);
        // TODO: saving IDs instead because heuristic reduction messes up indexes; storing the ID works
        // because, in the test instances, PM with ID 'i' is at position 'i'; ideally, an index converter
        // should be passed from the heuristic reduction class to this class, or parsing of the population
        // file should convert from IDs to indexes
        // moreover, a dirty HACK is necessary (new int variable) in order to ensure that the variable's
        // range can accommodate the server's id; this must be fixed as well
        for (int i = 0; i < getNumberOfVariables(); ++i) {
            int id = this.pms.get(assign[i]).getID();
            converted_sol.setVariable(i, EncodingUtils.newInt(0, Math.max(this.pms.size()-1, id)));
            setVirtualMachineMapping(converted_sol, i, id);
        }
        this.encoding = enc_tmp;
        return converted_sol;
    }
    
    /**
     * Converts a population of solutions to the default variable encoding.
     * @param pop The population of solutions.
     * @return An equivalent population of solutions in the default variable encoding.
     */
    private NondominatedPopulation convertToDefaultEncoding(NondominatedPopulation pop) {
        NondominatedPopulation converted_pop = new NondominatedPopulation();
        for (int i = 0; i < pop.size(); ++i) {
            converted_pop.add(convertToDefaultEncoding(pop.get(i)));
        }
        return converted_pop;
    }
    
    /**
     * Converts a set of populations of solutions to the default variable encoding.
     * @param pops The list of populations.
     * @return A list of equivalent populations of solutions in the default variable encoding.
     */
    private List<NondominatedPopulation> convertToDefaultEncoding(List<NondominatedPopulation> pops) {
        List<NondominatedPopulation> converted_pops = new LinkedList<NondominatedPopulation>();
        for (int i = 0; i < pops.size(); ++i) {
            converted_pops.add(convertToDefaultEncoding(pops.get(i)));
        }
        return converted_pops;
    }
    
    /**
     * Dumps a population of solutions to a file in MOEA Framework's format.
     * It assumes that all solutions have been evaluated.
     * @param path The file path.
     * @param pop The population.
     * @throws IOException If an error occurs writing to the file.
     * @see #evaluate(Solution)
     */
    public void dumpPopulation(String path, NondominatedPopulation pop) throws IOException {
        new Analyzer().withProblem(NAME).add(NAME, convertToDefaultEncoding(pop)).saveAs(NAME, new File(path));
    }
    
    /**
     * Dumps a set of populations of solutions to a file in MOEA Framework's format. The method expects the
     * set of populations to represent the solution sets computed using different seeds in a randomized
     * algorithm. Each population is the full solution set computed using a single seed.
     * It assumes that all solutions have been evaluated.
     * @param path The file path.
     * @param pops The set of populations.
     * @throws IOException If an error occurs writing to the file.
     * @see {@link #evaluate(Solution)} {@link AllocAlgorithm#allocateMultipleSeeds(int)}
     */
    public void dumpPopulations(String path, List<NondominatedPopulation> pops) throws IOException {
        new Analyzer().withProblem(NAME).addAll(NAME, convertToDefaultEncoding(pops)).saveAs(NAME, new File(path));
    }
    
    /**
     * Dumps a population to a reference set file that can be used in population analysis for computing
     * statistics that demand a reference set (e.g. inverted generational distance).
     * It assumes that all solutions in {@code ref_set} have been evaluated.
     * @param path The path to the reference set file.
     * @param ref_set The reference set.
     * @throws IOException If an error occurs writing to the file.
     * @see {@link #evaluate(Solution)} {@link #analyzePopulations(Map, String)}
     */
    public void dumpReferenceSet(String path, NondominatedPopulation ref_set) throws IOException {
        PopulationIO.writeObjectives(new File(path), ref_set);
    }
    
    /**
     * Retrieves the default reference solution set to use when performing analysis of populations produced
     * by algorithms. This is required for computing hypervolumes and to avoid certain corner case error
     * conditions in the computation of inverted generational distance.
     * @return The default reference set.
     */
    public NondominatedPopulation getDefaultReferenceSet() {
        NondominatedPopulation ref = new NondominatedPopulation();
        Solution ref_sol = makeOrigin(getNumberOfObjectives());
        ref_sol.setObjective(0, getMaxEnergyConsumption());
        ref.add(ref_sol);
        ref_sol = makeOrigin(getNumberOfObjectives());
        ref_sol.setObjective(1, getMaxResourceWastage());
        ref.add(ref_sol);
        if (getNumberOfObjectives() == 3) {
            ref_sol = makeOrigin(getNumberOfObjectives());
            ref_sol.setObjective(2, getMaxMigrationCost());
            ref.add(ref_sol);
        }
        return ref;
    }
    
    /**
     * Retrieves the normalized default reference solution set to use when performing analysis of populations
     * produced by algorithms. This is required for computing hypervolumes and to avoid certain corner
     * case error conditions in the computation of inverted generational distance.
     * @return The normalized default reference set.
     */
    public NondominatedPopulation getNormalizedDefaultReferenceSet() {
        NondominatedPopulation ref = getDefaultReferenceSet();
        NondominatedPopulation norm_ref = new NondominatedPopulation();
        for (int i = 0; i < ref.size(); ++i) {
            Solution sol = ref.get(i);
            normalize(sol);
            norm_ref.add(sol);
        }
        return norm_ref;
    }
    
    /**
     * Creates a solution object that represents the origin of the objective space.
     * @param nobj The number of objective functions in the instance.
     * @return The origin of the objective space.
     */
    private Solution makeOrigin(int nobj) {
        Solution solution = null;
        if (nobj == 3) {
            solution = new Solution(new double[] {0.0, 0.0, 0.0});
        }
        else {
            solution = new Solution(new double[] {0.0, 0.0});
        }
        return solution;
    }
    
    /**
     * Performs statistical analysis on the solution sets stored in a set of files. The files may be
     * solution sets produced by different algorithms. The result of the analysis is printed to the console,
     * which includes inverted generational distance, hypervolume, epsilon-indicator, spacing and statistical
     * significance.
     * @param dataset A mapping of labels (usually algorithm names) to file paths. These files are expected
     * to be in the MOEA Framework format, as produced by
     * {@link #dumpPopulation(String, NondominatedPopulation)} and {@link #dumpPopulations(String, List)}.
     * @throws IOException If an error occurs reading from the files.
     */
    public void analyzePopulations(Multimap<String, String> dataset) throws IOException {
        VariableEncoding enc_tmp = this.encoding;
        setEncoding(DEFAULT_ENCODING);
        NondominatedPopulation ref_set = buildReferenceSet(dataset);
        Solution nadir = getNadirPoint(ref_set), ideal = getIdealPoint(ref_set);
        Solution ref_point = computeRefPoint(ref_set, nadir);
        sanitizeRefSet(ref_set, nadir, ideal, ref_point);
        Analyzer analyzer = buildAnalyzer(dataset, ref_set, ideal, ref_point);
        analyzer.printAnalysis();
        this.encoding = enc_tmp;
    }
    
    /**
     * Builds the reference set to be used in inverted generational distance and epsilon-indicator
     * computation for a given dataset.
     * @param dataset A mapping of labels (usually algorithm names) to file paths. These files are expected
     * to be in the MOEA Framework format, as produced by
     * {@link #dumpPopulation(String, NondominatedPopulation)} and {@link #dumpPopulations(String, List)}.
     * @return The reference set.
     * @throws IOException If an error occurs reading from the files.
     */
    private NondominatedPopulation buildReferenceSet(Multimap<String, String> dataset) throws IOException {
        ReferenceSetMerger merger = new ReferenceSetMerger();
        for (Iterator<String> it = dataset.keySet().iterator(); it.hasNext();) {
            String label = it.next();
            Iterator<String> path_it = dataset.get(label).iterator();
            for (int i = 0; path_it.hasNext();) {
                String path = path_it.next();
                ResultFileReader reader = new ResultFileReader(this, new File(path));
                try {
                    for (; reader.hasNext(); ++i) {
                        NondominatedPopulation pop = reader.next().getPopulation();
                        Population ref_pop = new Population();
                        for (int j = 0; j < pop.size(); ++j) {
                            Solution sol = pop.get(j);
                            this.evaluate(sol);
                            if (!sol.violatesConstraints()) {
                                normalize(sol);
                                ref_pop.add(sol);
                            }
                        }
                        merger.add(label + "_seed" + i, ref_pop);
                    }
                }
                catch (FrameworkException fe) {     // FIXME: replace with more useful exception class
                    throw new RuntimeException("error reading population from " + path, fe);
                }
                finally {
                    reader.close();
                }
            }
        }
        NondominatedPopulation ref_set = merger.getCombinedPopulation();
        if (ref_set.isEmpty()) {                                    // necessary to prevent exception in
            ref_set.addAll(getNormalizedDefaultReferenceSet());     // MOEA framework
        }
        return ref_set;
    }
    
    /**
     * Computes a reference point for hypervolume computation based on some reference set and the respective
     * nadir point.
     * @param ref_set The reference set.
     * @param nadir The nadir point.
     * @return A solution representing the reference point.
     */
    private Solution computeRefPoint(NondominatedPopulation ref_set, Solution nadir) {
        assert(!ref_set.isEmpty());
        Solution ref_point = newSolution();
        double r = 1.0 + 1.0 / Math.max(1.0,  ref_set.size()-1);
        for (int i = 0; i < getNumberOfObjectives(); ++i) {
            ref_point.setObjective(i, nadir.getObjective(i) * r);
        }
        return ref_point;
    }
    
    /**
     * Computes the nadir point for a given reference set. The nadir point contains the worst possible values
     * in the reference set for each cost function.
     * @param ref_set The reference set.
     * @return The nadir point.
     */
    private Solution getNadirPoint(NondominatedPopulation ref_set) { return getPoint(ref_set, true); }
    
    /**
     * Computes the ideal point for a given reference set. The ideal point constains the best possible values
     * in the reference set for each cost function.
     * @param ref_set The reference set.
     * @return The ideal point.
     */
    private Solution getIdealPoint(NondominatedPopulation ref_set) { return getPoint(ref_set, false); }
    
    /**
     * Computes either the nadir or the ideal point for a given reference set.
     * @param ref_set The reference set.
     * @param nadir True if the nadir point is to be computed, false if the ideal point is to be computed
     * instead.
     * @return The point.
     */
    private Solution getPoint(NondominatedPopulation ref_set, boolean nadir) {
        assert(!ref_set.isEmpty());
        Solution point = newSolution();
        for (int i = 0; i < getNumberOfObjectives(); ++i) {
            double val = ref_set.get(0).getObjective(i);
            for (int j = 1; j < ref_set.size(); ++j) {
                double j_val = ref_set.get(j).getObjective(i);
                val = nadir ? Math.max(val, j_val) : Math.min(val, j_val);
            }
            point.setObjective(i, val);
        }
        return point;
    }
    
    /**
     * Cleans up the reference set in order to avoid possible error scenarios in the MOEA framework.
     * @param ref_set The reference set.
     * @param nadir The nadir point.
     * @param ideal The ideal point.
     * @param ref The clean reference set.
     */
    private void sanitizeRefSet(NondominatedPopulation ref_set, Solution nadir, Solution ideal, Solution ref) {
        assert(!ref_set.isEmpty());
        boolean single = ref_set.size() == 1;
        for (int i = 0; i < getNumberOfObjectives(); ++i) {
            if (single || nadir.getObjective(i) - ideal.getObjective(i) < Settings.EPS) {
                Solution sol = ideal.copy();
                sol.setObjective(i, ref.getObjective(i));
                int j = (i +1) % getNumberOfObjectives();                   // this is required when the reference
                sol.setObjective(j, sol.getObjective(j) - Settings.EPS);    // set has a single solution, or the
                ref_set.add(sol);                                           // new point would be dominated by it
            }
        }
    }
    
    /**
     * Builds the MOEA framework analyzer for performing statistical analysis on the solution sets stored
     * in a set of files.
     * @param dataset A mapping of labels (usually algorithm names) to file paths. These files are expected
     * to be in the MOEA Framework format, as produced by
     * {@link #dumpPopulation(String, NondominatedPopulation)} and {@link #dumpPopulations(String, List)}.
     * @param ref_set The reference set to use for inverted generational distance and epsilon-indicator
     * computation.
     * @param ideal The ideal point to use for normalization.
     * @param ref The reference point to use for hypervolume computation.
     * @return The analyzer object.
     * @throws IOException If an error occurs reading from the files.
     */
    private Analyzer buildAnalyzer(Multimap<String, String> dataset,
                                   NondominatedPopulation ref_set,
                                   Solution ideal,
                                   Solution ref)
            throws IOException {
        File ref_set_file = IOUtils.makeTemporaryFile("ref_set", ".pop", true);
        dumpReferenceSet(ref_set_file.getAbsolutePath(), ref_set);
        Analyzer analyzer = new Analyzer();
        analyzer = analyzer.withProblem(NAME)
                           .includeInvertedGenerationalDistance()
                           .includeHypervolume()
                           .includeSpacing()
                           .includeAdditiveEpsilonIndicator()
                           .withReferencePoint(ref.getObjectives())
                           .withIdealPoint(ideal.getObjectives())
                           .showIndividualValues()
                           .showStatisticalSignificance()
                           .withReferenceSet(ref_set_file);
        for (Iterator<String> it = dataset.keySet().iterator(); it.hasNext();) {
            String label = it.next();
            for (Iterator<String> path_it = dataset.get(label).iterator(); path_it.hasNext();) {
                String path = path_it.next();
                ResultFileReader reader = new ResultFileReader(this, new File(path));
                try {
                    while (reader.hasNext()) {
                        NondominatedPopulation pop = reader.next().getPopulation();
                        NondominatedPopulation filtered_pop = new NondominatedPopulation();
                        for (int i = 0; i < pop.size(); ++i) {
                            Solution sol = pop.get(i);
                            evaluate(sol);
                            if (!sol.violatesConstraints()) {
                                normalize(sol);
                                filtered_pop.add(sol);
                            }
                        }
                        analyzer.add(label, filtered_pop);
                    }
                }
                catch (FrameworkException fe) {     // FIXME: replace with more useful exception class
                    throw new RuntimeException("error reading population from " + path, fe);
                }
                finally {
                    reader.close();
                }
            }
        }
        return analyzer;
    }
    
    /**
     * Computes the objective function costs and constraint violations of a solution and stores them.
     * @param solution The solution.
     */
    public void evaluate(Solution solution) {
        // Initialize
        int[] x = getVirtualMachineAssignment(solution);
        BigInteger[] used_cpu_caps = new BigInteger[this.pms.size()];
        BigInteger[] used_mem_caps = new BigInteger[this.pms.size()];
        for (int i = 0; i < this.pms.size(); ++i) {
            used_cpu_caps[i] = BigInteger.ZERO;
            used_mem_caps[i] = BigInteger.ZERO;
        }
        for (int i = 0; i < this.vms.size(); ++i) {
            int pm_idx = x[i];
            used_cpu_caps[pm_idx] = used_cpu_caps[pm_idx].add(this.vms.get(i).getCPU());
            used_mem_caps[pm_idx] = used_mem_caps[pm_idx].add(this.vms.get(i).getMemory());
        }
        // Set energy consumption objective function value
        double total_energy = 0.0;
        for (int i = 0; i < this.pms.size(); ++i) {
            if (!used_cpu_caps[i].equals(BigInteger.ZERO)) {
                double idle_energy = (double)this.pms.get(i).getIdleConsumption();
                double max_energy = (double)this.pms.get(i).getMaxConsumption();
                double used_cpu = used_cpu_caps[i].doubleValue()/
                                  this.pms.get(i).getCPU().doubleValue();
                total_energy += idle_energy;
                total_energy += (max_energy-idle_energy) * used_cpu;
            }
        }
        solution.setObjective(ENERGY_OBJ_INDEX, total_energy);
        // Set resource wastage objective function value
        double total_wastage = 0.0;
        for (int i = 0; i < this.pms.size(); ++i) {
            if (!used_cpu_caps[i].equals(BigInteger.ZERO) || !used_mem_caps[i].equals(BigInteger.ZERO)) {
                double used_cpu = used_cpu_caps[i].doubleValue() / this.pms.get(i).getCPU().doubleValue();
                double used_mem = used_mem_caps[i].doubleValue() / this.pms.get(i).getMemory().doubleValue();
                double cpu_left = 1.0 - used_cpu;
                double mem_left = 1.0 - used_mem;
                if (this.include_denominators) {
                    total_wastage += (Math.abs(cpu_left-mem_left) + WASTAGE_EPSILON) /
                                     (2.0 * (used_cpu+used_mem));
                }
                else {
                    total_wastage += Math.abs(cpu_left-mem_left);
                }
            }
        }
        solution.setObjective(WASTAGE_OBJ_INDEX, total_wastage);
        // Set migration objective function value
        BigInteger migged_mem = BigInteger.ZERO;
        if (solution.getNumberOfObjectives() == 3) {
            for (int i = 0; i < this.mappings.size(); ++i) {
                VirtualMachine vm = this.mappings.get(i).getVirtualMachine();
                PhysicalMachine pm = this.mappings.get(i).getPhysicalMachine();
                if (x[getVirtualMachineIndex(vm)] != getPhysicalMachineIndex(pm)) {
                    migged_mem = migged_mem.add(vm.getMemory());
                }
            }
            solution.setObjective(MIGRATION_OBJ_INDEX, migged_mem.doubleValue());
        }
        // Set capacity constraint violations
        double viol = 0.0;
        for (int i = 0; i < this.pms.size(); ++i) {
            double diff = used_cpu_caps[i].subtract(this.pms.get(i).getCPU()).doubleValue();
            viol += diff > 0.0 ? diff : 0.0;
            diff = used_mem_caps[i].subtract(this.pms.get(i).getMemory()).doubleValue();
            viol += diff > 0.0 ? diff : 0.0;
        }
        // Set anti-colocation constraint violations
        Set<PhysicalMachine> used_pms = new HashSet<PhysicalMachine>();
        for (int i = 0; i < this.anti_coloc_vms.size(); ++i) {
            for (int j = 0; j < this.anti_coloc_vms.get(i).size(); ++j) {
                VirtualMachine vm = this.anti_coloc_vms.get(i).get(j);
                PhysicalMachine pm = this.pms.get(x[getVirtualMachineIndex(vm)]);
                if (used_pms.contains(pm)) {
                    viol += 1.0;
                }
                used_pms.add(pm);
            }
            used_pms.clear();
        }
        // Set platform constraint violations
        for (int i = 0; i < this.plat_constrained_vms.size(); ++i) {
            VirtualMachine vm = this.plat_constrained_vms.get(i);
            if (!vm.canRunInPhysicalMachine(pms.get(x[getVirtualMachineIndex(vm)]))) {
                viol += 1.0;
            }
        }
        // Set migration constraint violation
        if (solution.getNumberOfObjectives() == 3) {
            double diff = migged_mem.subtract(this.max_mig_mem).doubleValue();
            viol += diff > 0.0 ? diff : 0.0;
        }
        solution.setConstraint(0, viol);
    }
    
    /**
     * Normalized the objective values of a given solution into the [0, 1] range.
     * @param sol The solution to be normalized.
     */
    public void normalize(Solution sol) {
        sol.setObjective(0, sol.getObjective(0) / getMaxEnergyConsumption());
        sol.setObjective(1, sol.getObjective(1) / getMaxResourceWastage());
        if (getNumberOfObjectives() > 2) {
            sol.setObjective(2, sol.getObjective(2) / getMaxMigrationCost());
        }
    }

    /**
     * Creates a fresh solution with all variables unassigned.
     * @return The solution.
     */
    public Solution newSolution() {
        Solution solution = new Solution(getNumberOfVariables(),
                                         getNumberOfObjectives(),
                                         1);
        for (int i = 0; i < getNumberOfVariables(); ++i) {
            solution.setVariable(i, makeVariable());
        }
        return solution;
    }

}
