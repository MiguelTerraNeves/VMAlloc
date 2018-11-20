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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.moeaframework.core.PRNG;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.constraints.ConstraintAggregator;
import vmalloc.algorithm.constraints.ConstraintID;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.JobVec;
import vmalloc.domain.Mapping;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.utils.CollectionUtils;

/**
 * Abstract superclass for Virtual Machine Consolidation problem solvers that rely on constraint solving.
 * @author Miguel Terra-Neves
 */
// TODO: store flat VM vars and delete flattenJobVars
public abstract class ConstraintBasedAllocAlgorithm extends AllocAlgorithm {
    
    /**
     * Value of the epsilon parameter used when computing the enumeration threshold in hash-based
     * algorithms. Refer to:
     * Chakraborty, Supratik, Kuldeep S. Meel, and Moshe Y. Vardi. "Algorithmic improvements in approximate
     * counting for probabilistic inference: From linear to logarithmic SAT calls." Proc. of IJCAI. 2016.
     */
    // TODO: turn configurable or adaptive
    private static final double EPSILON = 0.8;
    
    /**
     * Key size to use for the hash function in hash-based algorithms. Refer to:
     * Chakraborty, Supratik, Kuldeep S. Meel, and Moshe Y. Vardi. "Algorithmic improvements in approximate
     * counting for probabilistic inference: From linear to logarithmic SAT calls." Proc. of IJCAI. 2016.
     */
    // TODO: turn configurable or adaptive
    private static final int KEY_SIZE = 1;
    
    /**
     * Boolean indicating if hash functions are enabled or not.
     */
    private boolean hash_enabled = false;

    /**
     * Boolean indicating if symmetry breaking is enabled or not. When symmetry breaking is enabled,
     * additional constraints are added in order to discard equivalent symmetric solutions from the search
     * space. Refer to:
     * Aloul, Fadi A., Karem A. Sakallah, and Igor L. Markov. "Efficient symmetry breaking for boolean
     * satisfiability." IEEE Transactions on Computers 55.5 (2006): 549-558.
     */
    private boolean break_symms = false;
    
    /**
     * Vector of Boolean variables, one for each physical machine in the problem instance. Variable with
     * index {@code i} indicates if the physical machine with index {@code i} is ON or not.
     */
    protected IVecInt pm_vars = null;
    
    /**
     * Vector of vectors of vectors of Boolean variables. There is one vector of vectors for each job in the
     * problem instance. The {@code i}-th vector corresponds to the job with index {@code i}. The
     * {@code i}-th vector contains one vector of Boolean variables per virtual machine in the {@code i}-th
     * job. The {@code j}-th sub-vector corresponds to the virtual machine with index {@code j} in the
     * corresponding job. In each virtual machine's variable vector, there is one variable per physical
     * machine. These dictate to which physical machine the virtual machine is mapped to.
     */
    protected IVec<IVec<IVecInt>> job_vars = null;
    
    /**
     * Creates an instance of a constraint-based Virtual Machine Consolidation problem solver.
     * @param instance The problem instance.
     */
    public ConstraintBasedAllocAlgorithm(VMCwMProblem instance) { super(instance); }
    
    /**
     * Enables use of hash functions for algorithms that support it. 
     */
    public void enableHashFunctions() { this.hash_enabled = true; }
    
    /**
     * Enables symmetry breaking. Additional constraints are added in order to discard equivalent symmetric
     * solutions from the search space. Refer to:
     * Aloul, Fadi A., Karem A. Sakallah, and Igor L. Markov. "Efficient symmetry breaking for boolean
     * satisfiability." IEEE Transactions on Computers 55.5 (2006): 549-558.
     */
    public void enableSymmetryBreaking() { this.break_symms = true; }
    
    /**
     * Checks if the use of hash functions is enabled.
     * @return True if hash functions are enabled, false otherwise.
     */
    protected boolean hashFunctionsEnabled() { return this.hash_enabled; }
    
    /**
     * Checks if symmetry breaking is enabled.
     * @return True if symmetry breaking is enabled, false otherwise.
     */
    protected boolean symmetryBreakingEnabled() { return this.break_symms; }
    
    /**
     * Computes the enumeration threshold for hash-based algorithms. Refer to:
     * Chakraborty, Supratik, Kuldeep S. Meel, and Moshe Y. Vardi. "Algorithmic improvements in approximate
     * counting for probabilistic inference: From linear to logarithmic SAT calls." Proc. of IJCAI. 2016.
     * @param epsilon The epsilon parameter value.
     * @return The enumeration threshold.
     */
    private int computeEnumerationThreshold(double epsilon) {
        return (int)(1 + 9.84 * (1 + epsilon/(1+epsilon)) * (1 + 1/epsilon) * (1 + 1/epsilon));
    }
    
