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

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.constraints.ConstraintID;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.utils.CollectionUtils;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Multi-Objective
 * Combinatorial Optimization problem and enumerating its Pareto minimal correction subsets using the CLD
 * algorithm. Refer to:
 * Marques-Silva, Joao, et al. "On computing minimal correction subsets." Twenty-Third International Joint
 * Conference on Artificial Intelligence. 2013.
 * @author Miguel Terra-Neves
 */
public class ParetoCLD extends MultiObjectiveConstraintBasedAllocAlgorithm {

    /**
     * Stores the maximum number of conflicts allowed before merging a partition with the next one in the
     * stratified algorithm.
     */
    private long part_max_confl = 200000;
    
    /**
     * Stores the maximum number of conflicts allowed for each run of CLD.
     */
    private long max_confl = NO_TIMEOUT;
    
    /**
     * Sets the maximum number of conflicts allowed for each run of CLD. Only meant to be used by
     * sub-classes.
     * @param max_confl The maximum number of conflicts.
     */
    protected void setMaxConflicts(long max_confl) { this.max_confl = max_confl; }
    
    /**
     * Set the maximum number of conflicts allowed before merging a partition with the next one in the
     * stratified algorithm.
     * @param max_confl The maximum number of conflicts.
     */
    public void setPartMaxConflicts(long max_confl) { this.part_max_confl = max_confl; }
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that uses the CLD algorithm to
     * enumerate Pareto minimal correction subsets.
     * @param instance The problem instance.
     */
    public ParetoCLD(VMCwMProblem instance) { super(instance); }

    /**
     * Set a given constraint solver's maximum number of conflicts.
     * @param solver The constraint solver.
     * @param base_confl An offset for the maximum number of conflicts.
     */
    private void setSolverMaxConflicts(ConstraintSolver solver, long base_confl) {
        long confl_limit = this.part_max_confl;
        if (this.max_confl != NO_TIMEOUT) {
            confl_limit = Math.min(confl_limit, this.max_confl - (solver.getConflicts() - base_confl));
        }
        solver.setMaxConflicts(confl_limit);
    }
    
    /**
     * Helper method that runs the stratified CLD algorithm for MCS enumeration.
     * @param solver A constraint solver already initialized with a working formula.
     * @param asms A vector of literals that must be satisfied by the solutions found by the solver.
     * @param undef_fmls A vector of partitions of the set of literals for which we want to find an MCS.
     * @param mss An output vector used to store the maximal satisfiable subset.
     * @param to_remove_ids An output vector in which to store the IDs of removable constraints.
     * @return True if an MCS was found, false otherwise.
     */
    protected boolean runCLD(ConstraintSolver solver,
                             IVecInt asms,
                             IVec<IVecInt> undef_fmls,
                             IVecInt mss,
                             IVec<ConstraintID> to_remove_ids) {
        if (getRemainingTime() <= 0) { return false; }
        boolean[] last_model = null;
        boolean mcs_exists = false;
        long base_confl = solver.getConflicts();
        int no_model_count = 0, asm_top = asms.size();
        for (int undef_idx = 0; undef_idx < undef_fmls.size(); undef_idx++) {
            IVecInt undef_part = undef_fmls.get(undef_idx);
            if (no_model_count > 20) {      // TODO: make this configurable
                System.out.println("c No progress for a while, merging last " + (undef_fmls.size() - undef_idx) +
                                   " partitions.");
                for (int i = undef_fmls.size()-1; i > undef_idx; --i) {
                    undef_fmls.get(i).moveTo(undef_part);
                    undef_fmls.pop();
                }
            }
            try {
                System.out.println("c Solving partition " + undef_idx + " of size " + undef_part.size());
                boolean done = false;
                while (!done) {
                    if (    getRemainingTime() <= 0 ||
                            (this.max_confl != NO_TIMEOUT && solver.getConflicts() - base_confl >= this.max_confl)) {
                        asms.shrinkTo(asm_top);
                        return mcs_exists;
                    }
                    if (mcs_exists) {
                        IVecInt sat_lits = extractSatisfied(last_model, undef_part);
                        sat_lits.copyTo(mss);
                        sat_lits.copyTo(asms);
                    }
                    if (undef_part.isEmpty()) {
                        to_remove_ids.push(null);
                        printElapsedTime();
                        break;
                    }
                    to_remove_ids.push(solver.addRemovableClause(undef_part));
                    if (undef_idx < undef_fmls.size()-1 || this.max_confl != NO_TIMEOUT) {
                        setSolverMaxConflicts(solver, base_confl);
                    }
                    System.out.println("c Computing mapping");
                    checkSAT(solver, asms);
                    solver.resetMaxConflicts();
                    if (!solver.isSolved() && getRemainingTime() > 0 && undef_idx < undef_fmls.size()-1) {
                        undef_idx++;
                        assert(undef_idx < undef_fmls.size());
                        System.out.println("c Conflict timeout, adding partition " + undef_idx + " of size " +
                                           undef_fmls.get(undef_idx).size());
                        undef_fmls.get(undef_idx).moveTo(undef_part);
                        solver.removeConstraint(to_remove_ids.last());
                        to_remove_ids.pop();
                    }
                    else if (!solver.isSolved()) {
                        asms.shrinkTo(asm_top);
                        return mcs_exists;
                    }
                    else if (solver.isSatisfiable()) {
                        mcs_exists = true;
                        no_model_count = -1;    // incremented to 0 when MCS is proven
                        saveSolution(modelToAllocation(solver,
                                                       this.instance.getPhysicalMachines(),
                                                       this.instance.getJobs(),
                                                       this.job_vars),
                                     true);
                        last_model = solver.getModel();
                    }
                    else {
                        no_model_count += 1;
                        done = true;
                    }
                }
            }
            catch (ContradictionException e) {
                to_remove_ids.push(null);
            }
            System.out.println("c Partition " + undef_idx + " solved, next one");
            printElapsedTime();
            if (to_remove_ids.last() != null) { solver.removeConstraint(to_remove_ids.last()); }
            to_remove_ids.pop();
            for (int i = 0; i < undef_part.size(); ++i) {
                asms.push(-undef_part.get(i));
            }
        }
        asms.shrinkTo(asm_top);
        return mcs_exists;
    }
    
