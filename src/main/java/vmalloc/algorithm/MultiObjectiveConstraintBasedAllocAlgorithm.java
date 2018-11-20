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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.moeaframework.core.PRNG;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.constraints.ConstraintAggregator;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.constraints.OPBPrinter;
import vmalloc.algorithm.constraints.PseudoBooleanSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.JobVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.utils.CollectionUtils;
import vmalloc.utils.MathUtils;

/**
 * Abstract superclass for Virtual Machine Consolidation problem solvers that rely on multi-objective
 * constraint-based optimization.
 * @author Miguel Terra-Neves
 */
public abstract class MultiObjectiveConstraintBasedAllocAlgorithm extends ConstraintBasedAllocAlgorithm {
    
    /**
     * Number of variables to include, per objective, in a diversification path for path diversification
     * based algorithms.
     */
    // TODO: turn configurable or adaptive
    private static final int VARS_PER_OBJECTIVE = 4;
    
    /**
     * Class for storing a weight-literal pair.
     * @author Miguel Terra-Neves
     */
    protected class WeightedLit implements Comparable<WeightedLit> {
        
        /**
         * The literal.
         */
        private int lit = 0;
        
        /**
         * The weight.
         */
        private BigDecimal weight = BigDecimal.ZERO;
        
        /**
         * Creates an instance of a weighted literal.
         * @param lit The literal.
         * @param weight The weight.
         */
        public WeightedLit(int lit, BigDecimal weight) {
            this.lit = lit;
            this.weight = weight;
        }
        
        /**
         * Retrieves the literal.
         * @return The literal.
         */
        public int getLit() { return this.lit; }
        
        /**
         * Retrieves the weight.
         * @return The weight.
         */
        public BigDecimal getWeight() { return this.weight; }
        
        public int compareTo(WeightedLit other) {
            return this.weight.compareTo(other.getWeight());
        }
    }
    
    /**
     * Class for objective function objects.
     * @author Miguel Terra-Neves
     */
    protected class ObjectiveFunction {
        
        /**
         * The literals in the objective function.
         */
        private IVecInt lits = null;
        
        /**
         * The coefficients of the literals in the objective function.
         */
        private IVec<BigDecimal> coeffs = null;
        
        /**
         * Stores the minimum value possible for the objective function.
         */
        private BigDecimal min_val = null;
        
        /**
         * Stores the maximum value possible for the objective function.
         */
        private BigDecimal max_val = null;
        
        /**
         * Creates an instance of an objective function object.
         * @param lits The objective function's literals.
         * @param coeffs The objective function's coefficients.
         */
        // TODO: filter out zero coefficients
        public ObjectiveFunction(IVecInt lits, IVec<BigDecimal> coeffs) {
            assert(lits.size() == coeffs.size());
            this.lits = lits;
            this.coeffs = coeffs;
            this.min_val = MathUtils.bigDecimalVecSum(filterPositive(coeffs));
            this.max_val = MathUtils.bigDecimalVecSum(filterNegative(coeffs));
        }
        
        /**
         * Core method for filtering positive or negative big decimals from a given vector.
         * @param vec The vector of big decimals.
         * @param filter_positive Boolean indicating which big decimals should be filtered. If true, the
         * positive ones are filtered. If false, the negative are.
         * @return A vector with the big decimals of {@code vec} that were not filtered.
         */
        private IVec<BigDecimal> filter_core(IVec<BigDecimal> vec, boolean filter_positive) {
            IVec<BigDecimal> pos_vec = new Vec<BigDecimal>();
            for (int i = 0; i < vec.size(); ++i) {
                if (    (filter_positive && vec.get(i).compareTo(BigDecimal.ZERO) < 0) ||
                        (!filter_positive && vec.get(i).compareTo(BigDecimal.ZERO) > 0)) {
                    pos_vec.push(vec.get(i));
                }
            }
            return pos_vec;
        }
        
        /**
         * Filters the negative big decimals of a vector.
         * @param vec The vector of big decimals.
         * @return A vector with all positive big decimals in {@code vec}.
         */
        private IVec<BigDecimal> filterNegative(IVec<BigDecimal> vec) { return filter_core(vec, false); }
        
        /**
         * Filters the positive big decimals of a vector.
         * @param vec The vector of big decimals.
         * @return A vector with all negative big decimals in {@code vec}.
         */
        private IVec<BigDecimal> filterPositive(IVec<BigDecimal> vec) { return filter_core(vec, true); }
        
        /**
         * Retrieves the literals in the objective function.
         * @return The objective function's literals.
         */
        public IVecInt getLits() { return this.lits; }
        
        /**
         * Retrieves the coefficients in the objective function.
         * @return The objective function's coefficients.
         */
        public IVec<BigDecimal> getCoeffs() { return this.coeffs; }
        
        /**
         * Retrieves the minimum value possible for the objective function.
         * @return The objective function's minimum value.
         */
        public BigDecimal getMinValue() { return this.min_val; }
        
        /**
         * Retrieves the maximum value possible for the objective function.
         * @return The objective function's maximum value.
         */
        public BigDecimal getMaxValue() { return this.max_val; }
        
        /**
         * Computes the size of the objective function's co-domain.
         * @return The objective function's co-domain size.
         */
        public BigDecimal getCoDomainSize() { return getMaxValue().subtract(getMinValue()); }
        
        /**
         * Retrieves the objective function as a vector of weighted literals.
         * @return The objective function as weighted literals.
         */
        public IVec<WeightedLit> asWeightedLits() {
            IVec<WeightedLit> weighted_lits = new Vec<WeightedLit>();
            for (int i = 0; i < this.lits.size(); ++i) {
                if (this.coeffs.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    weighted_lits.push(new WeightedLit(this.lits.get(i), this.coeffs.get(i)));
                }
                else if (this.coeffs.get(i).compareTo(BigDecimal.ZERO) < 0) {
                    weighted_lits.push(new WeightedLit(-this.lits.get(i), this.coeffs.get(i).negate()));
                }
            }
            return weighted_lits;
        }
    }
    
