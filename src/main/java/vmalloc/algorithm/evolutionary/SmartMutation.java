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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variation;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.ParetoCLD;
import vmalloc.algorithm.constraints.ConstraintID;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.domain.JobVec;
import vmalloc.domain.Mapping;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.utils.IOUtils;

/**
 * Class that implements the constraint-based smart mutation and improvement operators published in:
 * Terra-Neves, Miguel, Inês Lynce, and Vasco Manquinho. "Integrating Pseudo-Boolean Constraint Reasoning in
 * Multi-Objective Evolutionary Algorithms." Proc. of IJCAI. 2019.
 * @author Miguel Terra-Neves
 */
public class SmartMutation extends ParetoCLD implements Variation {

    /**
     * Stores the probability of applying smart mutation.
     */
    private final double probability;
    
    /**
     * Stores if domain knowledge should be exploited in order to determine which variables to unassign
     * before smart mutation.
     */
    private final boolean domain_unfix;
    
    /**
     * Stores if smart improvement is to be applied when a given individual is already satisfiable.
     */
    private final boolean smart_improve;
    
    /**
     * Stores the maximum number of conflicts allowed for smart mutation.
     */
    private long max_conflicts = NO_TIMEOUT;
    
    /**
     * Stores the maximum number of conflicts allowed for smart improvement.
     */
    private long improve_max_conflicts = NO_TIMEOUT;
    
    /**
     * Stores the fraction of servers that should be displaced before applying smart improvement.
     */
    private double improve_relax_rate = 0.5;
    
    /**
     * Stores the constraint solver used by smart mutation and improvement.
     */
    private ConstraintSolver solver = null;
    
    /**
     * Creates an instance of the smart mutation operator.
     * @param probability The probability of applying smart mutation.
     * @param domain_unfix True if domain knowledge is to be exploited in order to determine which variables
     * to unassign before smart mutation, false otherwise.
     * @param smart_improve True if smart improvement is to be applied when given an already feasible
     * individual, false otherwise.
     * @param instance The Virtual Machine Consolidation problem instance.
     */
    public SmartMutation(double probability, boolean domain_unfix, boolean smart_improve, Problem instance) {
        super((VMCwMProblem)instance);
        this.probability = probability;
        this.domain_unfix = domain_unfix;
        this.smart_improve = smart_improve;
        this.enableStratification(true);
        if (this.probability > 0.0) {   // solver isn't needed if probability is 0.0
            try {
                this.solver = buildSolver();
                initializeObjectiveFunctions();
            }
            catch (ContradictionException ce) {
                System.out.println("c Contradiction building solver for smart mutation");
                printUnsatisfiable();
                System.exit(0);
            }
        }
    }
    
    /**
     * Set the maximum number of conflicts allowed for smart mutation.
     * @param max_conflicts The maximum number of conflicts.
     */
    public void setMaxConflicts(long max_conflicts) { this.max_conflicts = max_conflicts; }
    
    /**
     * Set the maximum number of conflicts allowed for smart improvement.
     * @param max_conflicts The maximum number of conflicts.
     */
    public void setImprovementMaxConflicts(long max_conflicts) { this.improve_max_conflicts = max_conflicts; }
    
    /**
     * Smart improvement is implemented using the stratified Pareto-MCS algorithm. This method sets the
     * number of conflicts allowed before some partition is merged with the next one.
     * @param max_conflicts The maximum number of conflicts.
     */
    public void setImprovementPartMaxConflicts(long max_conflicts) { super.setPartMaxConflicts(max_conflicts); }
    
    /**
     * Set the fraction of servers to be displaced before applying smart improvement.
     * @param rate The fraction.
     */
    public void setImprovementRelaxRate(double rate) { this.improve_relax_rate = rate; }
    