    // FIXME: it'll run forever if no time limit is given when hash functions are enabled
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
        IVec<IVecInt> undef_fmls = buildUndefFormulas();
        IVec<ConstraintID> to_remove_ids = new Vec<ConstraintID>();
        IVec<ConstraintID> hash_ids = new Vec<ConstraintID>();
        IVecInt asms = new VecInt(), mss = new VecInt();
        if (hashFunctionsEnabled()) {
            System.out.println("c Setting initial hash function");
            hash_ids = setHashFunction(solver,
                                       flattenLitVectors(flattenJobVars(this.job_vars)),
                                       asms);
            assert(asms.size() > 0);
        }
        else if (pathDiversificationEnabled()) {
            System.out.println("c Initializing diversification paths");
            initDiversificationPaths();
            asms = pathToAssumptions(nextDiversificationPath());
            System.out.println("c Generating MCS for path " + litsToString(asms));
        }
        while (true) {
            mss.clear();
            boolean mcs_exists = runCLD(solver, asms, undef_fmls, mss, to_remove_ids);
            if (getRemainingTime() <= 0) {
                printTimeoutMessage();
                return;
            }
            if (mcs_exists) {
                IVecInt mcs = new VecInt();
                for (int i = 0; i < undef_fmls.size(); ++i) {
                    undef_fmls.get(i).copyTo(mcs);
                }
                if (    (hashFunctionsEnabled() || pathDiversificationEnabled()) &&
                        CollectionUtils.valuesIntersect(asms, solver.unsatExplanation())) {
                    System.out.println("c Validating MCS");
                    IVecInt asm_neg_clause = new VecInt();
                    for (int i = 0; i < asms.size(); ++i) {
                        asm_neg_clause.push(-asms.get(i));
                    }
                    asms.clear();
                    try {
                        to_remove_ids.push(solver.addRemovableClause(asm_neg_clause));
                        mss.copyTo(asms);
                        runCLD(solver, asms, new Vec<IVecInt>(new IVecInt[] { mcs }), mss, to_remove_ids);
                        if (getRemainingTime() <= 0) {
                            printTimeoutMessage();
                            return;
                        }
                    }
                    catch (ContradictionException e) {
                        assert(false); // should not happen
                    }
                }
                System.out.println("c MCS computed, generating another one");
                solver.removeConstraints(to_remove_ids);
                to_remove_ids.clear();
                try {
                    solver.addClause(mcs); // block MCS
                    undef_fmls = buildUndefFormulas();
                }
                catch (ContradictionException e) {
                    printOptimum();
                    return;
                }
            }
            else if (pathDiversificationEnabled()) {
                System.out.println("c No MCS for path " + litsToString(asms) + ", discarding path");
                discardLastDiversificationPath();
            }
            else if (!hashFunctionsEnabled()) {
                printOptimum();
                return;
            }
            if (hashFunctionsEnabled()) {
                System.out.println("c Generating new hash function");
                solver.removeConstraints(hash_ids);
                hash_ids = setHashFunction(solver,
                                           flattenLitVectors(flattenJobVars(this.job_vars)),
                                           asms);
            }
            if (pathDiversificationEnabled()) {
                PathNode path = nextDiversificationPath();
                if (path == null) {
                    printOptimum();
                    return;
                }
                else {
                    asms = pathToAssumptions(path);
                    System.out.println("c Generating MCS for path " + litsToString(asms));
                }
            }
            solver.removeConstraints(to_remove_ids);
            to_remove_ids.clear();
        }
    }

}