    /**
     * Retrieves the enumeration threshold for hash-based algorithms. Refer to:
     * Chakraborty, Supratik, Kuldeep S. Meel, and Moshe Y. Vardi. "Algorithmic improvements in approximate
     * counting for probabilistic inference: From linear to logarithmic SAT calls." Proc. of IJCAI. 2016.
     * @return The enumeration threshold.
     */
    protected int getEnumerationThreshold() { return computeEnumerationThreshold(EPSILON); }
    
    /**
     * Makes a fresh Boolean variable in a constraint aggregator.
     * @param aggr The constraint aggregator.
     * @return The fresh variable.
     */
    protected int newVar(ConstraintAggregator aggr) {
        aggr.newVars(1);
        return aggr.nVars();
    }
    
    /**
     * Makes a fresh Boolean variable in a constraint aggregator for each physical machine.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @return A vector with the fresh Boolean variables, one per physical machine.
     */
    protected IVecInt newVarsForPMs(ConstraintAggregator aggr, PhysicalMachineVec pms) {
        IVecInt pm_vars = new VecInt();
        int nvars = aggr.nVars();
        aggr.newVars(pms.size());
        for (int i = 1; i <= pms.size(); ++i) {
            pm_vars.push(nvars+i);
        }
        return pm_vars;
    }
    
    /**
     * Makes a fresh Boolean variable in a constraint aggregator for each possible virtual machine to physical
     * machine mapping.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param vms The virtual machines.
     * @return A vector of vectors of fresh Boolean variables, one vector per virtual machine, and within it
     * one variable per physical machine.
     */
    protected IVec<IVecInt> newVarsForVMs(ConstraintAggregator aggr,
                                          PhysicalMachineVec pms,
                                          VirtualMachineVec vms) {
        IVec<IVecInt> vm_vars = new Vec<IVecInt>();
        int new_vars = 0, nvars = aggr.nVars();
        for (int i = 0; i < vms.size(); ++i) {
            vm_vars.push(new VecInt());
            for (int j = 0; j < pms.size(); ++j) {
                vm_vars.get(i).push(++new_vars + nvars);
            }
        }
        aggr.newVars(new_vars);
        return vm_vars;
    }
    
    /**
     * Makes a fresh Boolean variable in a constraint aggregator for each possible virtual machine to physical
     * machine mapping.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param jobs The jobs.
     * @return A vector of vectors of vectors of fresh Boolean variables, one vector per job, within it
     * one vector per virtual machine in the job, and within the latter one fresh Boolean variable per
     * physical machine.
     */
    protected IVec<IVec<IVecInt>> newVarsForJobs(ConstraintAggregator aggr,
                                                 PhysicalMachineVec pms,
                                                 JobVec jobs) {
        IVec<IVec<IVecInt>> job_vars = new Vec<IVec<IVecInt>>();
        for (int i = 0; i < jobs.size(); ++i) {
            job_vars.push(newVarsForVMs(aggr, pms, jobs.get(i).virtualMachinesAsVec()));
            assert(jobs.get(i).nVirtualMachines() == job_vars.get(i).size());
        }
        return job_vars;
    }
    
    /**
     * Receives as input a vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintSolver, PhysicalMachineVec, JobVec)}, and returns a flat version of
     * the main vector. Also works if literals are provided instead of variables.
     * @param job_vars The vector of vectors of vectors of Boolean variables.
     * @return The flat vector.
     */
    protected IVec<IVecInt> flattenJobVars(IVec<IVec<IVecInt>> job_vars) {
        IVec<IVecInt> vm_vars = new Vec<IVecInt>();
        for (int i = 0; i < job_vars.size(); ++i) {
            for (int j = 0; j < job_vars.get(i).size(); ++j) {
                vm_vars.push(job_vars.get(i).get(j));
            }
        }
        return vm_vars;
    }
    
    /**
     * Flattens a vector of vectors of literals.
     * @param lits The vector of vectors of literals.
     * @return The flat vector of literals.
     */
    protected IVecInt flattenLitVectors(IVec<IVecInt> lits) {
        IVecInt flat_lits = new VecInt();
        for (int i = 0; i < lits.size(); ++i) {
            for (int j = 0; j < lits.get(i).size(); ++j) {
                flat_lits.push(lits.get(i).get(j));
            }
        }
        return flat_lits;
    }
    
