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
