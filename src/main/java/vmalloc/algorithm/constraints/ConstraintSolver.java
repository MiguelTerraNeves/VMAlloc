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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.Clock;

/**
 * Abstract superclass for constraint solvers.
 * @author Miguel Terra-Neves
 */
public abstract class ConstraintSolver extends ConstraintAggregator {
    
    /**
     * Integer constant used to represent undefined timeouts.
     */
    protected static final int NO_TIMEOUT = -1;
    
    /**
     * Time instant from which calls to {@link #solve()} or {@link #solve(IVecInt)} must terminate.
     * {@code timeout} is specified in seconds starting from the moment at which the application was
     * launched. If {@code timeout} is set to {@link #NO_TIMEOUT}, then no time limit is imposed.
     */
    private long timeout = NO_TIMEOUT;
    
    /**
     * Number of conflicts from which calls to {@link #solve()} or {@link #solve(IVecInt)} must terminate.
     * If {@code max_conflicts} is set to {@link #NO_TIMEOUT}, then no conflict limit is imposed.
     */
    private long max_conflicts = NO_TIMEOUT;
    
    /**
     * Adds a removable constraint stating that the sum of the literals in {@code lits} must be
     * equal to {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param rhs The value in right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract ConstraintID addRemovableExactly(IVecInt lits, int rhs)
            throws ContradictionException;
    
    /**
     * Adds a removable constraint stating that the sum of the literals in {@code lits} must be less
     * or equal to {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param rhs The value in right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract ConstraintID addRemovableAtMost(IVecInt lits, int rhs)
            throws ContradictionException;
    
    /**
     * Adds a removable constraint stating that the sum of the literals in {@code lits} must be
     * equal or larger than {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param rhs The value in right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract ConstraintID addRemovableAtLeast(IVecInt lits, int rhs)
            throws ContradictionException;
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be equal to {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract ConstraintID addRemovableEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException;
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be greater or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract ConstraintID addRemovableGreaterOrEqual(
            IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs) throws ContradictionException;
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be less or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public abstract ConstraintID addRemovableLessOrEqual(
            IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs) throws ContradictionException;
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be equal to {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public ConstraintID addRemovableEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        return addRemovableEqual(lits, scaled.getCoefficients(), scaled.getRightHandSide());
    }
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be greater or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public ConstraintID addRemovableGreaterOrEqual(
            IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs) throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        return addRemovableGreaterOrEqual(lits, scaled.getCoefficients(), scaled.getRightHandSide());
    }
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be less or equal to
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public ConstraintID addRemovableLessOrEqual(
            IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs) throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        return addRemovableLessOrEqual(lits, scaled.getCoefficients(), scaled.getRightHandSide());
    }
    
    @Override
    public void addEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        addEqual(lits, scaled.getCoefficients(), scaled.getRightHandSide());
    }
    
    @Override
    public void addGreaterOrEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        addGreaterOrEqual(lits, scaled.getCoefficients(), scaled.getRightHandSide());
    }
    
    @Override
    public void addLessOrEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        addLessOrEqual(lits, scaled.getCoefficients(), scaled.getRightHandSide());
    }
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be greater than
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public ConstraintID addRemovableGreater(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        return addRemovableGreaterOrEqual(lits,
                                          scaled.getCoefficients(),
                                          scaled.getRightHandSide().add(BigInteger.ONE));
    }
    
    /**
     * Adds a removable Pseudo-Boolean constraint stating that the sum of a set of coefficients in
     * {@code coeffs} times the corresponding literals in {@code lits} must be less than
     * {@code rhs}.
     * @param lits The literals in the left-hand side of the constraint.
     * @param coeffs The coefficients in the left-hand side of the constraint.
     * @param rhs The value in the right-hand side of the constraint.
     * @return The constraint's id.
     * @throws ContradictionException If the solver detects that the addition of this constraint
     * would result in a contradiction.
     */
    public ConstraintID addRemovableLess(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        return addRemovableLessOrEqual(lits,
                                       scaled.getCoefficients(),
                                       scaled.getRightHandSide().subtract(BigInteger.ONE));
    }
    
    @Override
    public void addGreater(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        addGreaterOrEqual(lits, scaled.getCoefficients(), scaled.getRightHandSide().add(BigInteger.ONE));
    }
    
