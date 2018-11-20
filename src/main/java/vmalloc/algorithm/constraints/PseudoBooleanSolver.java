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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IConstr;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

/**
 * Constraint solver wrapper around SAT4J for solving instances of the Pseudo-Boolean Satisfaction
 * (PBS) problem. Works incrementally.
 * @author Miguel Terra-Neves
 */
public class PseudoBooleanSolver extends ConstraintSolver {
    
    /**
     * Abstract superclass for constraint objects. This class and respective subclasses are
     * auxiliary and are required because SAT4J does not add constraints satisfied by unit
     * propagation (this is not documented). If constraints are removed using
     * {@link PseudoBooleanSolver#removeConstraint(ConstraintID)} or
     * {@link PseudoBooleanSolver#removeConstraints(IVec)}, then we need to add those that were
     * satisfied by unit propagation, or else the solver might produce incorrect results in
     * future calls to {@link PseudoBooleanSolver#solve()} or
     * {@link PseudoBooleanSolver#solve(IVecInt)}.
     */
    private abstract class Constraint {
        
        /**
         * Literals in the constraint.
         */
        private IVecInt lits = null;
        
        /**
         * The constraint's id if it is a removable constraint. Null, otherwise.
         */
        private ConstraintID id = null;
        
        /**
         * Creates an instance of a constraint.
         * @param lits The literals in the constraint.
         */
        Constraint(IVecInt lits) {
            this.lits = new VecInt(lits.size());
            lits.copyTo(this.lits);
        }
        
        /**
         * Creates an instance of a constraint.
         * @param lits The literals in the constraint.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        Constraint(IVecInt lits, ConstraintID id) {
            this(lits);
            this.id = id;
        }
        
        /**
         * Retrieves the literals in the constraint.
         * @return The constraint's literals.
         */
        protected IVecInt getLits() { return this.lits; }
        
        /**
         * Checks if the constraint is a removable constraint.
         * @return True if it is a removable constraint, false otherwise.
         */
        public boolean isRemovable() { return this.id != null; }
        
        /**
         * Retrieves the constraint's id.
         * @return The constraint's id if it is a removable constraint, null otherwise.
         * @see {@link #isRemovable()}
         */
        public ConstraintID getID() { return this.id; }
        
        /**
         * Adds the constraint to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        abstract IConstr addToSolver(IPBSolver solver) throws ContradictionException;
        
    }
    
    /**
     * Abstract superclass for constraint objects with a right-hand side.
     */
    private abstract class ConstraintWithRHS extends Constraint {
        
        /**
         * The right-hand side of the constraint.
         */
        private BigInteger rhs = null;
        
        /**
         * Creates an instance of a constraint with a right-hand side.
         * @param lits The constraint's literals.
         * @param rhs The constraint's right-hand side.
         */
        ConstraintWithRHS(IVecInt lits, BigInteger rhs) {
            this(lits, rhs, null);
        }
        
        /**
         * Creates an instance of a constraint with a right-hand side.
         * @param lits The constraint's literals.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        ConstraintWithRHS(IVecInt lits, BigInteger rhs, ConstraintID id) {
            super(lits, id);
            this.rhs = rhs;
        }
        
        /**
         * Retrieves the right-hand side of the constraint.
         * @return The constraint's right-hand side.
         */
        protected BigInteger getRHS() { return this.rhs; }
        
    }
    
    /**
     * Abstract superclass for constraint objects with coefficients (like Pseudo-Boolean
     * constraints) and a right-hand side.
     */
    private abstract class ConstraintWithCoeffsAndRHS extends ConstraintWithRHS {
        
        /**
         * The coefficients in the constraint.
         */
        private IVec<BigInteger> coeffs = null;
        
        /**
         * Creates an instance of a constraint with coefficients and a right-hand side.
         * @param lits The literals in the constraint.
         * @param coeffs The coefficients in the constraint.
         * @param rhs The constraint's right-hand side.
         */
        ConstraintWithCoeffsAndRHS(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs) {
            this(lits, coeffs, rhs, null);
        }
        
        /**
         * Creates an instance of a constraint with coefficients and a right-hand side.
         * @param lits The literals in the constraint.
         * @param coeffs The coefficients in the constraint.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        ConstraintWithCoeffsAndRHS(
                IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs, ConstraintID id) {
            super(lits, rhs, id);
            this.coeffs = new Vec<BigInteger>(coeffs.size());
            coeffs.copyTo(this.coeffs);
        }
        
        /**
         * Retrieves the coefficients of the constraint.
         * @return The constraint's coefficients.
         */
        protected IVec<BigInteger> getCoeffs() { return this.coeffs; }
        
    }
    
