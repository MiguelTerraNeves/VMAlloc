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

import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.operator.RandomInitialization;

/**
 * Evolutionary algorithms start with an initial random population of solutions, which is refined throughout
 * generations. In constrained problems, some of those solutions may be infeasible. This abstract superclass
 * represents initialization operators that attempt to initialize populations with feasible solutions.
 * Evolutionary algorithms have a hard time handling constrained problems. Initializing populations with
 * feasible solutions can make their job easier.
 * @author Miguel Terra-Neves
 */
public abstract class FeasibleInitialization extends RandomInitialization {

    /**
     * Creates an instance of the feasible initialization operator.
     * @param problem The problem instance.
     * @param populationSize The size of the population.
     */
    public FeasibleInitialization(Problem problem, int populationSize) {
        super(problem, populationSize);
    }
    
    /**
     * Makes a random set of solutions for the initial population. It attempts to produce a set of feasible
     * solutions.
     * @return An array with the initial solutions.
     */
    @Override
    public Solution[] initialize() {
        System.out.println("c Attempting feasible initialization");
        Solution[] initial_pop = new Solution[populationSize];
        for (int i = 0; i < populationSize; ++i) {
            initial_pop[i] = problem.newSolution();
            attemptFeasibleInitialization(initial_pop[i]);
        }
        return initial_pop;
    }
    
    /**
     * Attempts to initialize a solution as a feasible solution.
     * @param solution The solution to initialize.
     */
    protected abstract void attemptFeasibleInitialization(Solution solution);
    
}
