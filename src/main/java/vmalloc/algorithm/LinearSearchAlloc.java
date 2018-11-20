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

import org.sat4j.specs.ContradictionException;

import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.constraints.PseudoBooleanSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Weighted Boolean
 * Optimization problem and solving it using linear search. This is a single-objective algorithm that
 * minimizes only the number of physical machines that are ON.
 * @author Miguel Terra-Neves
 */
public class LinearSearchAlloc extends ConstraintBasedAllocAlgorithm {
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that uses linear search.
     * @param instance The problem instance.
     */
    public LinearSearchAlloc(VMCwMProblem instance) { super(instance); }
    
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
    
    @Override
    public void allocate() {
        System.out.println("c WARNING: LS minimizes number of servers, not energy consumption");
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
        while (true) {
            System.out.println("c Computing mapping");
            checkSAT(solver);
            if (!solver.isSolved()) {
                printTimeoutMessage();
                return;
            }
            else if (solver.isSatisfiable()) {
                ub = getUsedPMsCount(solver, this.instance.getPhysicalMachines(), this.job_vars);
                printUsedPMsCount(ub);
                saveSolution(modelToAllocation(solver,
                                               this.instance.getPhysicalMachines(),
                                               this.instance.getJobs(),
                                               this.job_vars));
                try {
                    solver.addAtMost(this.pm_vars, ub-1);
                }
                catch (ContradictionException e) {
                    printOptimum();
                    return;
                }
            }
            else {
                printOptimum();
                return;
            }
        }
    }

}
