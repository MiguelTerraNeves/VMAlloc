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

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;

import org.moeaframework.algorithm.AbstractEvolutionaryAlgorithm;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Population;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.Variation;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.util.TypedProperties;

import vmalloc.algorithm.evolutionary.SVUM;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.utils.MathUtils;

/**
 * Virtual Machine Consolidation algorithm that relies on biogeography-based optimization. Refer to:
 * Zheng, Qinghua, et al. "Virtual machine consolidated placement based on multi-objective biogeography-based
 * optimization." Future Generation Computer Systems 54 (2016): 95-122.
 * @author Miguel Terra-Neves
 */
// TODO: allow for any kind of crossover operator
public class BBOAlloc extends EvolutionaryAllocAlgorithm {
    
    /**
     * Class that implements the biography-based optimization algorithm.
     * @author Miguel Terra-Neves
     */
    private class BBO extends AbstractEvolutionaryAlgorithm {
        
        /**
         * An array of populations (archipelagos). Each archipelago contains a set of solutions (islands).
         * Stores the population of the current generation.
         */
        private Population[] populations;
        
        /**
         * The intra-archipelago immigration rate.
         */
        private double immigration_rate;
        
        /**
         * The inter-archipelago immigration rate.
         */
        private double cross_immig_prob;
        
        /**
         * The mutation operator.
         */
        private Variation mutation;
        
        /**
         * Creates an instance of the biography-based optimization algorithm.
         * @param problem The problem instance.
         * @param populations The container for the population (archipelagos) of each generation.
         * @param immigration_rate The intra-archipelago immigration rate.
         * @param cross_immig_prob The inter-archipelago immigration rate.
         * @param mutation The mutation operator.
         * @param initialization The population initialization operator.
         */
        public BBO(Problem problem,
                   Population[] populations,
                   double immigration_rate,
                   double cross_immig_prob,
                   Variation mutation,
                   Initialization initialization) {
            super(problem, null, null, initialization);
            assert(immigration_rate >= 0.0 && immigration_rate <= 1.0);
            assert(cross_immig_prob >= 0.0 && cross_immig_prob <= 1.0);
            assert(mutation.getArity() == 1);
            this.populations = populations;
            this.immigration_rate = immigration_rate;
            this.cross_immig_prob = cross_immig_prob;
            this.mutation = mutation;
        }
        
        /**
         * Computes a mapping of solutions to respective constraint violation values for a given population.
         * @param population The population.
         * @return A mapping of solutions to contraint violation values.
         */
        private Map<Solution, Double> getConstraintViolations(Population population) {
            Map<Solution, Double> sol2violation = new IdentityHashMap<Solution, Double>();
            for (int i = 0; i < population.size(); ++i) {
                Solution sol = population.get(i);
                sol2violation.put(sol, new Double(VMCwMProblem.getConstraintViolation(sol)));
            }
            return sol2violation;
        }
        
        /**
         * Sorts the solutions in a population by rank. Solutions are compared in lexicographic order, first
         * by comparing constraint violation and then by the quality of the solution in respect to a given
         * objective function.
         * @param population The population.
         * @param obj_idx The index of the objective function.
         */
        private void sortByRank(Population population, final int obj_idx) {
            final Map<Solution, Double> sol2violation = getConstraintViolations(population);
            population.sort(new Comparator<Solution>() {
                public int compare(Solution sol1, Solution sol2) {
                    assert(sol2violation.containsKey(sol1) && sol2violation.containsKey(sol2));
                    int violation1 = sol2violation.get(sol1).intValue();
                    int violation2 = sol2violation.get(sol2).intValue();
                    int violation_diff = violation1 - violation2;
                    if (violation_diff == 0) {
                        double obj_diff = sol1.getObjective(obj_idx) - sol2.getObjective(obj_idx);
                        if (obj_diff > 0.0) {
                            return 1;
                        }
                        else if (obj_diff < 0.0) {
                            return -1;
                        }
                        return 0;
                    }
                    return violation_diff;
                }
            });
        }
        
        /**
         * Sorts a given population by rank in respect to a given objective function and then returns the
         * respective ranks.
         * @param population The population.
         * @param obj_idx The objective function.
         * @return An array with the ranks of the population's individuals.
         */
        private int[] rankPopulation(Population population, final int obj_idx) {
            sortByRank(population, obj_idx);
            int[] ranks = new int[population.size()];
            for (int i = 0; i < population.size(); ++i) {
                ranks[i] = i+1;
            }
            return ranks;
        }
        
