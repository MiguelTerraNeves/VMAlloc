package vmalloc.algorithm;

import java.math.BigInteger;

import org.sat4j.specs.ContradictionException;

import vmalloc.algorithm.constraints.INewBestHandler;
import vmalloc.algorithm.constraints.PBOSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Pseudo-Boolean
 * Optimization problem. This is a single-objective algorithm that minimizes only the number of physical
 * machines that are ON.
 * @author Miguel Terra-Neves
 */
public class PBOAlloc extends ConstraintBasedAllocAlgorithm {
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on Pseudo-Boolean
     * Optimization.
     * @param instance The problem instance.
     */
    public PBOAlloc(VMCwMProblem instance) { super(instance); }
    
    /**
     * Makes the Pseudo-Boolean Optimization solver and initializes it with the respective formula for
     * the single-objective Virtual Machine Consolidation problem.
     * @return The Pseudo-Boolean Optimization solver.
     * @throws ContradictionException If the constraint solver detects that the base formula is
     * unsatisfiable.
     */
    protected PBOSolver buildSolver() throws ContradictionException {
        PBOSolver solver = new PBOSolver();
        addVMCConstraints(solver);
        System.out.println("c Setting objective function");
        solver.setObjectiveFunction(this.pm_vars);
        System.out.println("c Done");
        printElapsedTime();
        return solver;
    }
    
    @Override
    public void allocate() {
        System.out.println("c WARNING: PBO minimizes number of physical machines, not energy consumption");
        PBOSolver solver;
        try {
            solver = buildSolver();
        }
        catch (ContradictionException e) {
            printUnsatisfiable();
            return;
        }
        solver.setTimeout(getRemainingTime());
        solver.solve(new INewBestHandler() {
            public void handleNewBest(BigInteger best) {
                printUsedPMsCount(best.intValue());
            }
        });
        if (solver.isSolved()) {
            if (solver.isSatisfiable()) {
                saveSolution(modelToAllocation(solver,
                                               this.instance.getPhysicalMachines(),
                                               this.instance.getJobs(),
                                               job_vars));
            }
            if (solver.foundOptimum()) {
                printOptimum();
            }
        }
    }

}
