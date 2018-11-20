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