    /**
     * Adds constraints to a constraint aggregator stating that the sum of the resource capacities of the
     * physical machines that are on must be at least as much as the sum of the resource requirements of the
     * virtual machines.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param pm_vars A Boolean variable for each machine. The value of each variable indicates if the
     * corresponding physical machine is ON or not.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addLowerBoundConstraints(ConstraintAggregator aggr,
                                            PhysicalMachineVec pms,
                                            IVecInt pm_vars) throws ContradictionException {
        aggr.addGreaterOrEqual(pm_vars,
                               new Vec<BigInteger>(pms.getCPUs()),
                               this.instance.getTotalCPURequirements());
        aggr.addGreaterOrEqual(pm_vars,
                               new Vec<BigInteger>(pms.getMemories()),
                               this.instance.getTotalMemoryRequirements());
    }
    
    /**
     * Adds constraints to a constraint aggregator forcing each virtual machine to be mapped to exactly one
     * physical machine.
     * @param aggr The constraint aggregator.
     * @param vm_vars A vector of vectors of Boolean variables, as produced by
     * {@link #newVarsForVMs(ConstraintAggregator, PhysicalMachineVec, VirtualMachineVec)}. The value of each
     * variable indicates if the corresponding virtual machine is mapped to a specific physical machine.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addExactlyOnePMConstraintsForVMs(ConstraintAggregator aggr, IVec<IVecInt> vm_vars)
            throws ContradictionException {
        for (int i = 0; i < vm_vars.size(); ++i) {
            aggr.addExactly(vm_vars.get(i), 1);
        }
    }
    
    /**
     * Adds constraints to a constraint aggregator forcing each virtual machine to be mapped to exactly one
     * physical machine.
     * @param aggr The constraint aggregator.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of each variable
     * indicates if the corresponding virtual machine is mapped to a specific physical machine.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addExactlyOnePMConstraints(
            ConstraintAggregator aggr, IVec<IVec<IVecInt>> job_vars) throws ContradictionException {
        for (int i = 0; i < job_vars.size(); ++i) {
            addExactlyOnePMConstraintsForVMs(aggr, job_vars.get(i));
        }
    }
    
    /**
     * Adds constraints to a constraint aggregator stating that the sum of the resource requirements of the
     * virtual machines mapped to a given physical machine cannot exceed its capacity.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param vms The virtual machines.
     * @param vm_vars A vector of vectors of Boolean variables, as produced by
     * {@link #newVarsForVMs(ConstraintAggregator, PhysicalMachineVec, VirtualMachineVec)}. The value of the
     * {@code i}-th variable in the {@code j}-th vector indicates if the {@code j}-th virtual machine in
     * {@code vms} is mapped to the {@code i}-th physical machine in {@code pms}.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addPMCapacityConstraintsForVMs(ConstraintAggregator aggr,
                                                  PhysicalMachineVec pms,
                                                  VirtualMachineVec vms,
                                                  IVec<IVecInt> vm_vars)
                                                          throws ContradictionException {
        IVecInt lits = new VecInt();
        for (int i = 0; i < pms.size(); ++i) {
            lits.clear();
            for (int j = 0; j < vm_vars.size(); ++j) {
                lits.push(vm_vars.get(j).get(i));
            }
            aggr.addLessOrEqual(lits, new Vec<BigInteger>(vms.getCPUs()), pms.get(i).getCPU());
            aggr.addLessOrEqual(lits, new Vec<BigInteger>(vms.getMemories()), pms.get(i).getMemory());
        }
    }
    
    /**
     * Adds constraints to a constraint aggregator stating that the sum of the resource requirements of the
     * virtual machines mapped to a given physical machine cannot exceed its capacity.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param jobs The jobs.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector indicates if the {@code j}-th
     * virtual machine in the {@code k}-th job in {@code jobs} is mapped to the {@code i}-th physical
     * machine in {@code pms}.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addPMCapacityConstraints(ConstraintAggregator aggr,
                                            PhysicalMachineVec pms,
                                            JobVec jobs,
                                            IVec<IVec<IVecInt>> job_vars)
                                                    throws ContradictionException {
        addPMCapacityConstraintsForVMs(aggr, pms, jobs.flattenJobs(), flattenJobVars(job_vars));
    }
    
    /**
     * Adds constraints to a constraint aggregator stating that, if at least one virtual machine is mapped to a
     * given physical machine, then that physical machine must be on.
     * @param solver The constraint solver.
     * @param pm_vars A Boolean variable for each machine. The value of each variable indicates if the
     * corresponding physical machine is ON or not.
     * @param vm_vars A vector of vectors of Boolean variables, as produced by
     * {@link #newVarsForVMs(ConstraintAggregator, PhysicalMachineVec, VirtualMachineVec)}. The value of the
     * {@code i}-th variable in the {@code j}-th vector in {@code vm_vars} indicates if the corresponding
     * virtual machine is mapped to the physical machine that corresponds to the {@code i}-th variable in
     * {@code pm_vars}.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addVarLinkConstraintsForVMs(ConstraintAggregator aggr,
                                               IVecInt pm_vars,
                                               IVec<IVecInt> vm_vars) throws ContradictionException {
        for (int i = 0; i < pm_vars.size(); ++i) {
            for (int j = 0; j < vm_vars.size(); ++j) {
                IVecInt clause = new VecInt();
                clause.push(-vm_vars.get(j).get(i));
                clause.push(pm_vars.get(i));
                aggr.addClause(clause);
            }
        }
    }
    
    /**
     * Adds constraints to a constraint aggregator stating that, if at least one virtual machine is mapped to a
     * given physical machine, then that physical machine must be on.
     * @param aggr The constraint aggregator.
     * @param pm_vars A Boolean variable for each machine. The value of each variable indicates if the
     * corresponding physical machine is ON or not.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the physical machine that corresponds to the
     * {@code i}-th variable in {@code pm_vars}.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addVarLinkConstraints(ConstraintAggregator aggr,
                                         IVecInt pm_vars,
                                         IVec<IVec<IVecInt>> job_vars)
                                                 throws ContradictionException {
        addVarLinkConstraintsForVMs(aggr, pm_vars, flattenJobVars(job_vars));
    }
    
    /**
     * Adds constraints to a constraint aggregator stating that anti-colocatable virtual machines in the same
     * job must be mapped to distinct servers.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param jobs The jobs.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addAntiColocationConstraints(ConstraintAggregator aggr,
                                                PhysicalMachineVec pms,
                                                JobVec jobs,
                                                IVec<IVec<IVecInt>> job_vars)
                                                        throws ContradictionException {
        for (int i = 0; i < jobs.size(); ++i) {
            IVec<IVecInt> anti_coloc_vm_vars = new Vec<IVecInt>();
            for (int j = 0; j < jobs.get(i).nVirtualMachines(); ++j) {
                if (jobs.get(i).getVirtualMachine(j).isAntiColocatable()) {
                    anti_coloc_vm_vars.push(job_vars.get(i).get(j));
                }
            }
            for (int j = 0; j < pms.size(); ++j) {
                IVecInt cons_vars = new VecInt();
                for (int k = 0; k < anti_coloc_vm_vars.size(); ++k) {
                    cons_vars.push(anti_coloc_vm_vars.get(k).get(j));
                }
                if (cons_vars.size() > 1) {
                    aggr.addAtMost(cons_vars, 1);
                }
            }
        }
    }
    
    /**
     * Adds constraints to a constraint aggregator stating that virtual machines can only be mapped to physical
     * machines where they are allowed to run.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param jobs The jobs.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    // FIXME: missing addPlatformConstraintsForVMs
    protected void addPlatformConstraints(ConstraintAggregator aggr,
                                          PhysicalMachineVec pms,
                                          JobVec jobs,
                                          IVec<IVec<IVecInt>> job_vars)
                                                  throws ContradictionException {
        Map<Integer, Integer> pm_id_to_idx = CollectionUtils.makePhysicalMachineIDtoIndexMap(pms);
        for (int i = 0; i < jobs.size(); ++i) {
            for (int j = 0; j < jobs.get(i).nVirtualMachines(); ++j) {
                VirtualMachine vm = jobs.get(i).getVirtualMachine(j);
                for (int k = 0; k < vm.getUnallowedPhysicalMachines().size(); ++k) {
                    Integer unallowed_id =
                            new Integer(vm.getUnallowedPhysicalMachines().get(k).getID());
                    if (pm_id_to_idx.containsKey(unallowed_id)) {
                        IVecInt unit_cl = new VecInt();
                        unit_cl.push(-job_vars.get(i).get(j).get(pm_id_to_idx.get(unallowed_id)));
                        aggr.addClause(unit_cl);
                    }
                }
            }
        }
    }
    
    /**
     * Adds a constraint to a constraint aggregator stating that the sum of the memory requirements of
     * pre-allocated virtual machines migrated to some other physical machine cannot exceed a given
     * percentile of the total memory capacity of the physical machines.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param vms The virtual machines.
     * @param vm_vars A vector of vectors of Boolean variables, as produced by
     * {@link #newVarsForVMs(ConstraintAggregator, PhysicalMachineVec, VirtualMachineVec)}. The value of the
     * {@code i}-th variable in the {@code j}-th vector in {@code vm_vars} indicates if the corresponding
     * virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @param mappings The pre-existing mappings of virtual machines to physical machines.
     * @param max_mig_percentile The percentile of the total memory capacity allotted for migrations.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addMigrationConstraintForVMs(ConstraintAggregator aggr,
                                                PhysicalMachineVec pms,
                                                VirtualMachineVec vms,
                                                IVec<IVecInt> vm_vars,
                                                MappingVec mappings,
                                                double max_mig_percentile)
                                                        throws ContradictionException {
        if (mappings.size() > 0) {
            assert(max_mig_percentile >= 0.0 && max_mig_percentile <= 1.0);
            Map<Integer, Integer> pm_id_to_idx = CollectionUtils.makePhysicalMachineIDtoIndexMap(pms);
            Map<String, Integer> vm_id_to_idx = CollectionUtils.makeVirtualMachineIDtoIndexMap(vms);
            IVecInt lits = new VecInt();
            IVec<BigInteger> coeffs = new Vec<BigInteger>();
            BigDecimal total_mem_cap = new BigDecimal(this.instance.getTotalMemoryCapacity());
            BigDecimal mig_percentile = new BigDecimal(max_mig_percentile);
            BigInteger rhs = total_mem_cap.multiply(mig_percentile).toBigInteger();
            for (int i = 0; i < mappings.size(); ++i) {
                VirtualMachine vm = mappings.get(i).getVirtualMachine();
                PhysicalMachine pm = mappings.get(i).getPhysicalMachine();
                int vm_idx = vm_id_to_idx.get(vm.getID()).intValue();
                int pm_idx = pm_id_to_idx.get(new Integer(pm.getID())).intValue();
                coeffs.push(vm.getMemory());
                lits.push(-vm_vars.get(vm_idx).get(pm_idx));
            }
            aggr.addLessOrEqual(lits, coeffs, rhs);
        }
    }
    
    /**
     * Adds a constraint to a constraint aggregator stating that the sum of the memory requirements of
     * pre-allocated virtual machines migrated to some other physical machine cannot exceed a given
     * percentile of the total memory capacity of the physical machines.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param jobs The jobs.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @param mappings The pre-existing mappings of virtual machines to physical machines.
     * @param max_mig_percentile The percentile of the total memory capacity allotted for migrations.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addMigrationConstraint(ConstraintAggregator aggr,
                                          PhysicalMachineVec pms,
                                          JobVec jobs,
                                          IVec<IVec<IVecInt>> job_vars,
                                          MappingVec mappings,
                                          double max_mig_percentile)
                                                  throws ContradictionException {
        addMigrationConstraintForVMs(aggr,
                                     pms,
                                     jobs.flattenJobs(),
                                     flattenJobVars(job_vars),
                                     mappings,
                                     max_mig_percentile);
    }
    
    /**
     * Adds symmetry breaking constraints to a constraint aggregator. These constraints discard unnecessary
     * symmetric solutions from the search space. Refer to:
     * Aloul, Fadi A., Karem A. Sakallah, and Igor L. Markov. "Efficient symmetry breaking for boolean
     * satisfiability." IEEE Transactions on Computers 55.5 (2006): 549-558.
     * @param aggr The constraint aggregator.
     * @param pms The physical machines.
     * @param pm_vars A Boolean variable for each machine. The value of each variable indicates if the
     * corresponding physical machine is ON or not.
     * @param jobs The jobs.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintAggregator, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @param mappings The pre-existing mappings of virtual machines to physical machines.
     * @throws ContradictionException If the constraint aggregator detects that the addition of the constraints
     * would result in a contradiction.
     */
    protected void addSymmetryBreakingConstraints(ConstraintAggregator aggr,
                                                  PhysicalMachineVec pms,
                                                  IVecInt pm_vars,
                                                  JobVec jobs,
                                                  IVec<IVec<IVecInt>> job_vars,
                                                  MappingVec mappings)
                                                          throws ContradictionException {
        Map<VirtualMachine, PhysicalMachine> mapping_map =
                new HashMap<VirtualMachine, PhysicalMachine>();
        for (int i = 0; i < mappings.size(); ++i) {
            assert (!mapping_map.containsKey(mappings.get(i).getVirtualMachine()));
            mapping_map.put(mappings.get(i).getVirtualMachine(), mappings.get(i).getPhysicalMachine());
        }
        // Partition VMs into sets of symmetric VMs
        VirtualMachineVec vms = jobs.flattenJobs();
        IVec<IVecInt> vm_vars = flattenJobVars(job_vars);
        IVec<VirtualMachineVec> sym_groups = new Vec<VirtualMachineVec>();
        IVec<IVec<IVecInt>> sym_group_vars = new Vec<IVec<IVecInt>>();
        for (int i = 0; i < vms.size(); ++i) {
            VirtualMachine vm = vms.get(i);
            int j;
            for (j = 0; j < sym_groups.size(); ++j) {
                VirtualMachine rep_vm = sym_groups.get(j).get(0);
                if (((rep_vm.getJobID() == vm.getJobID() && rep_vm.isAntiColocatable() == vm.isAntiColocatable())
                        || (!rep_vm.isAntiColocatable() && !vm.isAntiColocatable()))
                        && rep_vm.getCPU().equals(vm.getCPU()) && rep_vm.getMemory().equals(vm.getMemory())
                        && mappedToSamePhysicalMachine(rep_vm, vm, mapping_map)) {
                    sym_groups.get(j).push(vm);
                    sym_group_vars.get(j).push(vm_vars.get(i));
                    break;
                }
            }
            if (j == sym_groups.size()) {
                VirtualMachineVec new_group = new VirtualMachineVec();
                IVec<IVecInt> new_group_vars = new Vec<IVecInt>();
                new_group.push(vm);
                new_group_vars.push(vm_vars.get(i));
                sym_groups.push(new_group);
                sym_group_vars.push(new_group_vars);
            }
        }
        // Add symmetry breaking constraints for VMs
        for (int i = 0; i < sym_groups.size(); ++i) {
            VirtualMachineVec sym_group = sym_groups.get(i);
            IVec<IVecInt> group_vars = sym_group_vars.get(i);
            boolean simplify = sym_group.get(0).isAntiColocatable();
            for (int j = 0; j < sym_group.size()-1; ++j) {
                VirtualMachine vm1 = sym_group.get(j);
                VirtualMachine vm2 = sym_group.get(j+1);
                IVecInt vm1_vars = group_vars.get(j);
                IVecInt vm2_vars = group_vars.get(j+1);
                IVecInt sym_pm_indexes = intersectAllowedPhysicalMachineIndexes(pms, vm1, vm2);
                simplify = simplify && sym_pm_indexes.size() == pms.size(); // FIXME: possible to simplify in more cases, but not trivial
                for (int k1 = 0; k1 < sym_pm_indexes.size(); ++k1) {
                    for (int k2 = 0; k2 <= k1; ++k2) {
                        if (k2 < k1 || vm1.isAntiColocatable()) {
                            IVecInt clause = new VecInt();
                            clause.push(-vm1_vars.get(sym_pm_indexes.get(k1)));
                            clause.push(-vm2_vars.get(sym_pm_indexes.get(k2)));
                            aggr.addClause(clause);
                        }
                    }
                }
            }
            if (simplify) {
                for (int j = 0; j < sym_group.size(); ++j) {
                    IVecInt vmj_vars = group_vars.get(j);
                    for (int k = 0; k < j; ++k) {
                        IVecInt unit_cl = new VecInt();
                        unit_cl.push(-vmj_vars.get(k));
                        aggr.addClause(unit_cl);
                    }
                    for (int k = 0; k < sym_group.size()-j-1; ++k) {
                        IVecInt unit_cl = new VecInt();
                        unit_cl.push(-vmj_vars.get(pms.size()-k-1));
                        aggr.addClause(unit_cl);
                    }
                }
            }
        }
    }
    
