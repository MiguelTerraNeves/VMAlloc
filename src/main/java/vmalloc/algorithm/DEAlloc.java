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

import org.moeaframework.algorithm.GDE3;
import org.moeaframework.core.Algorithm;
import org.moeaframework.core.Initialization;
import org.moeaframework.core.NondominatedSortingPopulation;
import org.moeaframework.core.Problem;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.operator.real.DifferentialEvolutionVariation;
import org.moeaframework.core.operator.real.DifferentialEvolutionSelection;
import org.moeaframework.core.spi.AlgorithmFactory;
import org.moeaframework.core.spi.AlgorithmProvider;
import org.moeaframework.core.spi.OperatorFactory;
import org.moeaframework.util.TypedProperties;

import vmalloc.algorithm.evolutionary.VMCwMProblem;

/**
 * Virtual Machine Consolidation algorithm that relies on generalized differential evolution. Refer to:
 * Kukkonen, Saku, and Jouni Lampinen. "GDE3: The third evolution step of generalized differential evolution."
 * Evolutionary Computation, 2005. The 2005 IEEE Congress on. Vol. 1. IEEE, 2005.
 * @author Miguel Terra-Neves
 */
public class DEAlloc extends EvolutionaryAllocAlgorithm {

    /**
     * Factory class for the Virtual Machine Consolidation algorithm based on differential evolution.
     * Required in order to instantiate the algorithm.
     * @author Miguel Terra-Neves
     */
    private class DEAllocAlgorithmProvider extends AlgorithmProvider {
        
        /**
         * If the name of the generalized differential evolution algorithm ("GDE3") is given as input,
         * produces an instance of it for Virtual Machine Consolidation. Otherwise, returns null.
         * @param name The name of the algorithm.
         * @param properties A set of configuration properties, such as population size ("populationSize"),
         * crossover rate ("de.crossoverRate") and step size ("de.stepSize").
         * @param problem The problem instance.
         * @return An instance of the generalized differential evolution algorithm if provided with the name
         * "GDE3", null otherwise.
         */
        @Override
        public Algorithm getAlgorithm(String name, Properties properties, Problem problem) {
            if (name.equals("GDE3")) {
                TypedProperties typed_props = new TypedProperties(properties);
                int pop_size = typed_props.getInt("populationSize", 100);
                DominanceComparator comparator = new ParetoDominanceComparator();
                NondominatedSortingPopulation population = new NondominatedSortingPopulation(comparator);
                Initialization initialization = makeInitializer(problem, pop_size);
                DifferentialEvolutionSelection selection =  new DifferentialEvolutionSelection();
                DifferentialEvolutionVariation variation = 
                        (DifferentialEvolutionVariation)OperatorFactory.getInstance().getVariation(
                                "de", properties, problem);
                return new GDE3(problem, population, comparator, selection, variation, initialization);
            }
            return null;
        }
        
    }
    
    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on differential
     * evolution.
     * @param instance The problem instance.
     */
    public DEAlloc(VMCwMProblem instance) {
        super(instance, "GDE3", VMCwMProblem.Encoding.INTEGER);
        AlgorithmFactory.getInstance().addProvider(new DEAllocAlgorithmProvider());
    }
    
    /**
     * Sets the crossover rate to be used by the algorithm.
     * @param rate The crossover rate.
     */
    public void setCrossoverRate(double rate) { exec = exec.withProperty("de.crossoverRate", rate); }
    
    /**
     * Sets the step size to be used by the algorithm.
     * @param rate The step size.
     */
    public void setStepSize(double step) { exec = exec.withProperty("de.stepSize", step); }

}
