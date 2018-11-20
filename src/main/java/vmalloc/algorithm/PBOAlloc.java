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

import java.math.BigInteger;

import org.sat4j.specs.ContradictionException;

import vmalloc.algorithm.constraints.INewBestHandler;
import vmalloc.algorithm.constraints.PBOSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Pseudo-Boolean
 * Optimization problem. This is a single-objective algorithm that minimizes only the number of physical
 * machines that are ON.
 * @author Miguel Terra-Neves
 */
public class PBOAlloc extends ConstraintBasedAllocAlgorithm {
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on Pseudo-Boolean
     * Optimization.
     * @param instance The problem instance.
     */
    public PBOAlloc(VMCwMProblem instance) { super(instance); }
    
    /**
     * Makes the Pseudo-Boolean Optimization solver and initializes it with the respective formula for
     * the single-objective Virtual Machine Consolidation problem.
     * @return The Pseudo-Boolean Optimization solver.
     * @throws ContradictionException If the constraint solver detects that the base formula is
     * unsatisfiable.
     */
    protected PBOSolver buildSolver() throws ContradictionException {
        PBOSolver solver = new PBOSolver();
        addVMCConstraints(solver);
        System.out.println("c Setting objective function");
        solver.setObjectiveFunction(this.pm_vars);
        System.out.println("c Done");
        printElapsedTime();
        return solver;
    }
    
    @Override
    public void allocate() {
        System.out.println("c WARNING: PBO minimizes number of physical machines, not energy consumption");
        PBOSolver solver;
        try {
            solver = buildSolver();
        }
        catch (ContradictionException e) {
            printUnsatisfiable();
            return;
        }
        solver.setTimeout(getRemainingTime());
        solver.solve(new INewBestHandler() {
            public void handleNewBest(BigInteger best) {
                printUsedPMsCount(best.intValue());
            }
        });
        if (solver.isSolved()) {
            if (solver.isSatisfiable()) {
                saveSolution(modelToAllocation(solver,
                                               this.instance.getPhysicalMachines(),
                                               this.instance.getJobs(),
                                               job_vars));
            }
            if (solver.foundOptimum()) {
                printOptimum();
            }
        }
    }

}
