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

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.constraints.ConstraintID;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.constraints.PseudoBooleanSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.utils.CollectionUtils;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Weighted Boolean
 * Optimization problem and enumerating its minimal correction subsets. It can also be configured to exploit
 * Boolean model sampling techniques in order to diversify the set of minimal correction subsets enumerated
 * by the algorithm. This is a single-objective algorithm that minimizes only the number of physical machines
 * that are ON. Uses the CLD algorithm for MCS enumeration. Refer to:
 * Marques-Silva, Joao, et al. "On computing minimal correction subsets." Twenty-Third International Joint
 * Conference on Artificial Intelligence. 2013.
 * @author Miguel Terra-Neves
 */
// FIXME: same as ParetoCLD but with a different objective; merge both into one class
public class MCSAlloc extends HashEnumAlloc {
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on enumerating
     * minimal correction subsets.
     * @param instance The problem instance.
     */
    public MCSAlloc(VMCwMProblem instance) { super(instance); }
    
    /**
     * Makes the pseudo-Boolean solver and initializes it with the base formula.
     * @return The pseudo-Boolean solver.
     * @throws ContradictionException If the constraint solver detects that the base formula is
     * unsatisfiable.
     */
    protected ConstraintSolver buildSolver() throws ContradictionException {
        ConstraintSolver solver = new PseudoBooleanSolver();
        addVMCConstraints(solver);
        return solver;
    }
    
    /**
     * Builds the vector of undefined (not known if satisfied or unsatisfied) soft literals for the objective
     * function.
     * @return The vector of undefined soft literals.
     */
    private IVecInt buildUndefFormulas(IVecInt pm_vars) {
        IVecInt undef_fmls = new VecInt();
        for (int i = 0; i < pm_vars.size(); ++i) {
            undef_fmls.push(-pm_vars.get(i));
        }
        return undef_fmls;
    }
    
    // FIXME: it'll run forever if no time limit is given when hash functions are enabled
    @Override
    public void allocate() {
        System.out.println("c WARNING: MCS minimizes number of servers, not energy consumption");
        System.out.println("c Initializing");
        ConstraintSolver solver = null;
        try {
            solver = buildSolver();
        }
        catch (ContradictionException e) {
            printUnsatisfiable();
            return;
        }
        int ub = this.instance.getPhysicalMachines().size()+1;
        IVecInt asms = new VecInt();
        IVecInt undef_fmls = buildUndefFormulas(this.pm_vars);
        boolean mcs_exists = false, next_mcs = false;
        IVec<ConstraintID> to_remove = new Vec<ConstraintID>();
        IVec<ConstraintID> hash_ids = new Vec<ConstraintID>();
        IVecInt hash_asms = new VecInt();
        if (hashFunctionsEnabled()) {
            System.out.println("c Setting initial hash function");
            hash_ids = setHashFunction(solver,
                                       flattenLitVectors(flattenJobVars(this.job_vars)),
                                       hash_asms);
            assert(hash_asms.size() > 0);
        }
        while (true) {
            System.out.println("c Computing mapping");
            checkSAT(solver, hash_asms);
            if (!solver.isSolved()) {
                printTimeoutMessage();
                return;
            }
            else if (solver.isSatisfiable()) {
                mcs_exists = true;
                int new_ub = getUsedPMsCount(solver, this.instance.getPhysicalMachines(), this.job_vars);
                if (new_ub < ub) {
                    ub = new_ub;
                    printUsedPMsCount(ub);
                    saveSolution(modelToAllocation(solver,
                                                   this.instance.getPhysicalMachines(),
                                                   this.instance.getJobs(),
                                                   this.job_vars));
                }
                IVecInt new_asms = extractSatisfied(solver, undef_fmls);
                try {
                    addVecToVec(solver.addRemovableConjunction(new_asms), to_remove);
                    to_remove.push(solver.addRemovableClause(undef_fmls));
                    for (int i = 0; i < new_asms.size(); ++i) {
                        asms.push(new_asms.get(i));
                    }
                }
                catch (ContradictionException e) {
                    next_mcs = true;
                }
            }
            else {
                if (!mcs_exists) {
                    printOptimum();
                    return;
                }
                if (    hash_asms.size() > 0 &&
                        CollectionUtils.valuesIntersect(hash_asms, solver.unsatExplanation())) {
                    System.out.println("c Removing hash function");
                    IVecInt hash_neg_clause = new VecInt();
                    for (int i = 0; i < hash_asms.size(); ++i) {
                        hash_neg_clause.push(-hash_asms.get(i));
                    }
                    hash_asms.clear();
                    try {
                        hash_ids.push(solver.addRemovableClause(hash_neg_clause));
                    }
                    catch (ContradictionException e) {
                        next_mcs = true;
                    }
                }
                else {
                    hash_asms.clear();
                    next_mcs = true;
                }
            }
            if (mcs_exists && next_mcs) {
                System.out.println("c MCS computed, generating another one");
                mcs_exists = next_mcs = false;
                solver.removeConstraints(to_remove);
                to_remove.clear();
                asms.clear();
                try {
                    solver.addClause(undef_fmls); // block MCS
                    undef_fmls = buildUndefFormulas(this.pm_vars);
                }
                catch (ContradictionException e) {
                    printOptimum();
                    return;
                }
                if (hashFunctionsEnabled()) {
                    System.out.println("c Generating new hash function");
                    solver.removeConstraints(hash_ids);
                    hash_ids = setHashFunction(solver,
                                               flattenLitVectors(flattenJobVars(this.job_vars)),
                                               hash_asms);
                }
            }
        }
    }

}