    /**
     * Given a solution, retrieves the set of indexes of the virtual machines that are believed to be
     * responsible for constraint violations.
     * @param sol 
     * @return The set of violating virtual machine indexes.
     */
    // TODO: some parts are very similar to VMCwMProblem.evaluate; refactor?
    private Set<Integer> getViolatingVirtualMachineIndexes(Solution sol) {
        int[] x = this.instance.getVirtualMachineAssignment(sol);
        PhysicalMachineVec pms = this.instance.getPhysicalMachines();
        VirtualMachineVec vms = this.instance.getVirtualMachines();
        Set<Integer> viol_vms = new HashSet<Integer>();
        // Check capacity constraint violations
        IVec<IVecInt> vms_in_pm_vecs = new Vec<IVecInt>(pms.size());
        BigInteger[] used_cpu_caps = new BigInteger[pms.size()];
        BigInteger[] used_mem_caps = new BigInteger[pms.size()];
        for (int i = 0; i < pms.size(); ++i) {
            vms_in_pm_vecs.unsafePush(new VecInt());
            used_cpu_caps[i] = BigInteger.ZERO;
            used_mem_caps[i] = BigInteger.ZERO;
        }
        for (int i = 0; i < vms.size(); ++i) {
            int pm_idx = x[i];
            vms_in_pm_vecs.get(pm_idx).push(i);
            used_cpu_caps[pm_idx] = used_cpu_caps[pm_idx].add(vms.get(i).getCPU());
            used_mem_caps[pm_idx] = used_mem_caps[pm_idx].add(vms.get(i).getMemory());
        }
        for (int i = 0; i < pms.size(); ++i) {
            if (    used_cpu_caps[i].compareTo(pms.get(i).getCPU()) > 0 ||
                    used_mem_caps[i].compareTo(pms.get(i).getMemory()) > 0) {
                IVecInt vms_in_pm = vms_in_pm_vecs.get(i);
                for (int j = 0; j < vms_in_pm.size(); ++j) {
                    viol_vms.add(vms_in_pm.get(j));
                }
            }
        }
        // Check anti-colocation constraint violations
        IVec<VirtualMachineVec> anti_coloc_vms = this.instance.getAntiColocatableVirtualMachines();
        Set<PhysicalMachine> used_pms = new HashSet<PhysicalMachine>();
        Set<PhysicalMachine> dup_pms = new HashSet<PhysicalMachine>();
        for (int i = 0; i < anti_coloc_vms.size(); ++i) {
            for (int j = 0; j < anti_coloc_vms.get(i).size(); ++j) {
                VirtualMachine vm = anti_coloc_vms.get(i).get(j);
                PhysicalMachine pm = pms.get(x[this.instance.getVirtualMachineIndex(vm)]);
                if (used_pms.contains(pm)) {
                    dup_pms.add(pm);
                }
                used_pms.add(pm);
            }
            for (int j = 0; j < anti_coloc_vms.get(i).size(); ++j) {
                VirtualMachine vm = anti_coloc_vms.get(i).get(j);
                int vm_idx = this.instance.getVirtualMachineIndex(vm);
                PhysicalMachine pm = pms.get(x[vm_idx]);
                if (dup_pms.contains(pm)) {
                    viol_vms.add(vm_idx);
                }
            }
            used_pms.clear();
            dup_pms.clear();
        }
        // Check platform constraint violations
        VirtualMachineVec plat_constr_vms = this.instance.getPlatformConstrainedVirtualMachines();
        for (int i = 0; i < plat_constr_vms.size(); ++i) {
            VirtualMachine vm = plat_constr_vms.get(i);
            int vm_idx = this.instance.getVirtualMachineIndex(vm);
            if (!vm.canRunInPhysicalMachine(pms.get(x[vm_idx]))) {
                viol_vms.add(vm_idx);
            }
        }
        // Check migration constraint violation
        if (sol.getNumberOfObjectives() == 3) {
            BigInteger migged_mem = BigInteger.ZERO;
            MappingVec mappings = this.instance.getMappings();
            for (int i = 0; i < mappings.size(); ++i) {
                VirtualMachine vm = mappings.get(i).getVirtualMachine();
                PhysicalMachine pm = mappings.get(i).getPhysicalMachine();
                if (x[this.instance.getVirtualMachineIndex(vm)] != this.instance.getPhysicalMachineIndex(pm)) {
                    migged_mem = migged_mem.add(vm.getMemory());
                }
            }
            if (migged_mem.compareTo(this.instance.getMaxMigrationMemory()) > 0) {
                for (int i = 0; i < mappings.size(); ++i) {
                    VirtualMachine vm = mappings.get(i).getVirtualMachine();
                    PhysicalMachine pm = mappings.get(i).getPhysicalMachine();
                    int vm_idx = this.instance.getVirtualMachineIndex(vm);
                    if (x[vm_idx] != this.instance.getPhysicalMachineIndex(pm)) {
                        viol_vms.add(vm_idx);
                    }
                }
            }
        }
        return viol_vms;
    }

