package vmalloc.algorithm;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.algorithm.constraints.ConstraintID;
import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.exception.NotSupportedException;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Multi-Objective
 * Combinatorial Optimization problem and enumerating its Pareto minimal correction subsets using the LBX
 * algorithm. Refer to:
 * Mencia, Carlos, Alessandro Previti, and Joao Marques-Silva. "Literal-based MCS extraction." IJCAI. Vol. 15.
 * 2015.
 * @author Miguel Terra-Neves
 */
public class ParetoLBX extends MultiObjectiveConstraintBasedAllocAlgorithm {

    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that uses the LBX algorithm to
     * enumerate Pareto minimal correction subsets.
     * @param instance The problem instance.
     */
    public ParetoLBX(VMCwMProblem instance) { super(instance); }
    
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
        IVec<IVecInt> undef_partitions = buildUndefFormulas();
        if (undef_partitions.size() > 0) {      // TODO: implement stratification
            throw new NotSupportedException("ParetoLBX does not support stratification");
        }
        IVecInt undef_fmls = undef_partitions.get(0);
        IVecInt asms = new VecInt(), mcs = new VecInt();
        IVec<ConstraintID> to_remove = new Vec<ConstraintID>();
        while (true) {
            System.out.println("c Computing mapping");
            checkSAT(solver, asms);
            if (!solver.isSolved()) {
                printTimeoutMessage();
                return;
            }
            else if (solver.isSatisfiable()) {
                saveSolution(modelToAllocation(solver,
                                               this.instance.getPhysicalMachines(),
                                               this.instance.getJobs(),
                                               this.job_vars),
                             true);
                IVecInt satisfied = extractSatisfied(solver, undef_fmls);
                try {
                    addVecToVec(solver.addRemovableConjunction(satisfied), to_remove); // FIXME: could use assumptions instead (consider giving it a try)
                }
                catch (ContradictionException e) {
                    assert(false); // should not happen
                }
            }
            else {
                if (asms.size() == 0) {
                    assert(mcs.size() == 0);
                    printOptimum();
                    return;
                }
                else {
                    try {
                        to_remove.push(solver.addRemovableClause(new VecInt(new int[] { -asms.get(0) })));
                        mcs.push(asms.get(0));
                    }
                    catch (ContradictionException e) {
                        assert(false); // should not happen
                    }
                }
            }
            if (undef_fmls.size() > 0) {
                asms = new VecInt(new int[] { undef_fmls.last() });
                undef_fmls.pop();
            }
            else {
                assert(mcs.size() > 0);
                System.out.println("c MCS computed, generating another one");
                solver.removeConstraints(to_remove);
                to_remove.clear();
                asms.clear();
                try {
                    solver.addClause(mcs);
                    undef_fmls = buildUndefFormulas().get(0);
                    mcs.clear();
                }
                catch (ContradictionException e) {
                    printOptimum();
                    return;
                }
            }
        }
    }

}
