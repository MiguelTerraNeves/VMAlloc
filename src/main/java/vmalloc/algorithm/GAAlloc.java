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

import java.util.Properties;

import org.moeaframework.algorithm.NSGAII;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedSortingPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.comparator.ChainedComparator;
import org.moeaframework.core.comparator.CrowdingComparator;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.TournamentSelection;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.util.TypedProperties;

import vmalloc.algorithm.evolutionary.SVUM;
import vmalloc.algorithm.evolutionary.SmartMutation;
import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on the NSGAII genetic algorithm. Refer to:
 * Deb, Kalyanmoy, et al. "A fast and elitist multiobjective genetic algorithm: NSGA-II." IEEE transactions on
 * evolutionary computation 6.2 (2002): 182-197.
 * @author Miguel Terra-Neves
 */
public class GAAlloc extends EvolutionaryAllocAlgorithm {
    
    /**
     * Factory class for the Virtual Machine Consolidation algorithm based on the NSGAII genetic algorithm.
     * Required in order to instantiate the algorithm.
     * @author Miguel Terra-Neves
     */
    private class GAAllocAlgorithmProvider extends AlgorithmProvider {
        
        /**
         * If the name of the NSGAII algorithm ("NSGAII") is given as input, produces an instance of it for
         * Virtual Machine Consolidation. Otherwise, returns null.
         * @param name The name of the algorithm.
         * @param properties A set of configuration properties, such as population size ("populationSize"),
         * uniform crossover rate ("ux.crossoverRate") and single value uniform mutation rate ("svum.rate").
         * @param problem The problem instance.
         * @return An instance of the NSGAII algorithm if provided with the name "NSGAII", null otherwise.
         */
        @Override
        public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
            if (name.equals("NSGAII")) {
                TypedProperties typed_props = new TypedProperties(properties);
                int pop_size = typed_props.getInt("populationSize", 100);
                Initialization initialization = makeInitializer(problem, pop_size);
                DominanceComparator comparator = new ParetoDominanceComparator();
                NondominatedSortingPopulation population = new NondominatedSortingPopulation(comparator);
                TournamentSelection selection =
                        new TournamentSelection(2, new ChainedComparator(new ParetoDominanceComparator(),
                                                                         new CrowdingComparator()));
                Variation variation = OperatorFactory.getInstance().getVariation("ux+svum+sm", properties, problem);
                return decorateWithPeriodicActions(
                        new NSGAII(problem, population, null, selection, variation, initialization));
            }
            return null;
        }
        
    }

    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on the NSGAII
     * genetic algorithm.
     * @param instance The problem instance.
     */
    public GAAlloc(VMCwMProblem instance) {
        super(instance, "NSGAII", VMCwMProblem.Encoding.INTEGER);
        AlgorithmFactory.getInstance().addProvider(new GAAllocAlgorithmProvider());
    }

    /**
     * Sets the uniform crossover rate to be used by the algorithm.
     * @param rate The crossover rate.
     */
    public void setCrossoverRate(double rate) { this.exec = this.exec.withProperty("ux.rate", rate); }
    
    /**
     * Sets the single value uniform mutation rate to be used by the algorithm.
     * @param rate The mutation rate.
     * @see SVUM
     */
    public void setMutationRate(double rate) { this.exec = this.exec.withProperty("svum.rate", rate); }

    /**
     * Set the smart mutation rate to be used by the algorithm.
     * @param rate The smart mutation rate.
     * @see SmartMutation
     */
    public void setSmartMutationRate(double rate) { this.exec = this.exec.withProperty("sm.rate", rate); }
    
    /**
     * Set the maximum number of conflicts for smart mutation.
     * @param max_conflicts The maximum number of conflicts.
     * @see SmartMutation
     */
    public void setMaxConflicts(long max_conflicts) {
        this.exec = this.exec.withProperty("sm.maxConflicts", max_conflicts);
    }
    
    /**
     * Disables exploitation of domain knowledge when deciding which variables to be unassigned before
     * applying smart mutation.
     * @see SmartMutation
     */
    public void disableDomainBasedUnfixing() { this.exec = this.exec.withProperty("sm.domainUnfix", false); }

    /**
     * Enables application of smart improvement when a given solution is already feasible.
     * @see SmartMutation
     */
    public void enableSmartImprovement() { this.exec = this.exec.withProperty("sm.improve", true); }
    
    /**
     * Set the maximum number of conflicts for smart improvement.
     * @param max_conflicts The maximum number of conflicts.
     * @see SmartMutation
     */
    public void setImprovementMaxConflicts(long max_conflicts) {
        this.exec = this.exec.withProperty("sm.improve.maxConflicts", max_conflicts);
    }
    
    /**
     * Smart improvement applies the stratified Pareto-MCS algorithm. This method sets the number of
     * conflicts allowed before merging some partition with the next one.
     * @param max_conflicts The maximum number of conflicts before merging.
     * @see SmartMutation
     */
    public void setImprovementPartMaxConflicts(long max_conflicts) {
        this.exec = this.exec.withProperty("sm.improve.partMaxConflicts", max_conflicts);
    }
    
    /**
     * Set the fraction of servers to be displaced before applying smart improvement.
     * @param rate The displacement fraction.
     * @see SmartMutation
     */
    public void setImprovementRelaxRate(double rate) {
        this.exec = this.exec.withProperty("sm.improve.relaxRate", rate);
    }
    
    /**
     * Sets the literal-weight ratio for the stratified Pareto-MCS algorithm.
     * @param ratio The literal-weight ratio.
     * @see ParetoCLD
     */
    public void setLitWeightRatio(double ratio) { this.exec = this.exec.withProperty("sm.improve.lwr", ratio); }

}