    /**
     * Initializes a constraint aggregator with the base variables and constraints common to all (or most)
     * constraint-based algorithms. This method creates and stores the Boolean variables in the
     * {@link #pm_vars} and {@link #job_vars} vectors.
     * @param aggr The constraint aggregator.
     * @throws ContradictionException If the constraint aggregator detects that the base formula is
     * unsatisfiable.
     */
    protected void addVMCConstraints(ConstraintAggregator aggr) throws ContradictionException {
        System.out.println("c Initializing constraint aggregator with base formula");
        this.pm_vars = newVarsForPMs(aggr, this.instance.getPhysicalMachines());
        this.job_vars = newVarsForJobs(aggr,
                                       this.instance.getPhysicalMachines(),
                                       this.instance.getJobs());
        addLowerBoundConstraints(aggr, this.instance.getPhysicalMachines(), this.pm_vars);
        addExactlyOnePMConstraints(aggr, this.job_vars);
        addPMCapacityConstraints(aggr,
                                 this.instance.getPhysicalMachines(),
                                 this.instance.getJobs(),
                                 this.job_vars);
        addVarLinkConstraints(aggr, this.pm_vars, this.job_vars);
        addAntiColocationConstraints(aggr,
                                     this.instance.getPhysicalMachines(),
                                     this.instance.getJobs(),
                                     this.job_vars);
        addPlatformConstraints(aggr,
                               this.instance.getPhysicalMachines(),
                               this.instance.getJobs(),
                               this.job_vars);
        addMigrationConstraint(aggr,
                               this.instance.getPhysicalMachines(),
                               this.instance.getJobs(),
                               this.job_vars,
                               this.instance.getMappings(),
                               this.instance.getMaxMigrationPercentile());
        if (symmetryBreakingEnabled()) {
            addSymmetryBreakingConstraints(aggr,
                                           this.instance.getPhysicalMachines(),
                                           this.pm_vars,
                                           this.instance.getJobs(),
                                           this.job_vars,
                                           this.instance.getMappings());
        }
        System.out.println("c Done");
        printElapsedTime();
    }
    