    /**
     * Builds a vector with assumption literals that enforce the placement of virtual machines given by
     * some solution, with the exception of the virtual machines in a given set.
     * @param sol The solution.
     * @param relax_vms A set of indexes of virtual machines whose placement is not to be enforced by the
     * assumptions.
     * @return The vector of assumption literals.
     */
    private IVecInt buildAssumptions(Solution sol, Set<Integer> relax_vms) {
        int[] x = this.instance.getVirtualMachineAssignment(sol);
        IVecInt asms = new VecInt();
        IVec<IVecInt> vm_vars = flattenJobVars(this.job_vars);
        IVec<IVecInt> aux_vm_plus_vars = flattenJobVars(this.aux_job_plus_vars);
        IVec<IVecInt> aux_vm_minus_vars = flattenJobVars(this.aux_job_minus_vars);
        for (int i = 0; i < vm_vars.size(); ++i) {
            if (!relax_vms.contains(i)) {
                for (int j = 0; j < vm_vars.get(i).size(); ++j) {
                    if (x[i] == j) {
                        asms.push(vm_vars.get(i).get(j));   // cannot add assumptions for auxiliary variables here
                    }                                       // because their values depend on the hosting server's
                    else {                                  // wastage
                        asms.push(-vm_vars.get(i).get(j));
                        asms.push(-aux_vm_plus_vars.get(i).get(j));
                        asms.push(-aux_vm_minus_vars.get(i).get(j));
                    }
                }
            }
        }
        return asms;
    }
    
    /**
     * Discards assumption literals present in the unsatisfiable core given by some constraint solver.
     * @param solver The constraint solver.
     * @param asms The assumption literals.
     */
    private void removeCoreLits(ConstraintSolver solver, IVecInt asms) {
        assert(solver.isSolved() && solver.isUnsatisfiable());
        IVecInt core = solver.unsatExplanation();
        if (core.isEmpty()) {
            System.out.println("c UNSAT instance that does not depend on assumptions during smart mutation");
            printOptimum();     // TODO: whole instance may be UNSAT; must check
            System.exit(0);     // TODO: .pop file won't be created; must implement shutdown handler
        }
        Set<Integer> core_lits = new HashSet<Integer>();
        for (int i = 0; i < core.size(); ++i) {
            core_lits.add(core.get(i));
        }
        for (int i = 0; i < asms.size();) {
            if (core_lits.contains(asms.get(i))) {
                asms.set(i, asms.last());
                asms.pop();
            }
            else {
                ++i;
            }
        }
    }
    
