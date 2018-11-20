package vmalloc.algorithm.constraints;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.utils.MathUtils;

/**
 * Abstract superclass for objects that aggregate constraints.
 * @author Miguel Terra-Neves
 */
public abstract class ConstraintAggregator {

    /**
     * Creates a new Boolean variable.
     * @see #nVars()
     */
    public abstract void newVar();
    
    /**
     * Creates multiple new Boolean variables.
     * @param nvars The number of variables to create.
     * @see #nVars()
     */
    public abstract void newVars(int nvars);
    
    /**
     * Retrieves the number of variables created. Constraints added to the aggregator
     * cannot have variables with indexes larger than the value returned by this method.
     * @return The number of variables created in the aggregator.
     */
    public abstract int nVars();
    
    /**
     * Adds a constraint stating that the sum of the literals in {@code lits} must be equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addExactly(IVecInt lits, int rhs) throws ContradictionException;
    
    /**
     * Adds a constraint stating that the sum of the literals in {@code lits} must be less or equal
     * to {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addAtMost(IVecInt lits, int rhs) throws ContradictionException;
    
    /**
     * Adds a constraint stating that the sum of the literals in {@code lits} must be equal or
     * larger than {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addAtLeast(IVecInt lits, int rhs) throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be equal to {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be equal to {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be greater or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addGreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be greater or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addGreaterOrEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be less or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addLessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be less or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addLessOrEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be greater than
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addGreater(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException;
    
    /**
     * Adds a Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be less than
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @throws ContradictionException If the aggregator detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract void addLess(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException;
    
    /**
     * Adds a constraint stating that at least one of the literals in {@code lits} must be
     * satisfied.
     * @param lits The clause's literals.
     * @throws ContradictionException If the aggregator detects that the addition of the clause would
     * result in a contradiction.
     */
    public abstract void addClause(IVecInt lits) throws ContradictionException;
    
    /**
     * Adds a constraint stating that all of the literals in {@code lits} must be satisfied.
     * @param lits The conjunction's literals.
     * @throws ContradictionException If the aggregator detects that the addition of the clause would
     * result in a contradiction.
     */
    public void addConjunction(IVecInt lits) throws ContradictionException {
        for (int i = 0; i < lits.size(); ++i) {
            addClause(new VecInt(new int[] { lits.get(i) }));
        }
    }
    
    /**
     * Auxiliary class used to store the result of applying a scaling factor to a set of
     * non-integral coefficients and rhs of a Pseudo-Boolean constraint.
     * @see ConstraintAggregator#scaleToInteger(IVec, BigDecimal)
     */
    protected class ScaledResult {
        
        /**
         * The coefficients after applying the scaling factor.
         */
        private IVec<BigInteger> coeffs;
        
        /**
         * The right-hand side after applying the scaling factor.
         */
        private BigInteger rhs;
        
        /**
         * Creates an instance of a {@code ScaledResult} object to hold the results of a scaling
         * operation.
         * @param coeffs The scaled coefficients.
         * @param rhs The scaled right-hand side.
         */
        ScaledResult(IVec<BigInteger> coeffs, BigInteger rhs) {
            this.coeffs = coeffs;
            this.rhs = rhs;
        }
        
        /**
         * Retrieves the scaled coefficients.
         * @return The scaled coefficients.
         */
        IVec<BigInteger> getCoefficients() { return this.coeffs; }
        
        /**
         * Retrieves the scaled right-hand side.
         * @return The scaled right-hand side.
         */
        BigInteger getRightHandSide() { return this.rhs; }
        
    }
    
    /**
     * Given a set of possible non-integral coefficients and the right-hand side of a
     * Pseudo-Boolean constraint, converts those coefficients and right-hand side to integers in a
     * way that preserves the constraint's model set.
     * @param coeffs The coefficients to scale.
     * @param rhs The right-hand side to scale.
     * @return A {@link ScaledResult} object with the scaled integer coefficients and right-hand
     * side.
     */
    protected ScaledResult scaleToInteger(IVec<BigDecimal> coeffs, BigDecimal rhs) {
        coeffs.push(rhs);
        IVec<BigInteger> scaled_coeffs = MathUtils.scaleToInteger(coeffs);
        BigInteger scaled_rhs = scaled_coeffs.last();
        coeffs.pop();
        scaled_coeffs.pop();
        return new ScaledResult(scaled_coeffs, scaled_rhs);
    }
    
}