    /**
     * Stores the objective functions for the Virtual Machine Consolidation instance. Objective functions
     * are grouped based on the original objectives in case of division reduction.
     */
    protected IVec<IVec<ObjectiveFunction>> obj_functions = null;
    
    /**
     * Vector of Boolean variables, one for each physical machine in the problem index. These are auxiliary
     * variables used in the linearization of the absolute value operator in the resource wastage function.
     */
    private IVecInt aux_pm_pvars = null;
    
    /**
     * Vector of vectors of vectors of Boolean variables. There is one vector of vectors for each job in the
     * problem instance. The {@code i}-th vector contains one vector of Boolean variables per virtual machine
     * in the {@code i}-th job. These are auxiliary variables used in the linearization of the absolute value
     * operator in the resource wastage function.
     */
    private IVec<IVec<IVecInt>> aux_job_plus_vars = null;
    
    /**
     * Vector of vectors of vectors of Boolean variables. There is one vector of vectors for each job in the
     * problem instance. The {@code i}-th vector contains one vector of Boolean variables per virtual machine
     * in the {@code i}-th job. These are auxiliary variables used in the linearization of the absolute value
     * operator in the resource wastage function.
     */
    private IVec<IVec<IVecInt>> aux_job_minus_vars = null;
    
    /**
     * Boolean indicating if objective function denominators are to be ignored or not.
     */
    private boolean ignore_denominators;
    
    /**
     * Boolean indicating if path diversification is enabled or not.
     */
    private boolean path_diverse_enabled;
    
    /**
     * Boolean indicating if stratification is enabled or not.
     */
    private boolean stratify = false;
    
    /**
     * Boolean indicating if merged stratification is to be used. If not, split stratification is used
     * instead.
     */
    private boolean merged_stratify = false;
    
    /**
     * The literal to weight ratio for stratification's partitioning heuristic.
     */
    private double lit_weight_ratio = 2.0;
    
    /**
     * The number of partitions for stratification's partitioning heuristic. Override literal to weight ratio.
     */
    private int npartitions = 0;
    
    /**
     * Vector of vectors of literals. After stratification is initialized, stores the partitions for each
     * objective function.
     */
    private IVec<IVec<IVecInt>> undef_partitions = null;
    
    /**
     * Vector of doubles. After stratification is initialized, stores the selection probability for each
     * objective function.
     */
    private IVec<Double> selection_probs = null;
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on multi-objective
     * constraint-based optimization.
     * @param instance The problem instance.
     */
    public MultiObjectiveConstraintBasedAllocAlgorithm(VMCwMProblem instance) {
        super(instance);
        this.ignore_denominators = false;
        this.path_diverse_enabled = false;
    }
    
    /**
     * Instructs the allocation algorithm to ignore denominators in the objective functions.
     */
    public void ignoreDenominators() { this.ignore_denominators = true; }
    
    /**
     * Checks if path diversification is enabled.
     * @return True if path diversification is enabled, false otherwise.
     */
    protected boolean pathDiversificationEnabled() { return this.path_diverse_enabled; }
    
    /**
     * Enables path diversification.
     */
    public void enablePathDiversification() { this.path_diverse_enabled = true; }
    
    /**
     * Enables stratification. {@code merged} indicates if division reduction should be handled by merging
     * reduced functions or by splitting the selection probability.
     * @param merged True if merged stratification is to be used, false if split.
     */
    public void enableStratification(boolean merged) {
        this.stratify = true;
        this.merged_stratify = merged;
    }
    
    /**
     * Enables stratification, handling division reduction by splitting the selection probability.
     */
    public void enableStratification() { enableStratification(false); }
    
    /**
     * Sets the literal to weight ratio for stratification's partitioning heuristic.
     * @param ratio The literal to weight ratio.
     */
    public void setLitWeightRatio(double ratio) { this.lit_weight_ratio = ratio; }
    
    /**
     * Sets a fixed number of partitions for stratification's partitioning heuristic. Overrides literal to
     * weight ratio.
     * @param val The number of partitions.
     * @see #setLitWeightRatio(double)
     */
    public void setFixedPartitionNumber(int val) { this.npartitions = val; }
    
    /**
     * Cache used in the {@link #getWastageCoefficients(PhysicalMachine, VirtualMachineVec)} to prevent
     * recomputing normalized resource requirements that have been computed already for a given pair of
     * physical machine resource capacities.
     */
    private Map<Pair<BigInteger,BigInteger>, Pair<IVec<BigDecimal>,IVec<BigDecimal> > > wastage_coeffs_cache =
            new HashMap<Pair<BigInteger,BigInteger>, Pair<IVec<BigDecimal>,IVec<BigDecimal> > >();
    
    /**
     * Retrieves the resource wastage coefficients for a given set of virtual machines normalized in respect
     * to the capacities of a given physical machine.
     * @param pm The physical machine.
     * @param vms The virtual machines.
     * @return A pair of vectors of normalized coefficients. {@link Pair#getLeft()} returns the normalized
     * CPU requirements and {@link Pair#getRight()} the memory requirements.
     */
    private Pair<IVec<BigDecimal>,IVec<BigDecimal> > getWastageCoefficients(PhysicalMachine pm,
                                                                            VirtualMachineVec vms) {
        Pair<BigInteger,BigInteger> resource_pair = Pair.of(pm.getCPU(), pm.getMemory());
        if (!this.wastage_coeffs_cache.containsKey(resource_pair)) {
            double[] norm_cpus = getNormalizedCPURequirements(vms, pm);
            double[] norm_mems = getNormalizedMemoryRequirements(vms, pm);
            IVec<BigDecimal> norm_mem_minus_cpu = indexWiseSubtraction(norm_mems, norm_cpus);
            IVec<BigDecimal> norm_cpu_minus_mem = indexWiseSubtraction(norm_cpus, norm_mems);
            this.wastage_coeffs_cache.put(resource_pair,
                                          Pair.of(norm_mem_minus_cpu, norm_cpu_minus_mem));
            assert(norm_mem_minus_cpu.size() == vms.size());
            assert(norm_cpu_minus_mem.size() == vms.size());
        }
        assert(this.wastage_coeffs_cache.containsKey(resource_pair));
        assert(this.wastage_coeffs_cache.get(resource_pair).getLeft().size() == vms.size());
        return this.wastage_coeffs_cache.get(resource_pair);
    }
    