    /**
     * Adds the contents of a constraint ID vector to another constraint ID vector.
     * @param source The source vector.
     * @param dest The destination vector.
     */
    protected void addVecToVec(IVec<ConstraintID> source, IVec<ConstraintID> dest) {
        for (int i = 0; i < source.size(); ++i) {
            dest.push(source.get(i));
        }
    }

    /**
     * Adds a random hash function constraint to a constraint solver.
     * @param solver The constraint solver.
     * @param lits The hash function argument literals.
     * @param asms An output vector where the activator literals for the hash function are stored.
     * @return A vector of constraint IDs that must be removed in order to fully remove the hash function
     * from the solver.
     */
    protected IVec<ConstraintID> setHashFunction(ConstraintSolver solver,
                                                 IVecInt lits,
                                                 IVecInt asms) {
        IVec<ConstraintID> ids = new Vec<ConstraintID>();
        boolean done = false;
        while (!done) {
            asms.clear();
            try {
                for (int i = 0; i < KEY_SIZE; ++i) {
                    boolean key_bit = PRNG.nextBoolean(), alpha_0 = PRNG.nextBoolean();
                    IVecInt bit_lits = new VecInt();
                    for (int j = 0; j < lits.size(); ++j) {
                        if (PRNG.nextBoolean()) {
                            bit_lits.push(lits.get(j));
                        }
                    }
                    int activator = newVar(solver);
                    addVecToVec(solver.addRemovableXOR(bit_lits, alpha_0 != key_bit, activator), ids);
                    asms.push(activator);
                }
                done = true;
            }
            catch (ContradictionException ce) {
                System.out.println("c Hash function led to empty cell, generating another one");
                solver.removeConstraints(ids);
                ids.clear();
            }
        }
        return ids;
    }