    /**
     * Constraint object that represents a disjunction of literals.
     */
    private class Clause extends Constraint {
        
        /**
         * Creates an instance of a clause.
         * @param lits The clause's literals.
         */
        Clause(IVecInt lits) { super(lits); }
        
        /**
         * Creates an instance of a clause.
         * @param lits The clause's literals.
         * @param id The constraint's id. If null, then the clause is considered not to be a removable
         * clause.
         */
        Clause(IVecInt lits, ConstraintID id) { super(lits, id); }
        
        /**
         * Adds the clause to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object for the clause.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        @Override
        IConstr addToSolver(IPBSolver solver) throws ContradictionException {
            return solver.addClause(getLits());
        }
        
    }
    
    /**
     * Constraint object that represents the constraint that the sum of a given set of literals
     * must be less or equal to a given value.
     */
    private class AtMost extends ConstraintWithRHS {
        
        /**
         * Creates an instance of an at-most constraint.
         * @param lits The literals in the at-most constraint.
         * @param rhs The constraint's right-hand side.
         */
        AtMost(IVecInt lits, int rhs) {
            super(lits, BigInteger.valueOf(rhs));
        }
        
        /**
         * Creates an instance of an at-most constraint.
         * @param lits The literals in the at-most constraint.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        AtMost(IVecInt lits, int rhs, ConstraintID id) {
            super(lits, BigInteger.valueOf(rhs), id);
        }
        
        /**
         * Adds the at-most constraint to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object for the at-most constraint.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        // FIXME: choose between intValue and intValueExact depending on version
        @Override
        IConstr addToSolver(IPBSolver solver) throws ContradictionException {
            //return solver.addAtMost(getLits(), getRHS().intValueExact());
            return solver.addAtMost(getLits(), getRHS().intValue());
        }
        
    }
    
    /**
     * Constraint object that represents the constraint that the sum of a given set of literals
     * must be equal or greater than a given value.
     */
    private class AtLeast extends ConstraintWithRHS {
        
        /**
         * Creates an instance of an at-least constraint.
         * @param lits The literals in the at-least constraint.
         * @param rhs The constraint's right-hand side.
         */
        AtLeast(IVecInt lits, int rhs) {
            super(lits, BigInteger.valueOf(rhs));
        }
        
        /**
         * Creates an instance of an at-least constraint.
         * @param lits The literals in the at-least constraint.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        AtLeast(IVecInt lits, int rhs, ConstraintID id) {
            super(lits, BigInteger.valueOf(rhs), id);
        }
        
        /**
         * Adds the at-least constraint to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object for the at-least constraint.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        // FIXME: choose between intValue and intValueExact depending on version
        @Override
        IConstr addToSolver(IPBSolver solver) throws ContradictionException {
            //return solver.addAtLeast(getLits(), getRHS().intValueExact());
            return solver.addAtLeast(getLits(), getRHS().intValue());
        }
        
    }
    
    /**
     * Constraint object that represents the constraint that the sum of a given set of literals
     * must be equal to a given value.
     */
    private class Exactly extends ConstraintWithRHS {
        
        /**
         * Creates an instance of an exactly constraint.
         * @param lits The literals in the exactly constraint.
         * @param rhs The constraint's right-hand side.
         */
        Exactly(IVecInt lits, int rhs) {
            super(lits, BigInteger.valueOf(rhs));
        }
        
        /**
         * Creates an instance of an exactly constraint.
         * @param lits The literals in the exactly constraint.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        Exactly(IVecInt lits, int rhs, ConstraintID id) {
            super(lits, BigInteger.valueOf(rhs), id);
        }
        
        /**
         * Adds the exactly constraint to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object for the exactly constraint.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        // FIXME: choose between intValue and intValueExact depending on version
        @Override
        IConstr addToSolver(IPBSolver solver) throws ContradictionException {
            //return solver.addExactly(getLits(), getRHS().intValueExact());
            return solver.addExactly(getLits(), getRHS().intValue());
        }
        
    }
    
    /**
     * Constraint object that represents the constraint that the sum of a given set of literals
     * times the corresponding coefficients must be greater or equal to a given value.
     */
    private class GreaterOrEqual extends ConstraintWithCoeffsAndRHS {
        