    @Override
    public void addLess(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        ScaledResult scaled = scaleToInteger(coeffs, rhs);
        addLessOrEqual(lits,
                       scaled.getCoefficients(),
                       scaled.getRightHandSide().subtract(BigInteger.ONE));
    }
    
    /**
     * Adds a removable constraint stating that at least one of the literals in {@code lits} must be
     * satisfied.
     * @param lits The clause's literals.
     * @return The clause's constraint id.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    public abstract ConstraintID addRemovableClause(IVecInt lits) throws ContradictionException;
    
    /**
     * Adds a removable constraint stating that all of the literals in {@code lits} must be satisfied.
     * @param lits The conjunction's literals.
     * @return A vector with a set of constraint IDs that must be removed in order to completely remove the
     * conjunction.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    // FIXME: return a single constraint ID
    public IVec<ConstraintID> addRemovableConjunction(IVecInt lits) throws ContradictionException {
        IVec<ConstraintID> ids = new Vec<ConstraintID>();
        for (int i = 0; i < lits.size(); ++i) {
            ids.push(addRemovableClause(new VecInt(new int[] { lits.get(i) })));
        }
        return ids;
    }
    
    /**
     * Helper method for adding constraints encoded into conjunctive normal form. Adds an auxiliary clause
     * to the solver and adds its constraint ID to the {@code ids} vector. If {@code ids} is {@code null},
     * then it is assumed that the constraint is not removable and no constraint ID is generated for the
     * clause.
     * @param lits The clause's literals.
     * @param ids A vector of constraint IDs in which to store the clause's constraint ID, or {@code null}
     * if the constraint is not removable.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    private void addAuxiliaryClause(IVecInt lits, IVec<ConstraintID> ids) throws ContradictionException {
        if (ids != null) {
            ids.push(addRemovableClause(lits));
        }
        else {
            addClause(lits);
        }
    }
    
    /**
     * Sub-routine used in the binary tree encoding of XOR constraints into conjunctive normal form. Encodes
     * a binary XOR constraint, where the arguments are the output literals of two of the encoding's
     * sub-trees. This results in the merging of those sub-trees into a single tree. A new output literal is
     * generated for the new tree and returned. The corresponding constraint IDs are added to the {@code ids}
     * vector. If {@code ids} is {@code null}, then it is assumed that the XOR constraint is not removable
     * and no constraint IDs are generated for the constraint.
     * @param left_out The output literal of the left sub-tree.
     * @param right_out The output literal of the right sub-tree.
     * @param ids A vector of constraint IDs in which to store the XOR constraint's IDs, or {@code null} if
     * the constraint is not removable.
     * @return A fresh output literal for the merged tree.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     * @see #xorToCNF(IVecInt, IVec)
     */
    private int mergeSubXOR(int left_out, int right_out, IVec<ConstraintID> ids) throws ContradictionException {
        newVar();
        int out = nVars();
        addAuxiliaryClause(new VecInt(new int[] { -left_out, right_out, out }), ids);
        addAuxiliaryClause(new VecInt(new int[] { left_out, -right_out, out }), ids);
        addAuxiliaryClause(new VecInt(new int[] { left_out, right_out, -out }), ids);
        addAuxiliaryClause(new VecInt(new int[] { -left_out, -right_out, -out }), ids);
        return out;
    }
    
    /**
     * Converts a XOR constraint to conjunctive normal form using a binary tree encoding. An output literal
     * is returned that must be used to set the right hand side of the XOR constraint. If an odd number of
     * literals are to be satisfied, then the output literal must be true, otherwise it must be false. The
     * corresponding constraint IDs are added to the {@code ids} vector. If {@code ids} is {@code null}, then
     * it is assumed that the XOR constraint is not removable and no constraint IDs are generated for the
     * constraint.
     * @param lits The constraint's literals.
     * @param ids A vector of constraint IDs in which to store the XOR constraint's IDs, or {@code null} if
     * the constraint is not removable.
     * @return The constraint's output literal.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     * @see #addXOR_core(IVecInt, boolean, int, IVec)
     */
    private int xorToCNF(IVecInt lits, IVec<ConstraintID> ids) throws ContradictionException {
        assert(lits.size() > 1);
        int split = lits.size() / 2;
        IVecInt left = new VecInt();
        for (int i = 0; i < split; ++i) {
            left.push(lits.get(i));
        }
        IVecInt right = new VecInt();
        for (int i = split; i < lits.size(); ++i) {
            right.push(lits.get(i));
        }
        int left_out, right_out;
        if (left.size() > 1) {
            left_out = xorToCNF(left, ids);
        }
        else {
            left_out = left.get(0);
        }
        if (right.size() > 1) {
            right_out = xorToCNF(right, ids);
        }
        else {
            right_out = right.get(0);
        }
        return mergeSubXOR(left_out, right_out, ids);
    }
    