    /**
     * Produces a vector with the results of element-wise subtraction of two arrays.
     * @param a1 An array.
     * @param a2 Another array.
     * @return A vector with the results of element-wise subtraction. The {@code i}-th position of the vector
     * contains the result of {@code a1[i] - a2[i]}.
     */
    private IVec<BigDecimal> indexWiseSubtraction(double[] a1, double[] a2) {
        assert(a1.length == a2.length);
        IVec<BigDecimal> sub_array = new Vec<BigDecimal>(a1.length);
        for (int i = 0; i < a1.length; ++i) {
            sub_array.push(new BigDecimal(a1[i] - a2[i]));
        }
        return sub_array;
    }
    
    /**
     * Adds auxiliary constraints, to a constraint aggregator, necessary for linearization of the absolute value
     * operator in the resource wastage objective function.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param aux_pm_vars The auxiliary Boolean variables added per physical machine.
     * @param jobs The jobs.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of each variable
     * indicates if the corresponding virtual machine is mapped to a specific physical machine.
     * @param aux_job_plus_vars One set of auxiliary Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}.
     * @param aux_job_minus_vars Another set of auxiliary Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addWastageAuxiliaryConstraints(ConstraintAggregator aggr,
                                                  PhysicalMachineVec pms,
                                                  IVecInt aux_pm_vars,
                                                  JobVec jobs,
                                                  IVec<IVec<IVecInt>> job_vars,
                                                  IVec<IVec<IVecInt>> aux_job_plus_vars,
                                                  IVec<IVec<IVecInt>> aux_job_minus_vars)
                                                          throws ContradictionException {
        assert(pms.size() == aux_pm_vars.size());
        assert(jobs.size() == job_vars.size());
        assert(jobs.size() == aux_job_plus_vars.size());
        assert(jobs.size() == aux_job_minus_vars.size());
        VirtualMachineVec vms = jobs.flattenJobs();
        IVec<IVecInt> vm_vars = flattenJobVars(job_vars);
        assert(vms.size() == vm_vars.size());
        for (int i = 0; i < pms.size(); ++i) {
            IVecInt lits = new VecInt();
            for (int j = 0; j < vms.size(); ++j) {
                lits.push(vm_vars.get(j).get(i));
            }
            PhysicalMachine pm = pms.get(i);
            Pair<IVec<BigDecimal>,IVec<BigDecimal> > coeff_vec_pair = getWastageCoefficients(pm, vms);
            IVec<BigDecimal> norm_mem_minus_cpu = coeff_vec_pair.getLeft();
            IVec<BigDecimal> norm_cpu_minus_mem = coeff_vec_pair.getRight();
            assert(norm_mem_minus_cpu.size() == lits.size());
            assert(norm_cpu_minus_mem.size() == lits.size());
            lits.push(aux_pm_vars.get(i));
            norm_mem_minus_cpu.push(BigDecimal.ONE);
            aggr.addGreaterOrEqual(lits, norm_mem_minus_cpu, BigDecimal.ZERO);
            norm_mem_minus_cpu.pop();
            lits.pop();
            lits.push(-aux_pm_vars.get(i));
            norm_cpu_minus_mem.push(BigDecimal.ONE);
            aggr.addGreaterOrEqual(lits, norm_cpu_minus_mem, BigDecimal.ZERO);
            norm_cpu_minus_mem.pop();
        }
        IVec<IVecInt> aux_vm_plus_vars = flattenJobVars(aux_job_plus_vars);
        IVec<IVecInt> aux_vm_minus_vars = flattenJobVars(aux_job_minus_vars);
        assert(vms.size() == aux_vm_plus_vars.size());
        assert(vms.size() == aux_vm_minus_vars.size());
        for (int i = 0; i < pms.size(); ++i) {
            IVecInt lits = new VecInt();
            lits.push(-aux_pm_vars.get(i));
            for (int j = 0; j < vms.size(); ++j) {
                lits.push(-aux_vm_plus_vars.get(j).get(i));
                aggr.addClause(lits);
                lits.pop();
            }
            lits.pop();
            lits.push(aux_pm_vars.get(i));
            for (int j = 0; j < vms.size(); ++j) {
                lits.push(-aux_vm_minus_vars.get(j).get(i));
                aggr.addClause(lits);
                lits.pop();
            }
        }
        for (int i = 0; i < vms.size(); ++i) {
            for (int j = 0; j < pms.size(); ++j) {
                // Encode 'vm_vars - plus_var - minus_var = 0', explicit equality is extremely inefficient (in SAT4J at least)
                int vm_var = vm_vars.get(i).get(j);
                int plus_var = aux_vm_plus_vars.get(i).get(j);
                int minus_var = aux_vm_minus_vars.get(i).get(j);
                aggr.addClause(new VecInt(new int[] { -vm_var, -plus_var, -minus_var }));
                aggr.addClause(new VecInt(new int[] { -vm_var, plus_var, minus_var }));
                aggr.addClause(new VecInt(new int[] { vm_var, -plus_var }));
                aggr.addClause(new VecInt(new int[] { vm_var, -minus_var }));
                aggr.addClause(new VecInt(new int[] { -plus_var, -minus_var }));
            }
        }
    }

    /**
     * Initializes a constraint aggregator with the base variables and constraints common to all (or most)
     * multi-objective constraint-based algorithms. This method creates and stores the Boolean variables in
     * the {@link ConstraintBasedAllocAlgorithm#pm_vars}, {@link ConstraintBasedAllocAlgorithm#job_vars},
     * {@link #aux_pm_pvars}, {@link #aux_job_plus_vars} and {@link #aux_job_minus_vars} vectors.
     * @param aggr The constraint aggregator.
     * @throws ContradictionException If the constraint aggregator detects that the base formula is
     * unsatisfiable.
     */
    @Override
    protected void addVMCConstraints(ConstraintAggregator aggr) throws ContradictionException {
        super.addVMCConstraints(aggr);
        System.out.println("c Adding algorithm's base auxiliary variables and constraints");
        this.aux_pm_pvars = newVarsForPMs(aggr, this.instance.getPhysicalMachines());
        this.aux_job_plus_vars = newVarsForJobs(aggr,
                                                this.instance.getPhysicalMachines(),
                                                this.instance.getJobs());
        this.aux_job_minus_vars = newVarsForJobs(aggr,
                                                 this.instance.getPhysicalMachines(),
                                                 this.instance.getJobs());
        addWastageAuxiliaryConstraints(aggr,
                                       this.instance.getPhysicalMachines(),
                                       this.aux_pm_pvars,
                                       this.instance.getJobs(),
                                       this.job_vars,
                                       this.aux_job_plus_vars,
                                       this.aux_job_minus_vars);
        System.out.println("c Done");
        printElapsedTime();
    }
    
