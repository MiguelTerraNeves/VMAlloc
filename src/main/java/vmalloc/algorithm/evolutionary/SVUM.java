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

package vmalloc.algorithm.evolutionary;

import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.operator.real.UM;
import org.moeaframework.core.variable.RealVariable;

/**
 * SVUM stands for Single-Value Uniform Mutation, and is a mutation operator not implemented in the MOEA
 * Framework (version 2.9). Within a given probability, the operator decides whether to mutate or not a given
 * solution. If so, it selects a random variable in the solution and performs a uniform mutation on that
 * variable.
 * @author Miguel Terra-Neves
 */
public class SVUM extends UM {

    /**
     * Creates an instance of the SVUM mutation operator that mutates a solution with a given probability.
     * @param probability The probability of mutation.
     */
    public SVUM(double probability) {
        super(probability);
    }
    
    /**
     * Applies single-value uniform mutation to a solution with the probability given when the operator was
     * instantiated.
     * @param parents An array of solutions. Only the first one is considered.
     * @return An array with the mutated (or not) solution in its first position.
     * @see SVUM#SVUM(double)
     */
    @Override
    public Solution[] evolve(Solution[] parents) {
        if (PRNG.nextDouble() <= getProbability()) {
            Solution result = parents[0].copy();
            Variable variable = result.getVariable(PRNG.nextInt(result.getNumberOfVariables()));
            if (variable instanceof RealVariable) {
                evolve((RealVariable)variable);
            }
            return new Solution[] { result };
        }
        return parents;
    }

}
