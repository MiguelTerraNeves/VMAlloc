package vmalloc.algorithm.evolutionary;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.spi.ProblemProvider;

import vmalloc.domain.JobVec;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachineVec;

/**
 * Problem factory class for Virtual Machine Consolidation problems. Required in order to perform solution
 * set analysis.
 * @author Miguel Terra-Neves
 */
public class VMCwMProblemProvider extends ProblemProvider {

    /**
     * Vector with the instance's physical machines
     */
    private final PhysicalMachineVec pms;
    
    /**
     * Vector with the instance's jobs.
     */
    private final JobVec jobs;
    
    /**
     * Vector with the instance's pre-existing virtual machine to server mappings.
     */
    private final MappingVec mappings;
    
    /**
     * The budget percentile of the total memory capacity that can be used for migrations.
     */
    private final double max_mig_percentile;
    
    /**
     * Boolean indicating if objective function computation should include the denominators in the expression.
     */
    private boolean include_denominators;

    /**
     * Creates a factory object for a given instance of the Virtual Machine Consolidation problem.
     * @param pms The set of physical machines.
     * @param jobs The set of jobs.
     * @param mappings The set of pre-existing virtual machine to physical machine mappings.
     * @param max_mig_percentile The migration budget percentile, i.e., the percentile of the total memory
     * capacity that can be used for migrations.
     */
    public VMCwMProblemProvider(PhysicalMachineVec pms,
                                JobVec jobs,
                                MappingVec mappings,
                                double max_mig_percentile) {
        this.pms = pms;
        this.jobs = jobs;
        this.mappings = mappings;
        this.max_mig_percentile = max_mig_percentile;
        this.include_denominators = true;
    }
    
    /**
     * Discards the denominators in the objective functions.
     */
    public void discardDenominators() { this.include_denominators = false; }
    
    /**
     * If the Virtual Machine Consolidation problem name is given as input, produces a {@link VMCwMProblem}
     * object with the instance provided when the problem provider was created. Otherwise, returns null.
     * @param name The problem name.
     * @return An instance of Virtual Machine Consolidation problem if {@link VMCwMProblem#NAME} is given as
     * input, null otherwise.
     */
    @Override
    public Problem getProblem(String name) {
        if (name.equals(VMCwMProblem.NAME)) {
            VMCwMProblem problem = new VMCwMProblem(this.pms,
                                                    this.jobs,
                                                    this.mappings,
                                                    this.max_mig_percentile);
            if (!this.include_denominators) {
                problem.discardDenominators();
            }
            return problem;
        }
        return null;
    }

    /**
     * If the Virtual Machine Consolidation problem name is given as input, produces the default reference
     * solution set to use when performing analysis of populations produced by algorithms. Otherwise, returns
     * null. This is required for computing hypervolumes, for example.
     * @param name The problem name.
     * @return The default reference set if {@link VMCwMProblem#NAME} is given as input, null otherwise.
     * @see {@link VMCwMProblem#analyzePopulations(java.util.Map)}
     */
    @Override
    public NondominatedPopulation getReferenceSet(String name) {
        if (name.equals(VMCwMProblem.NAME)) {
            VMCwMProblem instance =
                    new VMCwMProblem(this.pms, this.jobs, this.mappings, this.max_mig_percentile);
            return instance.getDefaultReferenceSet();
        }
        return null;
    }
    
}
