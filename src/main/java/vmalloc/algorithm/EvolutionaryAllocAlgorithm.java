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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.moeaframework.Executor;
import org.moeaframework.algorithm.PeriodicAction;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.operator.RandomInitialization;
import org.moeaframework.core.spi.OperatorFactory;

import vmalloc.Clock;
import vmalloc.algorithm.evolutionary.BinPackingInitialization;
import vmalloc.algorithm.evolutionary.RandomBinPackingInitialization;
import vmalloc.algorithm.evolutionary.SVUMProvider;
import vmalloc.algorithm.evolutionary.SmartMutationProvider;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.exception.NotSupportedException;

/**
 * Abstract superclass for Virtual Machine Consolidation problem solvers that rely on evolutionary
 * algorithms.
 * @author Miguel Terra-Neves
 */
public abstract class EvolutionaryAllocAlgorithm extends AllocAlgorithm {
    
    /**
     * Enumerator that represents types of population initialization operators.
     * @author Miguel Terra-Neves
     */
    public static enum InitializationType {
        
        /**
         * Fully random initialization.
         */
        RANDOM,
        
        /**
         * Random bin-packing initialization.
         * @see RandomBinPackingInitialization
         */
        RANDOM_PACKING,
        
        /**
         * First-fit initialization with randomized virtual machine orderings.
         * @see BinPackingInitialization
         */
        SHUFFLED_FIRST_FIT,
        
        /**
         * First-fit initialization with randomize virtual machine orderings and migration minimization.
         * @see BinPackingInitialization
         */
        SHUFFLED_VMCWM_HEURISTIC,
        
        /**
         * Same as {@link #SHUFFLED_FIRST_FIT}, except that a mix of bin-packing algorithms is used.
         * @see BinPackingInitialization
         */
        MIXED;
        
    }
    
    /**
     * Periodic action that collects the current set of feasible solutions in some evolutionary algorithm's
     * population and, optionally, logs them into a file.
     * @author Miguel Terra-Neves
     */
    private class SolutionCollector extends PeriodicAction {

        /**
         * Creates an instance of a solution collector.
         * @param algorithm The evolutionary algorithm to be instrumented with the collector.
         * @param frequency The frequency of solution collection measured in iterations of the evolutionary
         * algorithm.
         */
        public SolutionCollector(Algorithm algorithm, int frequency) {
            super(algorithm, frequency, FrequencyType.STEPS);
        }

        @Override
        public void doAction() {
            NondominatedPopulation pop = getResult();
            solutions.clear();
            for (int i = 0; i < pop.size(); ++i) {
                saveSolution(pop.get(i), true, true);
            }
            logProgress();
        }
        
    }
    
    /**
     * Periodic action that logs the number of feasible solutions in some evolutionary algorithm's
     * population.
     * @author Miguel Terra-Neves
     */
    private class FeasibleCountLogger extends PeriodicAction {

        /**
         * Stores the number of feasible solutions counted in the last execution of {@link #doAction()}.
         */
        int last_count = -1;
        
        /**
         * Creates an instance of the feasible solution count logger.
         * @param algorithm The evolutionary algorithm to be instrumented with the collector.
         * @param frequency The frequency of solution collection measured in iterations of the evolutionary
         * algorithm.
         */
        public FeasibleCountLogger(Algorithm algorithm, int frequency) {
            super(algorithm, frequency, FrequencyType.STEPS);
        }

        @Override
        public void doAction() {
            NondominatedPopulation pop = getResult();
            int feasible = 0;
            for (int i = 0; i < pop.size(); ++i) {
                if (!pop.get(i).violatesConstraints()) { ++feasible; }
            }
            if (last_count != feasible || this.iteration % 1000 == 0) {     // prevent excessive logging
                System.out.println("c :iteration " + this.iteration +
                                   " :elapsed-time " + Clock.getInstance().getElapsed() +
                                   " :nfeasible " + feasible);
            }
            this.last_count = feasible;
        }
        
    }
    
    /**
     * A string representation of the algorithm that will be executed.
     */
    private String algorithm = null;
    
    /**
     * The type of population initialization operator to use.
     */
    private InitializationType init_type = InitializationType.RANDOM;
    
    /**
     * A MOEA Framework executor object that manages the properties of the algorithm that will be executed
     * and runs it.
     */
    protected Executor exec = null;

    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on evolutionary
     * algorithms.
     * @param instance The problem instance.
     * @param algorithm The evolutionary algorithm to run.
     * @param encoding The variable encoding type to use.
     */
    public EvolutionaryAllocAlgorithm(VMCwMProblem instance,
                                      String algorithm,
                                      VMCwMProblem.Encoding encoding) {
        super(instance, encoding);
        this.exec = new Executor();
        this.algorithm = algorithm;
        OperatorFactory.getInstance().addProvider(new SVUMProvider());
        OperatorFactory.getInstance().addProvider(new SmartMutationProvider());
    }
    
    /**
     * Instruments a given evolutionary algorithm with a solution collector and feasible solution counter.
     * @param alg The algorithm to be instrumented.
     * @return The instrumented algorithm.
     */
    // TODO: configurable frequencies?
    protected Algorithm decorateWithPeriodicActions(Algorithm alg) {
        return new FeasibleCountLogger(new SolutionCollector(alg, 1), 1);
    }
    