    /**
     * Makes the constraint solver and initializes it with the base formula for multi-objective optimization.
     * @return The constraint solver.
     * @throws ContradictionException If the constraint solver detects that the base formula is
     * unsatisfiable.
     */
    protected ConstraintSolver buildSolver() throws ContradictionException {
        ConstraintSolver solver = new PseudoBooleanSolver();
        addVMCConstraints(solver);
        return solver;
    }
    
    /**
     * Initializes the energy consumption objective function and adds it to {@link #obj_functions}.
     */
    private void initializeEnergyObjectiveFunction() {
        VirtualMachineVec vms = this.instance.getJobs().flattenJobs();
        IVec<IVecInt> vm_vars = flattenJobVars(this.job_vars);
        IVecInt lits = new VecInt();
        IVec<BigDecimal> coeffs = new Vec<BigDecimal>();
        for (int i = 0; i < this.instance.getPhysicalMachines().size(); ++i) {
            PhysicalMachine pm = this.instance.getPhysicalMachines().get(i);
            coeffs.push(new BigDecimal(pm.getIdleConsumption()));
            lits.push(this.pm_vars.get(i));
            int energy_range = pm.getMaxConsumption() - pm.getIdleConsumption();
            double[] norm_cpus = getNormalizedCPURequirements(vms, pm);
            for (int j = 0; j < vms.size(); ++j) {
                lits.push(vm_vars.get(j).get(i));
                coeffs.push(new BigDecimal(energy_range * norm_cpus[j]));
            }
        }
        this.obj_functions.push(
                new Vec<ObjectiveFunction>(new ObjectiveFunction[] { new ObjectiveFunction(lits, coeffs) }));
    }
    
    /**
     * Initializes the resource wastage objective function and adds it to {@link #obj_functions}. If not
     * ignoring denominators, multiple objective functions are added.
     */
    private void initializeWastageObjectiveFunction() {
        VirtualMachineVec vms = this.instance.getJobs().flattenJobs();
        IVec<IVecInt> aux_vm_plus_vars = flattenJobVars(this.aux_job_plus_vars);
        IVec<IVecInt> aux_vm_minus_vars = flattenJobVars(this.aux_job_minus_vars);
        IVec<IVecInt> vm_vars = flattenJobVars(this.job_vars);
        IVecInt ign_lits = new VecInt(); // literal vector to use if ignoring denominators
        IVec<BigDecimal> ign_coeffs = new Vec<BigDecimal>(); // coeff vector to use if ignoring denominators
        this.obj_functions.push(new Vec<ObjectiveFunction>());
        for (int i = 0; i < this.instance.getPhysicalMachines().size(); ++i) {
            PhysicalMachine pm = this.instance.getPhysicalMachines().get(i);
            IVecInt num_lits = new VecInt();
            IVec<BigDecimal> num_coeffs = new Vec<BigDecimal>();
            Pair<IVec<BigDecimal>,IVec<BigDecimal>> coeff_vec_pair = getWastageCoefficients(pm, vms);
            IVec<BigDecimal> norm_mem_minus_cpu = coeff_vec_pair.getLeft();
            IVec<BigDecimal> norm_cpu_minus_mem = coeff_vec_pair.getRight();
            assert(norm_mem_minus_cpu.size() == vms.size());
            assert(norm_cpu_minus_mem.size() == vms.size());
            for (int j = 0; j < vms.size(); ++j) {
                if (this.ignore_denominators) {
                    ign_lits.push(aux_vm_plus_vars.get(j).get(i));
                    ign_coeffs.push(norm_mem_minus_cpu.get(j));
                    ign_lits.push(aux_vm_minus_vars.get(j).get(i));
                    ign_coeffs.push(norm_cpu_minus_mem.get(j));
                }
                else {
                    num_lits.push(aux_vm_plus_vars.get(j).get(i));
                    num_coeffs.push(norm_mem_minus_cpu.get(j));
                    num_lits.push(aux_vm_minus_vars.get(j).get(i));
                    num_coeffs.push(norm_cpu_minus_mem.get(j));
                }
            }
            if (!this.ignore_denominators) {
                IVecInt den_lits = new VecInt();
                IVec<BigDecimal> den_coeffs = new Vec<BigDecimal>();
                double[] norm_cpu = getNormalizedCPURequirements(vms, pm);
                double[] norm_mem = getNormalizedMemoryRequirements(vms, pm);
                assert(norm_cpu.length == vms.size());
                assert(norm_mem.length == vms.size());
                for (int j = 0; j < vms.size(); ++j) {
                    den_lits.push(vm_vars.get(j).get(i));
                    den_coeffs.push(new BigDecimal(2 * (norm_cpu[j] + norm_mem[j])).negate());
                }
                this.obj_functions.last().push(new ObjectiveFunction(num_lits, num_coeffs));
                this.obj_functions.last().push(new ObjectiveFunction(den_lits, den_coeffs));
            }
        }
        if (this.ignore_denominators) {
            this.obj_functions.last().push(new ObjectiveFunction(ign_lits, ign_coeffs));
        }
    }
    
