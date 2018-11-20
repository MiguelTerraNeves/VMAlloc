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

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.constraints.ConstraintID;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Multi-Objective
 * Combinatorial Optimization problem and solving it using the guided improvement algorithm. Refer to:
 * Jackson, Daniel, H. Estler, and Derek Rayside. "The guided improvement algorithm for exact,
 * general-purpose, many-objective combinatorial optimization." (2009).
 * @author Miguel Terra-Neves
 */
public class GIAAlloc extends MultiObjectiveConstraintBasedAllocAlgorithm {

    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that uses the guided improvement
     * algorithm.
     * @param instance The problem instance.
     */
    public GIAAlloc(VMCwMProblem instance) { super(instance); }

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
        System.out.println("c Initializing objective functions");
        initializeObjectiveFunctions();
        printElapsedTime();
        int enum_threshold = getEnumerationThreshold();
        IVec<ConstraintID> to_remove = new Vec<ConstraintID>();
        IVec<ConstraintID> hash_ids = new Vec<ConstraintID>();
        if (hashFunctionsEnabled()) {
            hash_ids = setHashFunction(solver, flattenLitVectors(flattenJobVars(this.job_vars)));
        }
        int nsols_in_cell = 0;
        System.out.println("c Searching for a Pareto optimal solution");
        while (true) {
            checkSAT(solver);
            if (!solver.isSolved()) {
                printTimeoutMessage();
                return;
            }
            else if (solver.isSatisfiable()) {
                try {
                    ++nsols_in_cell;
                    saveSolution(modelToAllocation(solver,
                                                   this.instance.getPhysicalMachines(),
                                                   this.instance.getJobs(),
                                                   this.job_vars),
                                 true);
                    IVec<BigDecimal> obj_values = new Vec<BigDecimal>();
                    for (int i = 0; i < this.obj_functions.size(); ++i) {
                        for (int j = 0; j < this.obj_functions.get(i).size(); ++j) {
                            ObjectiveFunction function = this.obj_functions.get(i).get(j);
                            obj_values.push(computeObjectiveValue(solver, function));
                            to_remove.push(solver.addRemovableLessOrEqual(function.getLits(),
                                                                          function.getCoeffs(),
                                                                          obj_values.last()));
                        }
                    }
                    IVecInt or_lits = new VecInt();
                    for (int i = 0; i < this.obj_functions.size(); ++i) {
                        for (int j = 0; j < this.obj_functions.get(i).size(); ++j) {
                            ObjectiveFunction function = this.obj_functions.get(i).get(j);
                            int new_var = newVar(solver);
                            function.getLits().push(new_var);
                            function.getCoeffs().push(function.getCoDomainSize().negate());
                            solver.addLess(function.getLits(), function.getCoeffs(), obj_values.get(i));
                            function.getLits().pop();
                            function.getCoeffs().pop();
                            or_lits.push(-new_var);
                        }
                    }
                    solver.addClause(or_lits);
                }
                catch (ContradictionException e) {
                    solver.removeConstraints(to_remove);
                    to_remove.clear();
                    printElapsedTime();
                    System.out.println("c Searching for another Pareto optimal solution");
                }
            }
            else if (to_remove.size() > 0) {
                solver.removeConstraints(to_remove);
                to_remove.clear();
                printElapsedTime();
                System.out.println("c Searching for another Pareto optimal solution");
            }
            else if (!hashFunctionsEnabled()) {
                System.out.println("c Done");
                printElapsedTime();
                return;
            }
            else {
                nsols_in_cell = enum_threshold; // force new hash function
            }
            if (hashFunctionsEnabled() && nsols_in_cell >= enum_threshold) {
                assert(hash_ids.size() > 0);
                solver.removeConstraints(hash_ids);
                to_remove = setHashFunction(solver, flattenLitVectors(flattenJobVars(this.job_vars)));
                nsols_in_cell = 0;
            }
        }
    }

}
