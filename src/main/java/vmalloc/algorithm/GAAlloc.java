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
                Variation variation = OperatorFactory.getInstance().getVariation("ux+svum", properties, problem);
                return new NSGAII(problem, population, null, selection, variation, initialization);
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
    public void setCrossoverRate(double rate) { exec = exec.withProperty("ux.rate", rate); }
    
    /**
     * Sets the single value uniform mutation rate to be used by the algorithm.
     * @param rate The mutation rate.
     * @see SVUM
     */
    public void setMutationRate(double rate) { exec = exec.withProperty("svum.rate", rate); }

}