    /**
     * Initializes the migration cost objective function and adds it to {@link #obj_functions}.
     */
    private void initializeMigrationObjectiveFunction() {
        VirtualMachineVec vms = this.instance.getJobs().flattenJobs();
        IVec<IVecInt> vm_vars = flattenJobVars(this.job_vars);
        Map<Integer, Integer> pm_id_to_idx =
                CollectionUtils.makePhysicalMachineIDtoIndexMap(this.instance.getPhysicalMachines());
        Map<String, Integer> vm_id_to_idx = CollectionUtils.makeVirtualMachineIDtoIndexMap(vms);
        IVecInt lits = new VecInt();
        IVec<BigDecimal> coeffs = new Vec<BigDecimal>();
        for (int i = 0; i < this.instance.getMappings().size(); ++i) {
            VirtualMachine vm = this.instance.getMappings().get(i).getVirtualMachine();
            PhysicalMachine pm = this.instance.getMappings().get(i).getPhysicalMachine();
            int vm_idx = vm_id_to_idx.get(vm.getID()).intValue();
            int pm_idx = pm_id_to_idx.get(new Integer(pm.getID())).intValue();
            lits.push(-vm_vars.get(vm_idx).get(pm_idx));
            coeffs.push(new BigDecimal(vm.getMemory()));
        }
        this.obj_functions.push(
                new Vec<ObjectiveFunction>(new ObjectiveFunction[] { new ObjectiveFunction(lits, coeffs) }));
    }
    
    /**
     * Initializes the objective functions and adds them to {@link #obj_functions}.
     */
    protected void initializeObjectiveFunctions() {
        this.obj_functions = new Vec<IVec<ObjectiveFunction>>();
        initializeEnergyObjectiveFunction();
        initializeWastageObjectiveFunction();
        if (this.instance.getMappings().size() > 0) {
            initializeMigrationObjectiveFunction();
        }
    }
    
    /**
     * Computes an objective function cost of the constraint solver's current model. It is assumed that the
     * solver's {@link ConstraintSolver#solve()} or {@link ConstraintSolver#solve(IVecInt)} was called before
     * calling this function, and that {@link ConstraintSolver#isSatisfiable()} returned true.
     * @param solver The constraint solver.
     * @param function The objective function.
     * @return The objective function's cost.
     */
    protected BigDecimal computeObjectiveValue(ConstraintSolver solver, ObjectiveFunction function) {
        assert(solver.isSolved() && solver.isSatisfiable());
        BigDecimal obj_val = BigDecimal.ZERO;
        for (int i = 0; i < function.getLits().size(); ++i) {
            if (solver.modelValue(function.getLits().get(i))) {
                obj_val = obj_val.add(function.getCoeffs().get(i));
            }
        }
        return obj_val;
    }
    
    /**
     * Builds the vector of partitions of undefined (not known if satisfied or unsatisfied) soft literals for
     * the objective functions, used by minimal correction subset based algorithms. If stratification is
     * disabled, a single partition is returned.
     * @return The vector of partitions of undefined soft literals.
     */
    protected IVec<IVecInt> buildUndefFormulas() {
        return stratify ? buildUndefFormulas_stratified() : buildUndefFormulas_unstratified();
    }

    /**
     * Builds the unstratified vector of partitions of undefined (not known if satisfied or unsatisfied)
     * soft literals for the objective functions, used by minimal correction subset based algorithms.
     * A single partition is returned.
     * @return A single partition of undefined soft literals.
     */
    private IVec<IVecInt> buildUndefFormulas_unstratified() {
        IVecInt undef_fmls = new VecInt();
        for (int i = 0; i < this.obj_functions.size(); ++i) {
            for (int k = 0; k < this.obj_functions.get(i).size(); ++k) {
                ObjectiveFunction function = this.obj_functions.get(i).get(k);
                IVecInt lits = function.getLits();
                IVec<BigDecimal> coeffs = function.getCoeffs();
                for (int j = 0; j < lits.size(); ++j) {
                    if (coeffs.get(j).compareTo(BigDecimal.ZERO) > 0) {
                        undef_fmls.push(-lits.get(j));
                    }
                    else if (coeffs.get(j).compareTo(BigDecimal.ZERO) < 0) {
                        undef_fmls.push(lits.get(j));
                    }
                }
            }
        }
        return new Vec<IVecInt>(new IVecInt[] { undef_fmls });
    }

    /**
     * Builds the stratified vector of partitions of undefined (not known if satisfied or unsatisfied)
     * soft literals for the objective functions, used by minimal correction subset based algorithms.
     * @return The vector of partitions of undefined soft literals.
     */
    private IVec<IVecInt> buildUndefFormulas_stratified() {
        if (this.undef_partitions == null) {
            initStratified();
            assert(MathUtils.sum(this.selection_probs) == 1.0);
        }
        IVec<IVecInt> undef_fmls = new Vec<IVecInt>();
        IVec<IVec<IVecInt>> part_stacks = new Vec<IVec<IVecInt>>();
        double[] part_probs = CollectionUtils.unwrapDoubleVec(this.selection_probs);
        for (int i = 0; i < this.undef_partitions.size(); ++i) {
            IVec<IVecInt> partition = this.undef_partitions.get(i);
            IVec<IVecInt> part_stack = new Vec<IVecInt>();
            for (int j = partition.size()-1; j >= 0; --j) {
                part_stack.push(partition.get(j));
            }
            part_stacks.push(part_stack);
        }
        while (part_stacks.size() > 0) {
            int idx = MathUtils.rouletteWheelSelection(part_probs);
            IVec<IVecInt> part_stack = part_stacks.get(idx);
            IVecInt part = new VecInt();
            part_stack.last().copyTo(part);
            undef_fmls.push(part);
            part_stack.pop();
            if (part_stack.size() == 0) {
                part_stacks.set(idx, part_stacks.last());
                part_probs[idx] = part_probs[part_stacks.size()-1];     // FIXME: probability array is larger
                part_probs[part_stacks.size()-1] = 0;                   // than necessary due to trailing 0s;
                part_probs = MathUtils.normalize(part_probs);           // may become inefficient with a large
                part_stacks.pop();                                      // number of objectives
            }
        }
        System.out.println("c Stratification produced " + undef_fmls.size() + " partitions");
        return undef_fmls;
    }
    