    /**
     * Adds a random hash function constraint to a constraint solver.
     * @param solver The constraint solver.
     * @param lits The hash function argument literals.
     * @return A vector of constraint IDs that must be removed in order to fully remove the hash function
     * from the solver.
     */
    protected IVec<ConstraintID> setHashFunction(ConstraintSolver solver, IVecInt lits) {
        IVec<ConstraintID> ids = new Vec<ConstraintID>();
        IVecInt activators = new VecInt();
        boolean done = false;
        while (!done) {
            try {
                ids = setHashFunction(solver, lits, activators);
                for (int i = 0; i < activators.size(); ++i) {
                    solver.addClause(new VecInt(new int[] { activators.get(i) }));
                }
                done = true;
            }
            catch (ContradictionException ce) {
                System.out.println("c Hash function led to empty cell, generating another one");
                solver.removeConstraints(ids);
                ids.clear();
                activators.clear();
            }
        }
        return ids;
    }
    
    /**
     * Retrieves the subset of literals, from a vector, satisfied by a constraint solver's current model.
     * It is assumed that the solver's {@link ConstraintSolver#solve()} or
     * {@link ConstraintSolver#solve(IVecInt)} was called before calling this function, and that
     * {@link ConstraintSolver#isSatisfiable()} returned true.
     * @param solver The constraint solver.
     * @param undef_fmls The vector of literals.
     * @return A vector with the subsets of literals satisfied by the solver's current model.
     */
    protected IVecInt extractSatisfied(ConstraintSolver solver, IVecInt undef_fmls) {
        assert(solver.isSolved() && solver.isSatisfiable());
        IVecInt sat = new VecInt();
        int i = 0;
        while (i < undef_fmls.size()) {
            if (    (undef_fmls.get(i) < 0 && !solver.modelValue(-undef_fmls.get(i)) ||
                    (undef_fmls.get(i) > 0 && solver.modelValue(undef_fmls.get(i))))) {
                sat.push(undef_fmls.get(i));
                undef_fmls.set(i, undef_fmls.get(undef_fmls.size()-1));
                undef_fmls.pop();
            }
            else {
                i++;
            }
        }
        return sat;
    }
    