        /**
         * Creates an instance of a greater or equal constraint.
         * @param lits The constraint's literals.
         * @param coeffs The constraint's coefficients.
         * @param rhs The constraint's right-hand side.
         */
        public GreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs) {
            super(lits, coeffs, rhs);
        }
        
        /**
         * Creates an instance of a greater or equal constraint.
         * @param lits The constraint's literals.
         * @param coeffs The constraint's coefficients.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        public GreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs, ConstraintID id) {
            super(lits, coeffs, rhs, id);
        }
        
        /**
         * Adds the greater or equal constraint to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object for the greater or equal constraint.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        @Override
        IConstr addToSolver(IPBSolver solver) throws ContradictionException {
            return solver.addPseudoBoolean(getLits(), getCoeffs(), true, getRHS());
        }
        
    }
    
    /**
     * Constraint object that represents the constraint that the sum of a given set of literals
     * times the corresponding coefficients must be less or equal to a given value.
     */
    private class LessOrEqual extends ConstraintWithCoeffsAndRHS {
        
        /**
         * Creates an instance of a less or equal constraint.
         * @param lits The constraint's literals.
         * @param coeffs The constraint's coefficients.
         * @param rhs The constraint's right-hand side.
         */
        public LessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs) {
            super(lits, coeffs, rhs);
        }
        
        /**
         * Creates an instance of a less or equal constraint.
         * @param lits The constraint's literals.
         * @param coeffs The constraint's coefficients.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        public LessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs, ConstraintID id) {
            super(lits, coeffs, rhs, id);
        }
        
        /**
         * Adds the less or equal constraint to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object for the less or equal constraint.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        @Override
        IConstr addToSolver(IPBSolver solver) throws ContradictionException {
            return solver.addPseudoBoolean(getLits(), getCoeffs(), false, getRHS());
        }
        
    }
    
    /**
     * Constraint object that represents the constraint that the sum of a given set of literals
     * times the corresponding coefficients must be equal to a given value.
     */
    private class Equal extends ConstraintWithCoeffsAndRHS {
        
        /**
         * Creates an instance of an equal constraint.
         * @param lits The constraint's literals.
         * @param coeffs The constraint's coefficients.
         * @param rhs The constraint's right-hand side.
         */
        public Equal(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs) {
            super(lits, coeffs, rhs);
        }
        
        /**
         * Creates an instance of an equal constraint.
         * @param lits The constraint's literals.
         * @param coeffs The constraint's coefficients.
         * @param rhs The constraint's right-hand side.
         * @param id The constraint's id. If null, then the constraint is considered not to be a removable
         * constraint.
         */
        public Equal(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs, ConstraintID id) {
            super(lits, coeffs, rhs, id);
        }
        
        /**
         * Adds the equal constraint to a SAT4J PBS solver.
         * @param solver The SAT4J PBS solver.
         * @return A SAT4J constraint object for the equal constraint.
         * @throws ContradictionException If the solver detects that the addition of this constraint
         * would result in a contradiction.
         */
        @Override
        IConstr addToSolver(IPBSolver solver) throws ContradictionException {
            return solver.addExactly(getLits(), getCoeffs(), getRHS());
        }
        
    }
    
    /**
     * An instance of the underlying SAT4J PBS solver.
     */
    protected IPBSolver solver = null;
    
    /**
     * Boolean used to store the satisfiability of the PBS instance of the last call to
     * {@link #solve()} or {@link #solve(IVecInt)}. True if the instance is satisfiable, false
     * otherwise.
     */
    protected boolean is_sat = false;
    
    /**
     * Boolean used to store if the PBS instance was solved successfully on the last call to
     * {@link #solve()} or {@link #solve(IVecInt)}. True if so, false otherwise.
     */
    protected boolean is_solved = false;
    
    /**
     * Mapping of constraint ids to SAT4J's {@link IConstr} objects. In SAT4J, constraints are
     * removed by providing the {@link IConstr} object, returned by the add methods, to the
     * {@link IPBSolver#removeConstr(IConstr)} method.
     */
    private Map<ConstraintID, IConstr> rem_map = new HashMap<ConstraintID, IConstr>();
    
    /**
     * Set used to store not removable constraints that SAT4J satisfied by unit propagation.
     * @see #removeConstraint_core(ConstraintID, boolean)
     */
    private Set<Constraint> unit_sat_constraints = new HashSet<Constraint>();
    