    /**
     * Initializes stratification by partitioning the objective functions and assigning selection
     * probabilities.
     */
    private void initStratified() {
        this.undef_partitions = new Vec<IVec<IVecInt>>();
        this.selection_probs = new Vec<Double>();
        for (int i = 0; i < this.obj_functions.size(); ++i) {
            if (this.merged_stratify) {
                mergedStratify(this.obj_functions, i);
            }
            else {
                splitStratify(this.obj_functions, i);
            }
        }
        logPartitions();
    }
    
    /**
     * Logs the partition sizes for each objective function.
     */
    private void logPartitions() {
        for (int i = 0; i < this.undef_partitions.size(); ++i) {
            IVec<IVecInt> obj_parts = this.undef_partitions.get(i);
            for (int j = 0; j < obj_parts.size(); ++j) {
                IVecInt sub_obj_part = obj_parts.get(j);
                System.out.println("c :obj-idx " + i + " :part-idx " + j +
                                   " :part-size " + sub_obj_part.size());
            }
        }
    }

    /**
     * Generates a single sequence of partitions for a given set of objectives, considering them as a single
     * one.
     * @param objs The objective functions.
     * @param idx The index to the set of objectives to be stratified.
     */
    private void mergedStratify(IVec<IVec<ObjectiveFunction>> objs, int idx) {
        IVec<WeightedLit> weighted_lits = new Vec<WeightedLit>();
        for (int i = 0; i < objs.get(idx).size(); ++i) {
            objs.get(idx).get(i).asWeightedLits().copyTo(weighted_lits);
        }
        this.undef_partitions.push(partition(weighted_lits));
        this.selection_probs.push(new Double((double)1/this.obj_functions.size()));
    }
    
    /**
     * Generates sequences of partitions for a given set of objectives, splitting the selection probability
     * of the original objective among them.
     * @param objs The objective functions.
     * @param idx The index to the set of objectives to be stratified.
     */
    private void splitStratify(IVec<IVec<ObjectiveFunction>> objs, int idx) {
        for (int i = 0; i < objs.get(idx).size(); ++i) {
            this.undef_partitions.push(partition(objs.get(idx).get(i).asWeightedLits()));
            this.selection_probs.push(new Double((double)1/(objs.size()*objs.get(idx).size())));
        }
    }
    
    /**
     * Generates a partition sequence for the given set of weighted literals.
     * @param weighted_lits The weighted literals.
     * @return The partitioning of {@code weighted_lits}.
     */
    // TODO: adaptive probabilities
    private IVec<IVecInt> partition(IVec<WeightedLit> weighted_lits) {
        WeightedLit[] weighted_lits_array = new WeightedLit[weighted_lits.size()];
        weighted_lits.copyTo(weighted_lits_array);
        Arrays.sort(weighted_lits_array);
        IVec<IVecInt> obj_partitions = new Vec<IVecInt>();
        IVecInt part = new VecInt();
        int nweights = 0;
        for (int i = weighted_lits_array.length-1; i >= 0; --i) {
            if (    ((this.npartitions > 0 && this.npartitions * part.size() >= weighted_lits_array.length) ||
                     (this.npartitions == 0 && (double)part.size() / nweights > this.lit_weight_ratio)) &&
                    (i < weighted_lits_array.length-1 &&
                     !weighted_lits_array[i].getWeight().equals(weighted_lits_array[i+1].getWeight()))) {
                obj_partitions.push(part);
                part = new VecInt();
                nweights = 0;
            }
            part.push(-weighted_lits_array[i].getLit());
            if (nweights == 0 || !weighted_lits_array[i].getWeight().equals(weighted_lits_array[i+1].getWeight())) {
                nweights++;
            }
        }
        if (part.size() > 0) obj_partitions.push(part);
        return obj_partitions;
    }
    
    /**
     * Diversification paths are represented as a binary tree. The {@code PathNode} class represents a node
     * in that binary tree. Each node corresponds to a literal that must be satisfied for that path. Each
     * node also has a parent node. If the literal is 0 and the parent is {@code null}, then that node is the
     * root of the diversification tree. A diversification path is the set of literals in a path from a leaf
     * to the root.
     * @author Admin
     */
    protected class PathNode {
        
        /**
         * A literal that must be satisfied for the diversification paths that contain the node, or 0 if
         * the node is the root.
         */
        private int lit = 0;
        
        /**
         * The parent node of the node. If {@code parent} is null, then the node is the root.
         */
        private PathNode parent = null;
        
        /**
         * Creates the root node in the tree of diversification paths. The root node works as a sentinel, and
         * does not contain a valid literal.
         */
        public PathNode() {}
        
        /**
         * Creates a node in the tree of diversification paths.
         * @param lit A literal that must be satisfied by the diversification paths containing this node.
         * @param parent The parent node of the new node.
         */
        public PathNode(int lit, PathNode parent) {
            assert(parent != null);
            this.lit = lit;
            this.parent = parent;
        }
        