    /**
     * Sets up an executor to run a given algorithm.
     * @param exec The executor.
     * @param alg A string representation of the algorithm.
     * @return The set-up executor.
     */
    private Executor setUpExecutor(Executor exec, String alg, VMCwMProblem instance) {
        return exec.withAlgorithm(alg).withProblem(instance);
    }
    
    /**
     * Makes an instance of the population initialization operator to use.
     * @param problem The problem instance.
     * @param pop_size The population size.
     * @return The initialization operator.
     */
    protected Initialization makeInitializer(VMCwMProblem problem, int pop_size) {
        if (init_type == InitializationType.RANDOM_PACKING) {
            return new RandomBinPackingInitialization(problem, pop_size);
        }
        else if (init_type == InitializationType.SHUFFLED_FIRST_FIT) {
            return new BinPackingInitialization(problem, pop_size, false, false);
        }
        else if (init_type == InitializationType.SHUFFLED_VMCWM_HEURISTIC) {
            return new BinPackingInitialization(problem, pop_size, true, false);
        }
        else if (init_type == InitializationType.MIXED) {
            return new BinPackingInitialization(problem, pop_size, false, true);
        }
        return new RandomInitialization(problem, pop_size);
    }
    
    /**
     * Makes an instance of the population initialization operator to use.
     * @param problem The problem instance. Only instances of {@link VMCwMProblem} are supported.
     * @param pop_size The population size.
     * @return The initialization operator.
     */
    protected Initialization makeInitializer(Problem problem, int pop_size) {
        if (!(problem instanceof VMCwMProblem)) {
            throw new NotSupportedException(
                    "Only instances of VMCwMProblem are supported for evolutionary algorithms");
        }
        return makeInitializer((VMCwMProblem)problem, pop_size);
    }
    
    /**
     * Checks if two solutions are equal. That is the case if both represent the exact same virtual machine
     * to physical machine allocation.
     * @param sol1 A solution.
     * @param sol2 Another solution.
     * @return True if {@code sol1} and {@code sol2} represent the same allocation, false otherwise.
     */
    protected boolean solutionEquals(Solution sol1, Solution sol2) {
        int[] x1 = this.instance.getVirtualMachineAssignment(sol1);
        int[] x2 = this.instance.getVirtualMachineAssignment(sol2);
        for (int i = 0; i < sol1.getNumberOfVariables(); ++i) {
            if (x1[i] != x2[i]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Sets the population size for the algorithm.
     * @param size The population size.
     */
    public void setPopulationSize(int size) { exec = exec.withProperty("populationSize", size); }
    
    /**
     * Sets the population initialization operator type to use.
     * @param type The initialization operator type.
     * @see InitializationType
     */
    public void setInitializationType(InitializationType type) { this.init_type = type; }
    
    /**
     * Dumps a solution's virtual machine to physical machine assignment to the standard output.
     * @param sol The solution.
     */
    protected void dumpSolution(Solution sol) {
        int[] x = this.instance.getVirtualMachineAssignment(sol);
        for (int i = 0; i < sol.getNumberOfVariables(); ++i) {
            System.out.print(x[i] + " ");
        }
    }
    
    /**
     * Prints a solution's virtual machine to physical machine assignment, preceded by a given label.
     * @param label The label.
     * @param sol The solution.
     */
    protected void printSolution(String label, Solution sol) {
        System.out.print("c Solution " + label + ": ");
        dumpSolution(sol);
        System.out.print("\n");
    }
    
    /**
     * Cleans up a given population by removing unfeasible solutions.
     * @param pop The population.
     * @return The cleaned population.
     */
    protected NondominatedPopulation cleanUpPopulation(NondominatedPopulation pop) {
        NondominatedPopulation new_pop = new NondominatedPopulation();
        for (int i = 0; i < pop.size(); ++i) {
            Solution sol = pop.get(i);
            if (!sol.violatesConstraints()) {
                new_pop.add(sol);
            }
        }
        return new_pop;
    }
    
    /**
     * Cleans up a set of populations by removing unfeasible solutions.
     * @param pops The list of populations.
     * @return The list of cleaned populations.
     */
    protected List<NondominatedPopulation> cleanUpPopulations(List<NondominatedPopulation> pops) {
        List<NondominatedPopulation> new_pops = new LinkedList<NondominatedPopulation>();
        for (Iterator<NondominatedPopulation> it = pops.iterator(); it.hasNext();) {
            new_pops.add(cleanUpPopulation(it.next()));
        }
        return new_pops;
    }
    
    @Override
    // FIXME: if no time limit is given, algorithm returns prematurely
    protected List<NondominatedPopulation> runMultipleSeeds(int nseeds) {
        System.out.println("c Applying evolutionary optimization");
        System.out.println("c Running with " + nseeds + " different seeds");
        this.exec = setUpExecutor(this.exec, this.algorithm, this.instance);
        return cleanUpPopulations(this.exec.withMaxTime(1000L*getRemainingTime()).runSeeds(nseeds));
    }
    
    @Override
    // FIXME: if no time limit is given, algorithm returns prematurely
    public void allocate() {
        System.out.println("c Applying evolutionary optimization");
        this.exec = setUpExecutor(this.exec, this.algorithm, this.instance);
        this.solutions = cleanUpPopulation(this.exec.withMaxTime(1000L*getRemainingTime()).run());
    }

}