    /**
     * Retrieves the indexes of the physical machines that are set to ON in a constraint solver's current
     * model. It is assumed that the solver's {@link ConstraintSolver#solve()} or
     * {@link ConstraintSolver#solve(IVecInt)} was called before calling this function, and that
     * {@link ConstraintSolver#isSatisfiable()} returned true.
     * @param solver The constraint solver.
     * @param pms The physical machines.
     * @param vm_vars A vector of vectors of Boolean variables, as produced by
     * {@link #newVarsForVMs(ConstraintSolver, PhysicalMachineVec, VirtualMachineVec)}. The value of the
     * {@code i}-th variable in the {@code j}-th vector in {@code vm_vars} indicates if the corresponding
     * virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @return A set with the indexes of the physical machines in {@code pms} set to ON by the solver.
     */
    protected Set<Integer> getUsedPhysicalMachineIndexesForVMs(ConstraintSolver solver,
                                                               PhysicalMachineVec pms,
                                                               IVec<IVecInt> vm_vars) {
        assert(solver.isSolved() && solver.isSatisfiable());
        Set<Integer> indexes = new HashSet<Integer>();
        for (int i = 0; i < pms.size(); ++i) {
            for (int j = 0; j < vm_vars.size(); ++j) {
                if (solver.modelValue(vm_vars.get(j).get(i))) {
                    indexes.add(new Integer(i));
                    break;
                }
            }
        }
        return indexes;
    }
    
    /**
     * Retrieves the indexes of the physical machines that are set to ON in a constraint solver's current
     * model. It is assumed that the solver's {@link ConstraintSolver#solve()} or
     * {@link ConstraintSolver#solve(IVecInt)} was called before calling this function, and that
     * {@link ConstraintSolver#isSatisfiable()} returned true.
     * @param solver The constraint solver.
     * @param pms The physical machines.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintSolver, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @return A set with the indexes of the physical machines in {@code pms} set to ON by the solver.
     */
    protected Set<Integer> getUsedPhysicalMachineIndexes(ConstraintSolver solver,
                                                         PhysicalMachineVec pms,
                                                         IVec<IVec<IVecInt>> job_vars) {
        return getUsedPhysicalMachineIndexesForVMs(solver, pms, flattenJobVars(job_vars));
    }
    