        /**
         * Retrieves the literal that must be satisfied by all paths containing the node, or 0 if the node
         * is the root.
         * @return The literal.
         */
        public int getLit() { return this.lit; }
        
        /**
         * Retrieves the node's parent node.
         * @return The node's parent node, or {@code null} if the node is the root.
         */
        public PathNode getParent() { return this.parent; }
        
        /**
         * Checks if the node is the root.
         * @return True if the node is the root, false otherwise.
         */
        public boolean isRoot() { return this.lit == 0 && this.parent == null; }
        
    }
    
    /**
     * Selects the literals of an objective function with the largest coefficients, in decreasing order.
     * Literals with variables in a set of forbidden variables are skipped. The variables in the selected
     * literals are added to the set of forbidden variables.
     * @param lits The objective function's literals.
     * @param coeffs The objective function's coefficients.
     * @param nvars The number of variables to select.
     * @param forbidden_vars The set of forbidden variables.
     * @return The literals that correspond to the largest coefficients, in decreasing order of those
     * coefficients.
     */
    private IVecInt selectLitsWithLargestCoeffs(IVecInt lits,
                                                IVec<BigDecimal> coeffs,
                                                int nvars,
                                                Set<Integer> forbidden_vars) {
        int[] sorted_idxs = CollectionUtils.getSortedDecreasingBigDecimalVecIndexes(coeffs);
        IVecInt chosen_lits = new VecInt();
        for (int i = 0; chosen_lits.size() < nvars && i < lits.size(); ++i) {
            int var = Math.abs(lits.get(sorted_idxs[i]));
            if (!forbidden_vars.contains(var)) {
                forbidden_vars.add(var);
                chosen_lits.push(lits.get(sorted_idxs[i]));
            }
        }
        return chosen_lits;
    }
    
    /**
     * Selects the literals to be included in the diversification paths according to the objective guided
     * heuristic. For better performance, the first diversification path should be all literals negated, and
     * then the literals should be switched from last to first.
     * @return A vector with the literals selected by the objective guided heuristic.
     */
    private IVecInt selectDiversificationLits_objectiveGuided() {
        Set<Integer> chosen_vars = new HashSet<Integer>();
        IVec<IVecInt> chosen_lit_vecs = new Vec<IVecInt>();
        for (int i = 0; i < this.obj_functions.size(); ++i) {
            IVecInt lits = new VecInt();
            IVec<BigDecimal> coeffs = new Vec<BigDecimal>();
            for (int j = 0; j < this.obj_functions.get(i).size(); ++j) {
                ObjectiveFunction function = this.obj_functions.get(i).get(j);
                function.getLits().copyTo(lits);
                function.getCoeffs().copyTo(coeffs);
            }
            chosen_lit_vecs.push(selectLitsWithLargestCoeffs(lits, coeffs, VARS_PER_OBJECTIVE, chosen_vars));
        }
        IVecInt div_lits = new VecInt();
        for (int i = 0; i < VARS_PER_OBJECTIVE; ++i) {
            for (int j = 0; j < chosen_lit_vecs.size(); ++j) {
                IVecInt lits = chosen_lit_vecs.get(j);
                div_lits.push(lits.last());
                lits.pop();
            }
        }
        return div_lits;
    }
    
    /**
     * Randomly selects the literals to include in the diversification paths.
     * @return A vector with the literals selected.
     */
    private IVecInt selectDiversificationLits_naiveRandom() {
        IVecInt div_lits = new VecInt();
        int nvars = 2*VARS_PER_OBJECTIVE +
                    (this.instance.getMappings().size() > 0 ? VARS_PER_OBJECTIVE : 0) +
                    (!this.ignore_denominators ? VARS_PER_OBJECTIVE : 0);
        for (int i = 0; i < nvars; ++i) {
            int job_idx = PRNG.nextInt(0, this.job_vars.size()-1);
            int vm_idx = PRNG.nextInt(0, this.job_vars.get(job_idx).size()-1);
            int pm_idx = PRNG.nextInt(0, this.job_vars.get(job_idx).get(vm_idx).size()-1);
            int var = this.job_vars.get(job_idx).get(vm_idx).get(pm_idx);
            div_lits.push(PRNG.nextBoolean() ? var : -var);
        }
        return div_lits;
    }
    
    /**
     * Vector with the literals selected for the diversification paths.
     */
    private IVecInt div_lits = null;
    
    /**
     * Vector that stores the leaves of the diversification path tree in as they are generated.
     */
    private IVec<PathNode> div_paths = null;
    
    /**
     * Stores the nodes that correspond to the last path generated by {@link #nextDiversificationPath()}.
     */
    private IVec<PathNode> current_path = null;
    
    /**
     * Auxiliary vector of Booleans used by {@link #nextDiversificationPath()} to store if both branches
     * have been generated for each node in {@link #current_path}.
     */
    private IVec<Boolean> both_generated = null;
    
    /**
     * When all diversification paths have been generated by, stores the index in {@link #div_paths} of the
     * last path returned by {@link #nextDiversificationPath()}.
     */
    private int current_path_idx = -1;

    /**
     * Initializes lazy generation of diversification paths.
     */
    // TODO: add option to choose between multiple path diversification strategies
    protected void initDiversificationPaths() {
        this.div_lits = selectDiversificationLits_objectiveGuided();
        this.div_paths = new Vec<PathNode>();
        this.current_path = new Vec<PathNode>();
        this.both_generated = new Vec<Boolean>();
        this.current_path_idx = -1;
    }
    
    /**
     * Pushes a new literal at the end of the current diversification path.
     * @param lit The literal.
     */
    private void pushLastPathLiteral(int lit) {
        if (this.current_path.isEmpty()) {
            this.current_path.push(new PathNode(lit, new PathNode()));
        }
        else {
            this.current_path.push(new PathNode(lit, this.current_path.last()));
        }
        this.both_generated.push(false);
    }
    
