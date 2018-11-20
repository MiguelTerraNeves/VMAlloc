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

package vmalloc.algorithm.constraints;

import java.math.BigInteger;

import org.sat4j.core.Vec;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import vmalloc.exception.NotSupportedException;

/**
 * Constrained optimization solver wrapper around SAT4J for solving Pseudo-Boolean Optimization
 * (PBO) problem instances.
 * @author Miguel Terra-Neves
 */
// TODO: unsupported max conflicts
public class PBOSolver extends PseudoBooleanSolver {
    
    /**
     * {@link BigInteger} constant used to represent the case where a solution has not been found
     * yet.
     */
    private static final BigInteger NO_BEST = BigInteger.ONE.negate();
    
    /**
     * An instance of the underlying SAT4J optimizer.
     */
    private PseudoOptDecorator opt = null;
    
    /**
     * Boolean used to store if the solver found an optimal solution on the last call to
     * {@link #solve()} or {@link #solve(INewBestHandler)}. True if so, false otherwise. 
     */
    private boolean found_optimum = false;
    
    /**
     * Stores the cost of the best solution found on the last call to {@link #solve()} or
     * {@link #solve(INewBestHandler)}. If no solution was found, the default value is
     * {@link #NO_BEST}.
     */
    private BigInteger best = NO_BEST;
    
    /**
     * Sets the objective function. The objective function is the minimization of the sum of a set
     * of variables times the corresponding coefficients.
     * @param vars The variables in the objective function.
     * @param coeffs The variable coefficients.
     */
    public void setObjectiveFunction(IVecInt vars, IVec<BigInteger> coeffs) {
        solver.setObjectiveFunction(new ObjectiveFunction(vars, coeffs));
    }
    
    /**
     * Sets the objective function. This method sets all variable coefficients to one.
     * @param vars The variables in the objective function.
     * @see #setObjectiveFunction(IVecInt, IVec)
     */
    // FIXME: shouldn't it be a vector of lits?
    public void setObjectiveFunction(IVecInt vars) {
        setObjectiveFunction(vars, new Vec<BigInteger>(vars.size(), BigInteger.ONE));
    }
    
    /**
     * Solves the Pseudo-Boolean optimization instance.
     */
    @Override
    public void solve() {
        solve(new INewBestHandler() { // dummy new best handler
            public void handleNewBest(BigInteger best) {}
        });
    }
    
    /**
     * Solves the Pseudo-Boolean optimization instance. {@link PBOSolver} does not support
     * assumptions. Calling this method with assumptions will result in a
     * {@link NotSupportedException}. Therefore, {@link #solve()} or {@link #solve(INewBestHandler)}
     * should be used instead.
     * @param asms A set of literals that must be satisfied by the solution.
     */
    @Deprecated
    @Override
    public void solve(IVecInt asms) {
        if (asms.size() > 0) {
            throw new NotSupportedException("PBOSolver does not support assumptions.");
        }
        solve();
    }
    
    /**
     * Solves the Pseudo-Boolean optimization instance, calling a given handler whenever a better
     * solution is found.
     * @param handler The callback object to be invoked whenever a better solution is found.
     */
    public void solve(INewBestHandler handler) {
        opt = new PseudoOptDecorator(solver);
        opt.setTimeout((int)getRemainingTime());
        is_solved = false;
        try {
            while (opt.admitABetterSolution()) {
                is_solved = is_sat = true;
                BigInteger new_best = new BigInteger(opt.getObjectiveValue().toString());
                if (best.equals(NO_BEST) || new_best.compareTo(best) < 0) {
                    best = new BigInteger(opt.getObjectiveValue().toString());
                    handler.handleNewBest(best);
                }
                //opt.forceObjectiveValueTo(best.subtract(BigInteger.ONE));
                opt.discardCurrentSolution();
                opt.setTimeout((int)getRemainingTime());
            }
            found_optimum = true;
        }
        catch (ContradictionException e) {
            found_optimum = true;
        }
        catch (TimeoutException e) {
            /* intentionally left empty */
        }
    }
    
    /**
     * Checks if an optimal solution was found. Must be called after {@link #solve()} or
     * {@link #solve(INewBestHandler)}.
     * @return True if the optimal solution was found, false otherwise.
     */
    public boolean foundOptimum() { return this.found_optimum; }
    
    /**
     * If a solution was found on the last call to {@link #solve()} or
     * {@link #solve(INewBestHandler)}, retrieves the cost of the best solution found by the solver.
     * @return The cost of the best solution.
     * @see ConstraintSolver#isSatisfiable()
     */
    public BigInteger getBest() { return this.best; }
    
    /**
     * Retrieves the value of a given literal in the best solution found by the solver on the
     * last call to {@link #solve()} or {@link #solve(INewBestHandler)}.
     * @param lit The literal.
     * @return True if {@code lit} has value 1, false otherwise.
     * @see ConstraintSolver#isSatisfiable()
     */
    @Override
    public boolean modelValue(int lit) {
        assert(lit != 0);
        return lit > 0 ? this.opt.model(lit) : !this.opt.model(-lit);
    }
    
}