    /**
     * Map used to store removable constraints that SAT4J satisfied by unit propagation.
     * @see #removeConstraint_core(ConstraintID, boolean)
     */
    private Map<ConstraintID, Constraint> unit_sat_removable = new HashMap<ConstraintID, Constraint>();
    
    /**
     * Creates an instance of a Pseudo-Boolean Satisfaction solver.
     */
    public PseudoBooleanSolver() { this.solver = SolverFactory.newDefault(); }

    @Override
    public void newVar() { newVars(1); }

    @Override
    public void newVars(int nvars) { this.solver.newVar(this.solver.nVars() + nvars); }

    @Override
    public int nVars() { return this.solver.nVars(); }
    
    /**
     * Adds a constraint to the solver. If the constraint is removable, stores a mapping between the
     * constraint's ID and the corresponding SAT4J {@link IConstr} object.
     * If SAT4J satisfied the constraint by unit propagation, then the constraint is stored. If some of the
     * constraints are removed in the future, then the constraints satisfied by unit propagation must be
     * added again to the SAT4J solver for correctness.
     * @param constraint The constraint.
     * @return The constraint's id (for convenience).
     * @throws ContradictionException If the solver detects that the addition of this constraint would
     * result in a contradiction.
     */
    private ConstraintID addConstraint(Constraint constraint) throws ContradictionException {
        IConstr constr = constraint.addToSolver(this.solver);
        if (constraint.isRemovable()) {
            assert(constraint.getID() != null);
            this.rem_map.put(constraint.getID(), constr);
        }
        if (constr == null && constraint.isRemovable()) {
            this.unit_sat_removable.put(constraint.getID(), constraint);
        }
        else if (constr == null) {
            this.unit_sat_constraints.add(constraint);
        }
        return constraint.getID();
    }

    @Override
    public ConstraintID addRemovableExactly(IVecInt lits, int rhs) throws ContradictionException {
        return addConstraint(new Exactly(lits, rhs, ConstraintID.makeFresh()));
    }

    @Override
    public ConstraintID addRemovableAtMost(IVecInt lits, int rhs) throws ContradictionException {
        return addConstraint(new AtMost(lits, rhs, ConstraintID.makeFresh()));
    }

    @Override
    public ConstraintID addRemovableAtLeast(IVecInt lits, int rhs) throws ContradictionException {
        return addConstraint(new AtLeast(lits, rhs, ConstraintID.makeFresh()));
    }

    @Override
    public void addExactly(IVecInt lits, int rhs) throws ContradictionException {
        addConstraint(new Exactly(lits, rhs));
    }

    @Override
    public void addAtMost(IVecInt lits, int rhs) throws ContradictionException {
        addConstraint(new AtMost(lits, rhs));
    }

    @Override
    public void addAtLeast(IVecInt lits, int rhs) throws ContradictionException {
        addConstraint(new AtLeast(lits, rhs));
    }

    @Override
    public ConstraintID addRemovableEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        return addConstraint(new Equal(lits, coeffs, rhs, ConstraintID.makeFresh()));
    }

    @Override
    public ConstraintID addRemovableGreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        return addConstraint(new GreaterOrEqual(lits, coeffs, rhs, ConstraintID.makeFresh()));
    }

    @Override
    public ConstraintID addRemovableLessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        return addConstraint(new LessOrEqual(lits, coeffs, rhs, ConstraintID.makeFresh()));
    }

    @Override
    public void addEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        addConstraint(new Equal(lits, coeffs, rhs));
    }

    @Override
    public void addGreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        addConstraint(new GreaterOrEqual(lits, coeffs, rhs));
    }