    /**
     * Pops the literal in the current path that was pushed most recently.
     */
    private void popLastPathLiteral() {
        assert(!this.current_path.isEmpty());
        this.current_path.pop();
        this.both_generated.pop();
    }
    
    /**
     * Retrieves the leaf node of the current path.
     * @return The leaf node of the current path.
     */
    private PathNode getPathLeaf() { return this.current_path.last(); }
    
    /**
     * Checks if both branches have been generated for the literal in the current path that was pushed most
     * recently.
     * @return True if both branches have been generated, false otherwise.
     */
    private boolean lastPathLiteralBothBranchesGenerated() { return this.both_generated.last(); }
    
    /**
     * Switches the polarity of the literal in the current path that was pushed most recently.
     */
    private void lastPathLiteralSwitch() {
        int lit = this.current_path.last().getLit();
        popLastPathLiteral();
        pushLastPathLiteral(-lit);
        this.both_generated.set(this.both_generated.size()-1, true);
    }
    
    /**
     * Called by {@link #nextDiversificationPath()} when all diversification paths have been generated.
     */
    private void setAllPathsGenerated() { this.current_path = null; }
    
    /**
     * Checks if all diversification paths have been generated.
     * @return True if all paths have been generated, false otherwise.
     */
    private boolean allPathsGenerated() { return this.current_path == null; }
    
    /**
     * Lazily generates the next diversification path. If all paths have been generated, it cycles through
     * the generated paths. It starts with the path that is the negation of all literals selected for path
     * diversification, and then switches literals from last to first.
     * @return The leaf node of the next diversification path.
     */
    protected PathNode nextDiversificationPath() {
        PathNode path = null;
        assert(allPathsGenerated() || this.current_path.size() == this.both_generated.size());
        if (!allPathsGenerated() && !this.current_path.isEmpty()) {
            while (!this.current_path.isEmpty() && lastPathLiteralBothBranchesGenerated()) {
                popLastPathLiteral();
            }
            if (this.current_path.isEmpty()) {
                setAllPathsGenerated();
            }
        }
        if (allPathsGenerated()) {
            if (!this.div_paths.isEmpty()) {
                this.current_path_idx = (this.current_path_idx+1) % this.div_paths.size();
                path = this.div_paths.get(this.current_path_idx);
            }
        }
        else {
            if (this.current_path.size() == 0) {
                pushLastPathLiteral(-this.div_lits.get(0));
            }
            else {
                lastPathLiteralSwitch();
            }
            while(this.current_path.size() < this.div_lits.size()) {
                pushLastPathLiteral(-this.div_lits.get(this.current_path.size()));
            }
            path = getPathLeaf();
            this.div_paths.push(path);
        }
        return path;
    }
    
    /**
     * Discards the last diversification path returned by {@link #nextDiversificationPath()}.
     */
    protected void discardLastDiversificationPath() {
        if (this.current_path == null) {
            this.div_paths.set(this.current_path_idx, this.div_paths.last());
            this.div_paths.pop();
            this.current_path_idx =
                    (this.current_path_idx >= this.div_paths.size()) ? 0 : this.current_path_idx-1;
        }
        else {
            this.div_paths.pop();
        }
    }
    
    /**
     * Converts a given diversification path to a vector of assumptions that enforce that path.
     * @param node The path's leaf node.
     * @return The path's assumption vector.
     */
    protected IVecInt pathToAssumptions(PathNode node) {
        assert(node != null);
        IVecInt asms = new VecInt();
        while (!node.isRoot()) {
            asms.push(node.getLit());
            node = node.getParent();
        }
        return asms;
    }
    
    /**
     * Produces a string representation of a vector of literals.
     * @param lits The literal vector.
     * @return The vector's string representation.
     */
    protected String litsToString(IVecInt lits) {
        assert(lits.size() > 0);
        String path_str = "[" + lits.get(0);
        for (int i = 1; i < lits.size(); ++i) {
            path_str += ", " + lits.get(i);
        }
        path_str += "]";
        return path_str;
    }
    
    /**
     * Dumps the VMC instance to a given file as a Multi-Objective Combinatorial Optimization problem in
     * OPB format.
     * @param path The file's path.
     */
    // FIXME: as is, it's safer to dump and solve on separate runs
    public void dumpMOCO(String path) {
        OPBPrinter printer = new OPBPrinter();
        try {
            addVMCConstraints(printer);
        }
        catch (ContradictionException e) {
            throw new RuntimeException("Unreachable code was reached");     // printers shouldn't throw contradiction exceptions
        }
        initializeObjectiveFunctions();
        for (int i = 0; i < this.obj_functions.size(); ++i) {
            IVec<ObjectiveFunction> obj_vec = this.obj_functions.get(i);
            if (obj_vec.size() == 1) {
                ObjectiveFunction function = obj_vec.get(0);
                printer.addObjective(function.getLits(), function.getCoeffs());
            }
            else {
                IVec<IVecInt> num_lits = new Vec<IVecInt>(), den_lits = new Vec<IVecInt>();
                IVec<IVec<BigDecimal>> num_coeffs = new Vec<IVec<BigDecimal>>();
                IVec<IVec<BigDecimal>> den_coeffs = new Vec<IVec<BigDecimal>>();
                for (int j = 0; j < obj_vec.size(); ++j) {
                    ObjectiveFunction num = obj_vec.get(j++), den = obj_vec.get(j);
                    num_lits.push(num.getLits());
                    num_coeffs.push(num.getCoeffs());
                    den_lits.push(den.getLits());
                    IVec<BigDecimal> neg_coeffs = new Vec<BigDecimal>();
                    for (int k = 0; k < den.getCoeffs().size(); ++k) {
                        neg_coeffs.push(den.getCoeffs().get(k).negate());
                    }
                    den_coeffs.push(neg_coeffs);
                }
                printer.addObjective(num_lits, num_coeffs, den_lits, den_coeffs);
            }
        }
        printer.print(path);
    }
    
}