        /**
         * Computes the immigration rate for a given rank and population size.
         * @param rank The rank.
         * @param pop_size The population size.
         * @return The immigration rate.
         */
        private double immigrationRate(int rank, int pop_size) {
            return ((double)rank*(rank+1)) / ((double)pop_size*(pop_size+1));
        }
        
        /**
         * Computes the immigration rates for a given set of ranks and population size.
         * @param ranks The ranks.
         * @param pop_size The population size.
         * @return An array with the immigration rates for each respective rank.
         */
        private double[] computeImmigrationRates(int[] ranks, int pop_size) {
            double[] rates = new double[ranks.length];
            for (int i = 0; i < ranks.length; ++i) {
                rates[i] = immigrationRate(ranks[i], pop_size);
            }
            return rates;
        }
        
        /**
         * Computes the emigration rate from an immigration rate.
         * @param rate The immigration rate.
         * @return The emigration rate.
         */
        private double immigrationToEmmigrationRate(double rate) {
            return 1.0 - rate;
        }
        
        /**
         * Computes the emigration rates for a given set of ranks and population size.
         * @param ranks The ranks.
         * @param pop_size The population size.
         * @return The emigration rates.
         */
        private double[] computeEmmigrationRates(int[] ranks, int pop_size) {
            double[] rates = computeImmigrationRates(ranks, pop_size);
            for (int i = 0; i < rates.length; ++i) {
                rates[i] = immigrationToEmmigrationRate(rates[i]);
            }
            return rates;
        }
        
        /**
         * Scales the values in an array of doubles such that all values are in the range [0, 1] after
         * scaling.
         * @param array The array of doubles.
         */
        private void scale(double[] array) {
            double min = MathUtils.min(array), max = MathUtils.max(array), max_min_diff = max-min;
            for (int i = 0; i < array.length; ++i) {
                array[i] = (array[i] - min) / max_min_diff;
            }
        }
        
        /**
         * Performs roulette wheel selection from a population based on a given set of probabilities. It is
         * assumed that the sum of the probabilities is 1.
         * @param population The population.
         * @param probabilities The probabilities.
         * @return The solution selected by roulette wheel.
         */
        private Solution rouletteWheelSelection(Population population, double[] probabilities) {
            return population.get(MathUtils.rouletteWheelSelection(probabilities));
        }
        
        /**
         * Performs migration from an emigrating solution to an immigrating solution.
         * @param immig The immigration solution.
         * @param emmig The emigrating solution.
         * @return The offspring solution that results from the migration.
         */
        private Solution migrate(Solution immig, Solution emmig) {
            Solution new_sol = immig.copy();
            for (int i = 0; i < immig.getNumberOfVariables(); ++i) {
                if (PRNG.nextDouble() <= immigration_rate) {
                    Variable emmig_var = emmig.getVariable(PRNG.nextInt(emmig.getNumberOfVariables()));
                    new_sol.setVariable(i, emmig_var);
                }
                else {
                    new_sol.setVariable(i, immig.getVariable(i));
                }
            }
            return new_sol;
        }
        
        /**
         * Computes the euclidean distance between two solutions.
         * @param sol1 A solution.
         * @param sol2 Another solution.
         * @return The euclidean distance between both solutions.
         */
        private double euclideanDistance(Solution sol1, Solution sol2) {
            assert(sol1.getNumberOfVariables() == sol2.getNumberOfVariables());
            double distance = 0.0, npms = instance.getPhysicalMachines().size();
            int[] x1 = instance.getVirtualMachineAssignment(sol1);
            int[] x2 = instance.getVirtualMachineAssignment(sol2);
            for (int i = 0; i < sol1.getNumberOfVariables(); ++i) {
                double var_diff = (x1[i]-x2[i]) / npms;
                distance += var_diff * var_diff;
            }
            return Math.sqrt(distance);
        }
        
        /**
         * Computes the distances from a solution to the individuals of a population.
         * @param sol The solution.
         * @param pop The population.
         * @return An array with the distances from {@code sol} to each element in {@code pop}.
         */
        private double[] computeDistances(Solution sol, Population pop) {
            double[] distances = new double[pop.size()];
            for (int i = 0; i < pop.size(); ++i) {
                distances[i] = euclideanDistance(sol, pop.get(i));
            }
            return distances;
        }
        