    /**
     * Retrieves the number of physical machines that are set to ON in a constraint solver's current model.
     * It is assumed that the solver's {@link ConstraintSolver#solve()} or
     * {@link ConstraintSolver#solve(IVecInt)} was called before calling this function, and that
     * {@link ConstraintSolver#isSatisfiable()} returned true.
     * @param solver The constraint solver.
     * @param pms The physical machines.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintSolver, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @return The number of physical machines set to ON by the solver.
     */
    protected int getUsedPMsCount(ConstraintSolver solver,
                                  PhysicalMachineVec pms,
                                  IVec<IVec<IVecInt>> job_vars) {
        return getUsedPhysicalMachineIndexes(solver, pms, job_vars).size();
    }
    
    /**
     * Converts a constraint solver's current model to an allocation (vector of virtual machine to physical
     * machine mappings). It is assumed that the solver's {@link ConstraintSolver#solve()} or
     * {@link ConstraintSolver#solve(IVecInt)} was called before calling this function, and that
     * {@link ConstraintSolver#isSatisfiable()} returned true.
     * @param solver The constraint solver.
     * @param pms The physical machines.
     * @param vms The virtual machines.
     * @param vm_vars A vector of vectors of Boolean variables, as produced by
     * {@link #newVarsForVMs(ConstraintSolver, PhysicalMachineVec, VirtualMachineVec)}. The value of the
     * {@code i}-th variable in the {@code j}-th vector in {@code vm_vars} indicates if the {@code j}-th
     * virtual machine in {@code vms} is mapped to the {@code i}-th physical machine in {@code pms}.
     * @return The allocation computed by the solver.
     */
    protected MappingVec modelToAllocationForVMs(ConstraintSolver solver,
                                                 PhysicalMachineVec pms,
                                                 VirtualMachineVec vms,
                                                 IVec<IVecInt> vm_vars) {
        assert(solver.isSolved() && solver.isSatisfiable());
        MappingVec allocation = new MappingVec();
        for (int i = 0; i < vm_vars.size(); ++i) {
            for (int j = 0; j < vm_vars.get(i).size(); ++j) {
                if (solver.modelValue(vm_vars.get(i).get(j))) {
                    allocation.push(new Mapping(vms.get(i), pms.get(j)));
                    break;
                }
            }
        }
        return allocation;
    }
    
    /**
     * Converts a constraint solver's current model to an allocation (vector of virtual machine to physical
     * machine mappings). It is assumed that the solver's {@link ConstraintSolver#solve()} or
     * {@link ConstraintSolver#solve(IVecInt)} was called before calling this function, and that
     * {@link ConstraintSolver#isSatisfiable()} returned true.
     * @param solver The constraint solver.
     * @param pms The physical machines.
     * @param jobs The jobs.
     * @param job_vars A vector of vectors of vectors of Boolean variables, as produced by
     * {@link #newVarsForJobs(ConstraintSolver, PhysicalMachineVec, JobVec)}. The value of the {@code i}-th
     * variable in the {@code j}-th sub-vector in the {@code k}-th vector in {@code job_vars} indicates if
     * the corresponding virtual machine is mapped to the {@code i}-th physical machine in {@code pms}.
     * @return
     */
    protected MappingVec modelToAllocation(ConstraintSolver solver,
                                           PhysicalMachineVec pms,
                                           JobVec jobs,
                                           IVec<IVec<IVecInt>> job_vars) {
        return modelToAllocationForVMs(solver, pms, jobs.flattenJobs(), flattenJobVars(job_vars));
    }

    /**
     * Checks the satisfiability of the formula in a constraint solver.
     * @param solver The constraint solver.
     */
    protected void checkSAT(ConstraintSolver solver) { checkSAT(solver, new VecInt()); }
    
    /**
     * Checks the satisfiability of the formula in a constraint solver under a set of assumptions
     * (additional literals that must be satisfied by the solver).
     * @param solver The constraint solver.
     * @param asms The assumptions.
     */
    protected void checkSAT(ConstraintSolver solver, IVecInt asms) {
        if (getTimeout() != NO_TIMEOUT) {
            assert(getTimeout() > 0);
            solver.setTimeout((int)getRemainingTime());
        }
        solver.solve(asms);
    }
    
    /**
     * Prints a message stating that an instance is unsatisfiable and the elapsed time.
     */
    protected void printUnsatisfiable() {
        System.out.println("c Instance is unsatisfiable");
        printElapsedTime();
    }
    
    /**
     * Prints a message stating that optimality was proven and the elapsed time.
     */
    protected void printOptimum() {
        System.out.println("c Proved optimality");
        printElapsedTime();
    }
    
    /**
     * Prints a message stating that the timeout was triggered and the elapsed time.
     */
    protected void printTimeoutMessage() {
        System.out.println("c TIMEOUT!");
        printElapsedTime();
    }
    
    /**
     * Prints a message stating that a solution with a given number of physical machines set to ON was found
     * and the elapsed time.
     * @param used The number of physical machines set to ON.
     */
    protected void printUsedPMsCount(int used) {
        System.out.println("c Solution using " + used + " PMs found");
        printElapsedTime();
    }
    
}