    /**
     * Applies smart mutation, i.e. attempts to obtain a feasible solution from a given unfeasible one.
     * @param sol The unfeasible solution.
     * @param viol_vms A set of indexes of virtual machines believed to be responsible for constraint
     * violations.
     * @return A new feasible solution, or the same solution if smart mutation fails.
     */
    // TODO: use domain knowledge in assumption removal process?
    private Solution attemptFixSolution(Solution sol, Set<Integer> viol_vms) {
        Solution sol_cpy = sol.copy();
        IVecInt asms = buildAssumptions(sol, viol_vms);
        long base_conflicts = this.solver.getConflicts();
        do {
            if (getRemainingTime() <= 0) { return sol_cpy; }
            if (this.max_conflicts != NO_TIMEOUT) {
                this.solver.setMaxConflicts(this.max_conflicts - (this.solver.getConflicts() - base_conflicts));
            }
            checkSAT(this.solver, asms);
            if (!this.solver.isSolved()) {
                if (getRemainingTime() <= 0) {
                    System.out.println("c Timeout triggered during smart mutation");
                }
            }
            else if (this.solver.isSatisfiable()) {
                try {
                    blockSolution(this.solver, this.job_vars);
                }
                catch (ContradictionException ce) {
                    System.out.println("c Contradiction blocking solution after smart mutation");
                    printOptimum();
                    System.exit(0);
                }
                MappingVec mappings = modelToAllocation(this.solver,
                                                        this.instance.getPhysicalMachines(),
                                                        this.instance.getJobs(),
                                                        this.job_vars);
                for (int i = 0; i < mappings.size(); ++i) {
                    Mapping mapping = mappings.get(i);
                    this.instance.setVirtualMachineMapping(sol_cpy,
                                                           mapping.getVirtualMachine(),
                                                           mapping.getPhysicalMachine());
                }
            }
            else if (this.solver.isUnsatisfiable()) {
                removeCoreLits(this.solver, asms);
            }
        }
        while (this.solver.isSolved() && this.solver.isUnsatisfiable());
        printElapsedTime();
        return sol_cpy;
    }
    
    /**
     * Randomly selects a subset of virtual machines to be displaced.
     * @param sol A solution.
     * @return The set of indexes of the virtual machines to be displaced.
     */
    private Set<Integer> selectRelaxVirtualMachines(Solution sol) {
        Set<Integer> relax_pms = new HashSet<Integer>();
        for (int i = 0; i < this.instance.getPhysicalMachines().size(); ++i) {
            if (PRNG.nextDouble() < this.improve_relax_rate) {
                relax_pms.add(i);
            }
        }
        Set<Integer> relax_vms = new HashSet<Integer>();
        int[] x = this.instance.getVirtualMachineAssignment(sol);
        for (int i = 0; i < sol.getNumberOfVariables(); ++i) {
            if (relax_pms.contains(x[i])) {
                relax_vms.add(i);
            }
        }
        return relax_vms;
    }
    
    /**
     * Builds the vector of partitions of undefined (not known if satisfied or unsatisfied) soft literals for
     * the objective functions, used by the Pareto-MCS algorithm.
     * @param asms The vector of assumptions. Used to filter unnecessary undefined soft literals.
     * @return The vector of partitions of undefined soft literals.
     */
    private IVec<IVecInt> buildUndefFormulas(IVecInt asms) {
        Set<Integer> asm_set = new HashSet<Integer>();
        for (int i = 0; i < asms.size(); ++i) {
            asm_set.add(asms.get(i));
        }
        IVec<IVec<ObjectiveFunction>> relax_objs = new Vec<IVec<ObjectiveFunction>>();
        for (int i = 0; i < this.obj_functions.size(); ++i) {
            IVec<ObjectiveFunction> obj = this.obj_functions.get(i);
            IVec<ObjectiveFunction> relax_obj = new Vec<ObjectiveFunction>();
            for (int j = 0; j < obj.size(); ++j) {
                IVecInt sub_obj_lits = obj.get(j).getLits();
                IVec<BigDecimal> sub_obj_coeffs = obj.get(j).getCoeffs();
                IVecInt relax_sub_obj_lits = new VecInt();
                IVec<BigDecimal> relax_sub_obj_coeffs = new Vec<BigDecimal>();
                for (int k = 0; k < sub_obj_lits.size(); ++k) {
                    int l = sub_obj_lits.get(k);
                    if (!asm_set.contains(l) && !asm_set.contains(-l)) {
                        relax_sub_obj_lits.push(l);
                        relax_sub_obj_coeffs.push(sub_obj_coeffs.get(k));
                    }
                }
                relax_obj.push(new ObjectiveFunction(relax_sub_obj_lits, relax_sub_obj_coeffs));
            }
            relax_objs.push(relax_obj);
        }
        return buildUndefFormulas(relax_objs);
    }
    
