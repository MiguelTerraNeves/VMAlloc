package vmalloc.algorithm.constraints;

import java.math.BigInteger;

/**
 * Interface of observer objects that listen for new solutions in optimization problems.
 * When solving optimization problems, constraint solvers may be able to produce intermediate
 * sub-optimal solutions. Instances of this class provide a callback method that is invoked whenever
 * a constraint solver finds a solution better than the previous one.
 * @author Miguel Terra-Neves
 */
public interface INewBestHandler {
    
    /**
     * Callback method invoked by the optimization solver when a better solution is found.
     * @param best The cost of the new solution.
     */
    void handleNewBest(BigInteger best);
    
}
