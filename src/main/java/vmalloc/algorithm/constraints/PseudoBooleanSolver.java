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
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
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
     * Stores a mapping of constraint ID's to activator literals of removable constraints.
     */
    private Map<ConstraintID, Integer> act_map = new HashMap<ConstraintID, Integer>();
    
    /**
     * Creates a fresh activator literal.
     * @return The activator literal.
     */
    private int newActivator() {
        newVar();
        return nVars();
    }
    
    /**
     * Given a clause and an activator literal, produces a clause activated by that literal.
     * @param lits The clause's literals.
     * @param act The activator literal.
     * @return An equivalent clause activated by the literal {@code act}.
     */
    private IVecInt pushActivator(IVecInt lits, int act) {
        IVecInt act_lits = new VecInt(lits.size()+1);
        lits.copyTo(act_lits);
        act_lits.unsafePush(-act);
        return act_lits;
    }
    
    /**
     * Stores an activator literal with a fresh constraint ID, and returns that ID.
     * @param act The activator literal.
     * @return A fresh ID for the literal's constraint.
     */
    private ConstraintID storeActivator(int act) {
        ConstraintID id = ConstraintID.makeFresh();
        act_map.put(id, act);
        return id;
    }
    
    /**
     * An interface for objects that check if a given coefficient satisfies some condition.
     * @author Miguel Terra-Neves
     */
    private interface ICoeffChecker {
        public boolean check(BigInteger c);
    }
    
    /**
     * Retrieves the sum of the coefficients that satisfy some condition implemented by a given coefficient
     * checker.
     * @param coeffs The coefficients.
     * @param checker The coefficient checker.
     * @return The sum of the coefficients that satisfy the condition implemented by {@code checker}.
     */
    private BigInteger getCoeffSum(IVec<BigInteger> coeffs, ICoeffChecker checker) {
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < coeffs.size(); ++i) {
            BigInteger coeff = coeffs.get(i);
            if (checker.check(coeff)) {
                sum = sum.add(coeff);
            }
        }
        return sum;
    }
    
    /**
     * Retrieves the sum of the positive coefficients in a given vector.
     * @param coeffs The coefficients.
     * @return The sum of the positive coefficients.
     */
    private BigInteger getMaxSum(IVec<BigInteger> coeffs) {
        return getCoeffSum(coeffs, new ICoeffChecker() {
            @Override
            public boolean check(BigInteger c) { return c.signum() > 0; }
        });
    }
    
    /**
     * Retrieves the sum of the negative coefficients in a given vector.
     * @param coeffs The coefficients.
     * @return The sum of the negative coefficients.
     */
    private BigInteger getMinSum(IVec<BigInteger> coeffs) {
        return getCoeffSum(coeffs, new ICoeffChecker() {
            @Override
            public boolean check(BigInteger c) { return c.signum() < 0; }
        });
    }
    
    /**
     * Creates a vector of ones with a given size.
     * @param size The vector size.
     * @return A vector with {@code size} ones.
     */
    private IVec<BigInteger> makeUnitCoeffs(int size) {
        IVec<BigInteger> coeffs = new Vec<BigInteger>();
        for (int i = 0; i < size; ++i) {
            coeffs.push(BigInteger.ONE);
        }
        return coeffs;
    }
    
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

    @Override
    public ConstraintID addRemovableExactly(IVecInt lits, int rhs) throws ContradictionException {
        int act = newActivator();
        IVecInt act_lits = pushActivator(lits, act);
        IVec<BigInteger> act_coeffs = makeUnitCoeffs(lits.size());
        act_coeffs.push(BigInteger.valueOf(rhs));
        addGreaterOrEqual(act_lits, act_coeffs, BigInteger.valueOf(rhs));
        act_coeffs.pop();
        act_coeffs.push(BigInteger.valueOf(-lits.size()));
        addLessOrEqual(act_lits, act_coeffs, BigInteger.valueOf(rhs));
        return storeActivator(act);
    }

    @Override
    public ConstraintID addRemovableAtMost(IVecInt lits, int rhs) throws ContradictionException {
        int act = newActivator();
        IVecInt act_lits = pushActivator(lits, act);
        IVec<BigInteger> act_coeffs = makeUnitCoeffs(lits.size());
        act_coeffs.push(BigInteger.valueOf(-lits.size()));
        addLessOrEqual(act_lits, act_coeffs, BigInteger.valueOf(rhs));
        return storeActivator(act);
    }

    @Override
    public ConstraintID addRemovableAtLeast(IVecInt lits, int rhs) throws ContradictionException {
        int act = newActivator();
        IVecInt act_lits = pushActivator(lits, act);
        IVec<BigInteger> act_coeffs = makeUnitCoeffs(lits.size());
        act_coeffs.push(BigInteger.valueOf(rhs));
        addGreaterOrEqual(act_lits, act_coeffs, BigInteger.valueOf(rhs));
        return storeActivator(act);
    }

    @Override
    public void addExactly(IVecInt lits, int rhs) throws ContradictionException {
        this.solver.addExactly(lits, rhs);
    }

    @Override
    public void addAtMost(IVecInt lits, int rhs) throws ContradictionException {
        this.solver.addAtMost(lits, rhs);
    }

    @Override
    public void addAtLeast(IVecInt lits, int rhs) throws ContradictionException {
        this.solver.addAtLeast(lits, rhs);
    }

    @Override
    public ConstraintID addRemovableEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        int act = newActivator();
        IVecInt act_lits = pushActivator(lits, act);
        IVec<BigInteger> act_coeffs = new Vec<BigInteger>(coeffs.size()+1);
        coeffs.copyTo(act_coeffs);
        act_coeffs.push(getMinSum(coeffs).negate().add(rhs));
        addGreaterOrEqual(act_lits, act_coeffs, rhs);
        act_coeffs.pop();
        act_coeffs.push(rhs.subtract(getMaxSum(coeffs)));
        addLessOrEqual(act_lits, act_coeffs, rhs);
        return storeActivator(act);
    }

    @Override
    public ConstraintID addRemovableGreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        int act = newActivator();
        IVecInt act_lits = pushActivator(lits, act);
        IVec<BigInteger> act_coeffs = new Vec<BigInteger>(coeffs.size()+1);
        coeffs.copyTo(act_coeffs);
        act_coeffs.push(getMinSum(coeffs).negate().add(rhs));
        addGreaterOrEqual(act_lits, act_coeffs, rhs);
        return storeActivator(act);
    }

    @Override
    public ConstraintID addRemovableLessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        int act = newActivator();
        IVecInt act_lits = pushActivator(lits, act);
        IVec<BigInteger> act_coeffs = new Vec<BigInteger>(coeffs.size()+1);
        coeffs.copyTo(act_coeffs);
        act_coeffs.push(rhs.subtract(getMaxSum(coeffs)));
        addLessOrEqual(act_lits, act_coeffs, rhs);
        return storeActivator(act);
    }

    @Override
    public void addEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        this.solver.addExactly(lits, coeffs, rhs);
    }

    @Override
    public void addGreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        this.solver.addPseudoBoolean(lits, coeffs, true, rhs);
    }

    @Override
    public void addLessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        this.solver.addPseudoBoolean(lits, coeffs, false, rhs);
    }

    @Override
    public ConstraintID addRemovableClause(IVecInt lits) throws ContradictionException {
        int act = newActivator();
        IVecInt act_lits = pushActivator(lits, act);
        addClause(act_lits);
        return storeActivator(act);
    }

    @Override
    public void addClause(IVecInt lits) throws ContradictionException {
        this.solver.addClause(lits);
    }

    @Override
    public void removeConstraint(ConstraintID id) {
        int act = this.act_map.get(id);
        this.act_map.remove(id);
        try {
            this.solver.addClause(new VecInt(new int[] { -act }));
        }
        catch (ContradictionException e) { /* only occurs if activator was added to empty clause */ }
    }
    
    @Override
    protected long getRemainingTime() {
        long timeout = super.getRemainingTime();
        return (timeout > Integer.MAX_VALUE) ? Integer.MAX_VALUE : timeout; // FIXME: implicit limit, should change eventually
    }

    @Override
    public void solve(IVecInt asms) {
        IVecInt act_asms = new VecInt(asms.size() + this.act_map.size());
        asms.copyTo(act_asms);
        for (Iterator<Integer> it = this.act_map.values().iterator(); it.hasNext();) {
            act_asms.unsafePush(it.next());
        }
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
            is_sat = this.solver.isSatisfiable(act_asms);
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
            if (explanation != null) {
                for (int i = 0; i < explanation.size();) {
                    if (this.act_map.containsValue(explanation.get(i))) {
                        explanation.set(i, explanation.last());
                        explanation.pop();
                    }
                    else {
                        ++i;
                    }
                }
                return explanation;
            }
        }
        catch (NullPointerException npe) { /* left empty on purpose, bug in sat4j may cause a NullPointerException */ }
        return new VecInt();
    }
    
    @Override
    public long getConflicts() {return this.solver.getStat().get("conflicts").longValue(); }

}
