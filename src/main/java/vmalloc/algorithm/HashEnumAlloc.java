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

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.constraints.ConstraintID;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.constraints.PseudoBooleanSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.JobVec;
import vmalloc.domain.PhysicalMachineVec;

/**
 * Virtual Machine Consolidation algorithm that relies on the application of Boolean model enumeration and
 * sampling techniques to pseudo-Boolean formulations.
 * @author Miguel Terra-Neves
 */
public class HashEnumAlloc extends ConstraintBasedAllocAlgorithm {
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on Boolean model
     * enumeration and sampling techniques.
     * @param instance The problem instance.
     */
    public HashEnumAlloc(VMCwMProblem instance) { super(instance); }
    
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
     * Adds an hash function to a constraint solver.
     * @param solver The constraint solver.
     * @return A vector of constraint IDs that must be removed in order to fully remove the hash function
     * from the solver.
     */
    // TODO: update docs
    protected IVec<ConstraintID> setHashFunction(ConstraintSolver solver, IVecInt asms) {
        assert(hashFunctionsEnabled());
        System.out.println("c Setting hash function");
        IVecInt vars = flattenLitVectors(flattenJobVars(this.job_vars));
        IVec<ConstraintID> ids = setHashFunction(solver, vars, asms);
        System.out.println("c Hash function set");
        printElapsedTime();
        return ids;
    }
    
    @Override
    public void allocate() {
        System.out.println("c Initializing");
        ConstraintSolver solver = null;
        try {
            solver = buildSolver();
        }
        catch (ContradictionException e) {
            printUnsatisfiable();
            return;
        }
        int enum_threshold = getEnumerationThreshold();
        IVecInt asms = new VecInt();
        IVec<ConstraintID> to_remove = setHashFunction(solver, asms);
        int nsols_in_cell = 0;
        while (true) {
            checkSAT(solver, asms);
            if (!solver.isSolved()) {
                printTimeoutMessage();
                return;
            }
            else if (solver.isSatisfiable()) {
                ++nsols_in_cell;
                saveSolution(modelToAllocation(solver,
                                               this.instance.getPhysicalMachines(),
                                               this.instance.getJobs(),
                                               this.job_vars),
                             true);
                try {
                    blockSolution(solver, this.job_vars);
                }
                catch (ContradictionException ce) {
                    nsols_in_cell = enum_threshold; // force new hash function
                }
            }
            if (!solver.isSatisfiable() && hashFunctionsEnabled()) {
                return;
            }
            if (    !solver.isSatisfiable() ||
                    (hashFunctionsEnabled() && nsols_in_cell >= enum_threshold)) {
                assert(to_remove.size() > 0);
                solver.removeConstraints(to_remove);
                asms.clear();
                to_remove = setHashFunction(solver, asms);
                nsols_in_cell = 0;
            }
        }
    }

}
