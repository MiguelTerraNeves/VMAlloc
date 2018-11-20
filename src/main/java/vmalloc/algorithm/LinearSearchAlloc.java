package vmalloc.algorithm;

import org.sat4j.specs.ContradictionException;

import vmalloc.algorithm.constraints.ConstraintSolver;
import vmalloc.algorithm.constraints.PseudoBooleanSolver;
import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on formulating the instance as a Weighted Boolean
 * Optimization problem and solving it using linear search. This is a single-objective algorithm that
 * minimizes only the number of physical machines that are ON.
 * @author Miguel Terra-Neves
 */
public class LinearSearchAlloc extends ConstraintBasedAllocAlgorithm {
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that uses linear search.
     * @param instance The problem instance.
     */
    public LinearSearchAlloc(VMCwMProblem instance) { super(instance); }
    
    /**
     * Makes the pseudo-Boolean solver and initializes it with the base formula.
     * @return The pseudo-Boolean solver.
     * @throws ContradictionException If the constraint solver detects that the base formula is
     * unsatisfiable.
     */
    protected ConstraintSolver buildSolver() throws ContradictionException {
        ConstraintSolver solver = new PseudoBooleanSolver();
        addVMCConstraints(solver);
        return solver;
    }
    
    @Override
    public void allocate() {
        System.out.println("c WARNING: LS minimizes number of servers, not energy consumption");
        System.out.println("c Initializing");
        ConstraintSolver solver = null;
        try {
            solver = buildSolver();
        }
        catch (ContradictionException e) {
            printUnsatisfiable();
            return;
        }
        int ub = this.instance.getPhysicalMachines().size()+1;
        while (true) {
            System.out.println("c Computing mapping");
            checkSAT(solver);
            if (!solver.isSolved()) {
                printTimeoutMessage();
                return;
            }
            else if (solver.isSatisfiable()) {
                ub = getUsedPMsCount(solver, this.instance.getPhysicalMachines(), this.job_vars);
                printUsedPMsCount(ub);
                saveSolution(modelToAllocation(solver,
                                               this.instance.getPhysicalMachines(),
                                               this.instance.getJobs(),
                                               this.job_vars));
                try {
                    solver.addAtMost(this.pm_vars, ub-1);
                }
                catch (ContradictionException e) {
                    printOptimum();
                    return;
                }
            }
            else {
                printOptimum();
                return;
            }
        }
    }

}