    @Override
    public void addLessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        addConstraint(new LessOrEqual(lits, coeffs, rhs));
    }

    @Override
    public ConstraintID addRemovableClause(IVecInt lits) throws ContradictionException {
        return addConstraint(new Clause(lits, ConstraintID.makeFresh()));
    }

    @Override
    public void addClause(IVecInt lits) throws ContradictionException {
        addConstraint(new Clause(lits));
    }
    
    /**
     * Re-adds non-removable constraints previously satisfied by unit propagation to the solver. Constraints
     * no longer satisfied by unit propagation are cleaned up.
     */
    private void reAddNonRemovableSatisfied() {
        Iterator<Constraint> it = this.unit_sat_constraints.iterator();
        while (it.hasNext()) {
            Constraint constraint = it.next();
            try {
                IConstr constr = constraint.addToSolver(this.solver);
                if (constr != null) {
                    it.remove();
                }
            }
            catch (ContradictionException ce) { // this will never happen if called inside removeConstraint_core
                throw new RuntimeException(ce); // if formula was satisfied with the constraint, then it also is without
            }
        }
    }
    
    /**
     * Re-adds removable constraints previously satisfied by unit propagation to the solver. Constraints no
     * longer satisfied by unit propagation are cleaned up and an ID to {@link IConstr} object mapping is
     * added for that constraint.
     */
    private void reAddRemovableSatisfied() {
        Iterator<ConstraintID> it = this.unit_sat_removable.keySet().iterator();
        while (it.hasNext()) {
            ConstraintID id = it.next();
            Constraint constraint = this.unit_sat_removable.get(id);
            assert(id.equals(constraint.getID()));
            try {
                IConstr constr = constraint.addToSolver(this.solver);
                if (constr != null) {
                    it.remove();
                    this.rem_map.put(id, constr);
                }
            }
            catch (ContradictionException ce) { // this will never happen if called inside removeConstraint_core
                throw new RuntimeException(ce); // if formula was satisfied with the constraint, then it also is without
            }
        }
    }
    
    /**
     * Removes a constraint from the solver.
     * @param id The id of the constraint to be removed.
     * @param add_unit_satisfied True if constraints previously satisfied by unit propagation are to be
     * re-added to the solver, false otherwise.
     */
    private void removeConstraint_core(ConstraintID id, boolean add_unit_satisfied) {
        assert(this.rem_map.containsKey(id));
        IConstr constr_obj = this.rem_map.get(id);
        if (constr_obj != null) { // not documented, but sat4j may return null if the constraint was satisfied by unit propagation
            this.solver.removeConstr(constr_obj);
        }
        else {
            assert(this.unit_sat_removable.containsKey(id));
            this.unit_sat_removable.remove(id);
            add_unit_satisfied = false;
        }
        if (add_unit_satisfied) {
            reAddRemovableSatisfied();
            reAddNonRemovableSatisfied();
        }
        this.rem_map.remove(id);
    }

    @Override
    public void removeConstraint(ConstraintID id) {
        removeConstraint_core(id, true);
    }
    
    // re-implemented because this class' implementation of removeConstraint makes ConstraintSolver's
    // removeConstraints inefficient
    @Override
    public void removeConstraints(IVec<ConstraintID> ids) {
        for (int i = 0; i < ids.size(); ++i) {
            removeConstraint_core(ids.get(i), i == ids.size()-1);
        }
    }
    
    @Override
    protected long getRemainingTime() {
        long timeout = super.getRemainingTime();
        return (timeout > Integer.MAX_VALUE) ? Integer.MAX_VALUE : timeout; // FIXME: implicit limit, should change eventually
    }

    @Override
    public void solve(IVecInt asms) {
        this.solver.expireTimeout();
        this.solver.setTimeout((int)getRemainingTime());
        Timer timer = new Timer();          // timer required because SAT4J does not support conflict and time
        timer.schedule(new TimerTask() {    // timeouts simultaneously
            @Override
            public void run() {
                solver.expireTimeout();
            }
        }, 1000*getRemainingTime());
        if (getMaxConflicts() != NO_TIMEOUT) {
            this.solver.setTimeoutOnConflicts((int)getMaxConflicts());
        }
        try {
            is_sat = this.solver.isSatisfiable(asms);
            is_solved = true;
        }
        catch (TimeoutException e) {
            is_solved = false;
        }
        catch (NullPointerException e) {    // FIXME: DIRTY HACK!!! SAT4J is buggy and might throw a
            is_solved = false;              // NullPointerException when expiring timeout from outside;
        }                                   // might lead to unexpected behavior if solver is re-used
        timer.cancel();
        timer.purge();
    }

    @Override
    public boolean isSolved() { return this.is_solved; }

    @Override
    public boolean isSatisfiable() { return this.is_sat; }

    @Override
    public boolean isUnsatisfiable() { return !isSatisfiable(); }

    @Override
    public boolean modelValue(int lit) {
        assert(lit != 0);
        return lit > 0 ? this.solver.model(lit) : !this.solver.model(-lit);
    }
    
    @Override
    public IVecInt unsatExplanation() {
        try {
            IVecInt explanation = this.solver.unsatExplanation();
            return explanation == null ? new VecInt() : explanation;
        }
        catch (NullPointerException npe) { /* left empty on purpose, bug in sat4j may cause a NullPointerException */ }
        return new VecInt();
    }

}