    /**
     * Constant used to indicate that no activator literal is to be added to a constraint. Used in
     * constraints that support activation literals.
     */
    private static final int NO_ACTIVATOR = 0;
    
    /**
     * Core method that adds a XOR constraint to the solver by converting to conjunctive normal form using a
     * binary tree encoding. The user may provide an activator literal that can then be used to enable and
     * disable the constraint without the need to remove and re-add. The corresponding constraint IDs are
     * added to the {@code ids} vector. If {@code ids} is {@code null}, then it is assumed that the XOR
     * constraint is not removable and no constraint IDs are generated for the constraint.
     * @param lits The XOR constraint's literals.
     * @param rhs The right hand side of the constraint. If true, then the XOR constraint forces an odd
     * number of the literals to be satisfied, otherwise an even number of literals are to be satisfied
     * instead.
     * @param activator The activator literal, or {@link #NO_ACTIVATOR} if no activation literal is needed.
     * @param ids A vector of constraint IDs in which to store the XOR constraint's IDs, or {@code null} if
     * the constraint is not removable.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    private void addXOR_core(IVecInt lits, boolean rhs, int activator, IVec<ConstraintID> ids)
            throws ContradictionException {
        assert(ids == null || ids.isEmpty());
        IVecInt activation_clause = new VecInt();
        if (lits.size() > 1) {
            int out = xorToCNF(lits, ids);
            activation_clause.push((rhs) ? out : -out);
        }
        else if (lits.size() == 1) {
            activation_clause.push((rhs) ? lits.get(0) : -lits.get(0));
        }
        if (activator != NO_ACTIVATOR) {
            activation_clause.push(-activator);
        }
        addAuxiliaryClause(activation_clause, ids);
    }
    
    /**
     * Adds a removable XOR constraint to the solver. The user may provide an activator literal that can then
     * be used to enable and disable the constraint without the need to remove and re-add.
     * @param lits The XOR constraint's literals.
     * @param rhs The right hand side of the constraint. If true, then the XOR constraint forces an odd
     * number of the literals to be satisfied, otherwise an even number of literals are to be satisfied
     * instead.
     * @param activator The activator literal.
     * @return A vector with a set of constraint IDs that must be removed in order to completely remove the
     * XOR constraint.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    // FIXME: return a single constraint ID
    public IVec<ConstraintID> addRemovableXOR(IVecInt lits, boolean rhs, int activator) throws ContradictionException {
        IVec<ConstraintID> ids = new Vec<ConstraintID>();
        addXOR_core(lits, rhs, activator, ids);
        return ids;
    }
    
    /**
     * Adds a removable XOR constraint to the solver.
     * @param lits The XOR constraint's literals.
     * @param rhs The right hand side of the constraint. If true, then the XOR constraint forces an odd
     * number of the literals to be satisfied, otherwise an even number of literals are to be satisfied
     * instead.
     * @return A vector with a set of constraint IDs that must be removed in order to completely remove the
     * XOR constraint.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    // FIXME: return a single constraint ID
    public IVec<ConstraintID> addRemovableXOR(IVecInt lits, boolean rhs) throws ContradictionException {
        return addRemovableXOR(lits, rhs, NO_ACTIVATOR);
    }
    
    /**
     * Adds a XOR constraint to the solver. The user may provide an activator literal that can then be used
     * to enable and disable the constraint without the need to remove and re-add.
     * @param lits The XOR constraint's literals.
     * @param rhs The right hand side of the constraint. If true, then the XOR constraint forces an odd
     * number of the literals to be satisfied, otherwise an even number of literals are to be satisfied
     * instead.
     * @param activator The activator literal.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    public void addXOR(IVecInt lits, boolean rhs, int activator) throws ContradictionException {
        addXOR_core(lits, rhs, activator, null);
    }
    
    /**
     * Adds a XOR constraint to the solver.
     * @param lits The XOR constraint's literals.
     * @param rhs The right hand side of the constraint. If true, then the XOR constraint forces an odd
     * number of the literals to be satisfied, otherwise an even number of literals are to be satisfied
     * instead.
     * @throws ContradictionException If the solver detects that the addition of the clause would
     * result in a contradiction.
     */
    public void addXOR(IVecInt lits, boolean rhs) throws ContradictionException {
        addXOR(lits, rhs, NO_ACTIVATOR);
    }
    
