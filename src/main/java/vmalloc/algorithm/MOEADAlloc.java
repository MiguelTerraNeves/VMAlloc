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

import org.moeaframework.algorithm.MOEAD;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.util.TypedProperties;

import vmalloc.algorithm.evolutionary.SVUM;
import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on the MOEAD evolutionary algorithm. Refer to:
 * Zhang, Qingfu, and Hui Li. "MOEA/D: A multiobjective evolutionary algorithm based on decomposition." IEEE
 * Transactions on evolutionary computation 11.6 (2007): 712-731.
 * @author Miguel Terra-Neves
 */
public class MOEADAlloc extends EvolutionaryAllocAlgorithm {
    
    /**
     * Factory class for the Virtual Machine Consolidation algorithm based on the MOEAD evolutionary
     * algorithm. Required in order to instantiate the algorithm.
     * @author Miguel Terra-Neves
     */
    private class MOEADAllocAlgorithmProvider extends AlgorithmProvider {

        /**
         * If the name of the MOEAD algorithm ("MOEAD") is given as input, produces an instance of it for
         * Virtual Machine Consolidation. Otherwise, returns null.
         * @param name The name of the algorithm.
         * @param properties A set of configuration properties, such as population size ("populationSize"),
         * uniform crossover rate ("ux.crossoverRate"), single value uniform mutation rate ("svum.rate"),
         * maximum portion of the population that can be replaced by a new solution ("eta"), neighborhood
         * size ("neighborhoodSize") and probability of performing crossover within the neighborhood of a
         * solution ("delta").
         * @param problem The problem instance.
         * @return An instance of the MOEAD algorithm if provided with the name "MOEAD", null otherwise.
         */
        @Override
        public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
            if (name.equals("MOEAD")) {
                TypedProperties typed_props = new TypedProperties(properties);
                int pop_size =
                        typed_props.getInt("populationSize", Math.max(100, problem.getNumberOfObjectives()));
                Initialization init = makeInitializer(problem, pop_size);
                Variation variation = OperatorFactory.getInstance().getVariation("ux+svum", properties, problem);
                int neighbordhoodSize = (int)(typed_props.getDouble("neighborhoodSize", 0.1) * pop_size);
                neighbordhoodSize = Math.max(2, Math.min(pop_size, neighbordhoodSize));
                int eta = Math.max(2, (int)(typed_props.getDouble("eta", 0.01) * pop_size));
                double delta = typed_props.getDouble("delta", 0.9);
                return new MOEAD(problem, neighbordhoodSize, init, variation, delta, eta);
            }
            return null;
        }
        
    }
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on the MOEAD
     * evolutionary algorithm.
     * @param instance The problem instance.
     */
    public MOEADAlloc(VMCwMProblem instance) {
        super(instance, "MOEAD", VMCwMProblem.Encoding.INTEGER);
        AlgorithmFactory.getInstance().addProvider(new MOEADAllocAlgorithmProvider());
    }
    
    @Override
    public void setTimeout(long timeout) {
        super.setTimeout(timeout);
        exec = exec.withProperty("sm.timeout", getTimeout());
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
    
    /**
     * Sets the neighborhood size as a fraction of the population size.
     * @param f The fraction of the population size.
     */
    public void setNeighborhoodSize(double f) { exec = exec.withProperty("neighborhoodSize", f); }
    
    /**
     * Sets the probability of performing crossover with an individual of the neighborhood.
     * @param delta The neighborhood crossover probability.
     */
    public void setDelta(double delta) { exec = exec.withProperty("delta", delta); }
    
    /**
     * Sets the maximum portion of the population that can be replaced by a new solution as a fraction of the
     * population size.
     * @param eta The fraction of the population size.
     */
    public void setEta(double eta) { exec = exec.withProperty("eta", eta); }

}