        /**
         * Checks if an array only contains 0s.
         * @param array The array.
         * @return True if {@code array} contains all 0s, false otherwise.
         */
        private boolean allZeros(double[] array) {
            for (int i = 0; i < array.length; ++i) {
                if (array[i] != 0.0) {
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Performs inter-archipelago migration from an emigrating population to an immigrating population.
         * @param immig_pop The immigrating population.
         * @param emmig_pop The emigrating population.
         * @return The new population to replace the immigrating population after inter-archipelago migration.
         */
        private Population crossMigration(Population immig_pop, Population emmig_pop) {
            assert(immig_pop.size() == emmig_pop.size());
            Population offspring = new Population();
            for (int i = 0; i < immig_pop.size(); ++i) {
                Solution immig = immig_pop.get(i);
                if (PRNG.nextDouble() <= cross_immig_prob) {
                    double[] distances = computeDistances(immig, emmig_pop);
                    if (allZeros(distances)) {
                        offspring.add(immig);
                    }
                    else {
                        distances = MathUtils.normalize(distances);
                        Solution emmig = rouletteWheelSelection(emmig_pop, distances);
                        offspring.add(migrate(immig, emmig));
                    }
                }
                else {
                    offspring.add(immig);
                }
            }
            return offspring;
        }
        
        /**
         * Checks if a solution is in a population.
         * @param sol The solution.
         * @param population The population.
         * @return True if {@code sol} is in {@code population}.
         */
        private boolean solutionInPopulation(Solution sol, Population population) {
            boolean result = false;
            for (int i = 0; i < population.size() && !result; ++i) {
                result = solutionEquals(sol, population.get(i));
            }
            return result;
        }
        
        /**
         * Initializes the population with the initialization operator provided in {@link #BBO}.
         */
        @Override
        protected void initialize() {
            initialized = true;
            for (int i = 0; i < populations.length; ++i) {
                Solution[] initial_solutions = initialization.initialize();
                evaluateAll(initial_solutions);
                populations[i].addAll(initial_solutions);
            }
        }

        /**
         * Performs an iteration of the biogeography-based algorithm. Replaces the current population with a
         * new population for the next generation.
         */
        @Override
        protected void iterate() {
            int n_objectives = problem.getNumberOfObjectives(), n_sub_systems = populations.length;
            Population[] offsprings = new Population[n_sub_systems];
            for (int i = 0; i < n_sub_systems; ++i) {
                offsprings[i] = new Population();
            }
            Solution[] elites = new Solution[n_sub_systems];
            // Perform within-subsystem migration
            for (int i = 0; i < n_sub_systems; ++i) {
                Population population = populations[i];
                Population offspring = offsprings[i];
                int[] ranks = rankPopulation(population, i % n_objectives);
                int best_rank = ranks[0];
                elites[i] = population.get(0);
                double[] immig_rates = computeImmigrationRates(ranks, population.size());
                scale(immig_rates);
                for (int j = 0; j < population.size(); ++j) {
                    Solution sol = population.get(j);
                    double immig_rate = immig_rates[j];
                    if (PRNG.nextDouble() <= immig_rate) {
                        double[] emmig_rates = computeEmmigrationRates(ranks, population.size());
                        emmig_rates[j] = 0.0;
                        emmig_rates = MathUtils.normalize(emmig_rates);
                        Solution emmig_sol = rouletteWheelSelection(population, emmig_rates);
                        offspring.add(migrate(sol, emmig_sol));
                    }
                    else {
                        offspring.add(sol);
                    }
                    if (ranks[j] < best_rank) {
                        best_rank = ranks[j];
                        elites[i] = sol;
                    }
                }
            }
            // Perform cross-subsystem migration
            for (int i = 0; i < n_sub_systems; ++i) {
                for (int j = i+1; j < n_sub_systems; ++j) {
                    if (    i % n_objectives != j % n_objectives && // only do cross migration for different objectives
                            PRNG.nextDouble() <= 1 / (n_sub_systems - n_sub_systems%n_objectives)) {
                        Population new_i_offspring = crossMigration(offsprings[i], offsprings[j]);
                        Population new_j_offspring = crossMigration(offsprings[j], offsprings[i]);
                        offsprings[i] = new_i_offspring;
                        offsprings[j] = new_j_offspring;
                    }
                }
            }
            // Perform mutation
            for (int i = 0; i < n_sub_systems; ++i) {
                Population offspring = offsprings[i];
                for (int j = 0; j < offspring.size(); ++j) {
                    Solution[] parent = { offspring.get(j) };
                    offspring.replace(j, mutation.evolve(parent)[0]);
                }
            }
            // Perform elitism
            for (int i = 0; i < n_sub_systems; ++i) {
                Population offspring = offsprings[i];
                Solution elite = elites[i];
                if (!solutionInPopulation(elite, offspring)) {
                    evaluateAll(offspring);
                    offspring.add(elite);
                    sortByRank(offspring, i % n_objectives);
                    offspring.remove(offspring.size()-1);
                }
            }
            populations = offsprings;
        }
        
        /**
         * Retrieves the current population.
         * @return The current population.
         */
        @Override
        public Population getPopulation() {
            Population population = new Population();
            for (int i = 0; i < populations.length; ++i) {
                Population system_pop = populations[i];
                for (int j = 0; j < system_pop.size(); ++j) {
                    population.add(system_pop.get(j));
                }
            }
            return population;
        }
        
        /**
         * Retrieves the non-dominated solutions in the current population.
         * @return The non-dominated population.
         */
        @Override
        public NondominatedPopulation getResult() {
            return new NondominatedPopulation(getPopulation());
        }
        
    }
    
    /**
     * Factory class for the Virtual Machine Consolidation algorithm that uses biogeography-based
     * optimization. Required in order to instantiate the algorithm.
     * @author Miguel Terra-Neves
     */
    private class BBOAllocAlgorithmProvider extends AlgorithmProvider {
        
        /**
         * If the name of the biogeography-based optimization algorithm ("BBO") is given as input,
         * produces an instance of it for Virtual Machine Consolidation. Otherwise, returns null.
         * @param name The name of the algorithm.
         * @param properties A set of configuration properties, such as population size per archipelago
         * ("populationSize"), immigration rate ("immigrationRate"), single value uniform mutation rate
         * ("svum.rate"), number of archipelagoes ("subSystems") and inter-archipelago migration rate
         * ("crossMigrationRate").
         * @param problem The problem instance.
         * @return An instance of the biogeography-based optimization algorithm if provided with the name
         * "BBO", null otherwise.
         */
        @Override
        public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
            if (name.equals("BBO")) {
                TypedProperties typed_props = new TypedProperties(properties);
                int pop_size = typed_props.getInt("populationSize", 3);
                int n_sub_systems = typed_props.getInt("subSystems", 2*problem.getNumberOfObjectives());
                Population[] populations = new Population[n_sub_systems];
                for (int i = 0; i < n_sub_systems; ++i) {
                    populations[i] = new Population();
                }
                Initialization initialization = makeInitializer(problem, pop_size);
                double immig_rate = typed_props.getDouble("immigrationRate", 0.5);
                double cross_mig_rate = typed_props.getDouble("crossMigrationRate", 0.5);
                Variation mutation =
                        OperatorFactory.getInstance().getVariation("svum", properties, problem);
                return decorateWithPeriodicActions(
                        new BBO(problem,
                                populations,
                                immig_rate,
                                cross_mig_rate,
                                mutation,
                                initialization));
            }
            return null;
        }
        
    }

    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on biogeography-based
     * optimization.
     * @param instance The problem instance.
     */
    public BBOAlloc(VMCwMProblem instance) {
        super(instance, "BBO", VMCwMProblem.Encoding.INTEGER);
        AlgorithmFactory.getInstance().addProvider(new BBOAllocAlgorithmProvider());
    }
    
    /**
     * Sets the immigration rate to be used by the algorithm.
     * @param rate The immigration rate.
     */
    public void setImmigrationRate(double rate) { exec = exec.withProperty("immigrationRate", rate); }
    
    /**
     * Sets the single value uniform mutation rate to be used by the algorithm.
     * @param rate The mutation rate.
     * @see SVUM
     */
    public void setMutationRate(double rate) { exec = exec.withProperty("svum.rate", rate); }
    
    /**
     * Sets the number of archipelagoes to be used by the algorithm.
     * @param n The number of archipelagoes.
     */
    public void setSubSystems(int n) { exec = exec.withProperty("subSystems", n); }
    
    /**
     * Sets the inter-archipelago migration rate to be used by the algorithm.
     * @param rate The inter-archipelago migration rate.
     */
    public void setCrossSystemMigrationRate(double rate) {
        exec = exec.withProperty("crossMigrationRate", rate);
    }

}