    /**
     * Removes a constraint from the solver.
     * @param id The id of the constraint to be removed.
     */
    public abstract void removeConstraint(ConstraintID id);
    
    /**
     * Removes a set of constraints from the solver.
     * @param ids The ids of the constraints to be removed.
     */
    public void removeConstraints(IVec<ConstraintID> ids) {
        for (int i = 0; i < ids.size(); ++i) {
            removeConstraint(ids.get(i));
        }
    }
    
    /**
     * Sets the time in seconds allotted for future calls to {@link #solve()} or
     * {@link #solve(IVecInt)}.
     * @param timeout The allotted time in seconds.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout + (long)Clock.getInstance().getElapsed();
    }
    
    /**
     * Retrieves the remaining time allotted for calls to {@link #solve()} or
     * {@link #solve(IVecInt)}. Default value is {@link Long#MAX_VALUE}.
     * @return The remaining time in seconds.
     */
    protected long getRemainingTime() {
        if (timeout == NO_TIMEOUT) {
            return Long.MAX_VALUE; // FIXME: implicit limit, should change eventually
        }
        assert(timeout >= 0);
        return Math.max(timeout - (long)Clock.getInstance().getElapsed(), 0);
    }
    
    /**
     * Sets the maximum number of conflicts allowed for future calls to {@link #solve()} or
     * {@link #solve(IVecInt)}.
     * @param conflicts The number of conflicts.
     */
    public void setMaxConflicts(long conflicts) { this.max_conflicts = conflicts; }

    /**
     * Resets the maximum number of conflicts allowed for future calls to {@link #solve()} or
     * {@link #solve(IVecInt)}.
     */
    public void resetMaxConflicts() { this.max_conflicts = NO_TIMEOUT; }
    
    /**
     * Retrieves the maximum number of conflicts allowed for calls to {@link #solve()} or
     * {@link #solve(IVecInt)}.
     */
    protected long getMaxConflicts() { return this.max_conflicts; }
    
    /**
     * Solves the constraint satisfaction problem under a given set of assumptions.
     * @param asms A set of literals that must be satisfied by the solution.
     */
    public abstract void solve(IVecInt asms);
    
    /**
     * Solves the constraint satisfaction problem.
     */
    public void solve() { solve(new VecInt()); }
    
    /**
     * Checks if the solver was able to solve the constraint satisfaction problem. Must be called
     * after {@link #solve()} or {@link #solve(IVecInt)}.
     * @return True if the problem was solved, false otherwise (in this case, usually due to
     * timeout).
     */
    public abstract boolean isSolved();
    
    /**
     * Checks if the constraint set is satisfiable. Must be called after {@link #solve()} or
     * {@link #solve(IVecInt)}.
     * @return True if it is satisfiable, false otherwise.
     */
    public abstract boolean isSatisfiable();
    
    /**
     * Checks if the constraint set is unsatisfiable. Must be called after {@link #solve()} or
     * {@link #solve(IVecInt)}.
     * @return True if it is unsatisfiable, false otherwise.
     */
    public abstract boolean isUnsatisfiable();
    
    /**
     * If the constraint set is satisfiable, returns the value of a given literal in the solution
     * found by the solver. Must be called after {@link #solve()} or {@link #solve(IVecInt)}.
     * @param lit The literal.
     * @return True if {@code lit} has value 1, false otherwise.
     * @see #isSatisfiable()
     */
    public abstract boolean modelValue(int lit);
    
    /**
     * If the constraint set is unsatisfiable, returns a subset of the assumptions that are
     * responsible for unsatisfiability. Must be called after {@link #solve(IVecInt)}.
     * @return A vector with the assumptions literals responsible for unsatisfiability.
     * @see #isUnsatisfiable()
     */
    public abstract IVecInt unsatExplanation();
    
}