    // TODO: docs
    /**
     * Applies smart improvement, i.e. given a feasible solution, attempts to find improved versions of
     * that solution.
     * @param sol A feasible solution.
     * @param relax_vms The set of indexes of virtual machines to be displaced before applying smart
     * improvement.
     * @return An array of improved versions of {@code sol}, or {@code sol} if smart improvement fails.
     */
    private Solution[] attemptImproveSolution(Solution sol, Set<Integer> relax_vms) {
        getSolutions().clear();
        IVecInt asms = buildAssumptions(sol, relax_vms);
        if (this.improve_max_conflicts != NO_TIMEOUT) { super.setMaxConflicts(this.improve_max_conflicts); }
        IOUtils.stdoutDisable();
        IVec<IVecInt> undef_fmls = buildUndefFormulas(asms);
        IVec<ConstraintID> to_remove_ids = new Vec<ConstraintID>();
        IVecInt mss = new VecInt();
        boolean mcs_exists = false;
        do {
            if (getRemainingTime() <= 0) {
                IOUtils.stdoutEnable();
                return new Solution[] { sol.copy() };
            }
            mcs_exists = runCLD(this.solver, asms, undef_fmls, mss, to_remove_ids);
            this.solver.removeConstraints(to_remove_ids);
            to_remove_ids.clear();
            if (!mcs_exists && this.solver.isSolved() && this.solver.isUnsatisfiable()) {
                removeCoreLits(this.solver, asms);
            }
        }
        while (!mcs_exists && this.solver.isSolved() && this.solver.isUnsatisfiable());
        IOUtils.stdoutEnable();
        printElapsedTime();
        if (mcs_exists) {
            assert(getSolutions().size() > 0);
            IVecInt mcs = new VecInt();
            for (int i = 0; i < undef_fmls.size(); ++i) {
                undef_fmls.get(i).copyTo(mcs);
            }
            try {
                this.solver.addClause(mcs);
            }
            catch (ContradictionException ce) {
                System.out.println("c Contradiction blocking MCS after smart improvement");
                printOptimum();
                System.exit(0);     // TODO: .pop file won't be created; must implement shutdown handler
            }
            NondominatedPopulation imp_sols = getSolutions();
            Solution[] sols = new Solution[imp_sols.size()];
            for (int i = 0; i < sols.length; ++i) {
                sols[i] = imp_sols.get(sols.length-i-1);    // last solution goes first in case there's another
            }                                               // operator being applied next
            return sols;
        }
        return new Solution[] { sol.copy() };
    }
    
    /**
     * Applies smart mutation/improvement to a given individual.
     * @param parents An array with the individual to apply smart mutation/improvement to.
     * @return An array of offspring solutions.
     */
    public Solution[] evolve(Solution[] parents) {
        if (getRemainingTime() <= 0 || PRNG.nextDouble() > this.probability) { return parents; }
        Solution[] new_sols = new Solution[] { parents[0] };
        Set<Integer> viol_vms = getViolatingVirtualMachineIndexes(new_sols[0]);
        if (!viol_vms.isEmpty()) {
            if (!this.domain_unfix) {
                viol_vms = new HashSet<Integer>();
            }
            new_sols[0] = attemptFixSolution(new_sols[0], viol_vms);
        }
        else if (this.smart_improve) {
            Set<Integer> relax_vms = selectRelaxVirtualMachines(new_sols[0]);
            new_sols = attemptImproveSolution(new_sols[0], relax_vms);
        }
        return new_sols;
    }

    @Override
    public int getArity() { return 1; }

    @Override
    public void allocate() { /* intentionally left empty */ }

}
