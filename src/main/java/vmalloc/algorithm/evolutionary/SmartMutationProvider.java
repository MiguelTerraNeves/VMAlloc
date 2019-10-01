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

import java.util.Properties;

import org.moeaframework.core.Problem;
import org.moeaframework.core.Variation;
import org.moeaframework.core.spi.OperatorProvider;
import org.moeaframework.util.TypedProperties;

/**
 * Operator factory class for smart mutation ({@link SmartMutation}). Required in order to use the
 * operator.
 * @author Miguel Terra-Neves
 */
public class SmartMutationProvider extends OperatorProvider {

    @Override
    public String getMutationHint(Problem problem) { return null; }
    @Override
    public String getVariationHint(Problem problem) { return null; }
    
    /**
     * If the {@link SmartMutation} operator name ("sm") is given as input, produces an instance of it.
     * Otherwise, returns null.
     * @param name The operator name.
     * @param properties A set of configuration properties. Should contain smart mutation's configuration
     * parameters.
     * @param problem The problem instance.
     * @return An instance of the {@link SmartMutation} operator if provided with the name "sm", null
     * otherwise.
     */
    // FIXME: smart mutation won't work after the first seed if running with multiple seeds
    @Override
    public Variation getVariation(String name, Properties properties, Problem problem) {
        if (name.equals("sm")) {
            TypedProperties typed_props = new TypedProperties(properties);
            SmartMutation sm = new SmartMutation(typed_props.getDouble("sm.rate", 0.0),
                                                 typed_props.getBoolean("sm.domainUnfix", true),
                                                 typed_props.getBoolean("sm.improve", false),
                                                 problem);
            if (typed_props.contains("sm.maxConflicts")) {
                sm.setMaxConflicts(typed_props.getLong("sm.maxConflicts", Long.MAX_VALUE));
            }
            if (typed_props.contains("sm.improve.maxConflicts")) {
                sm.setImprovementMaxConflicts(typed_props.getLong("sm.improve.maxConflicts", Long.MAX_VALUE));
            }
            sm.setImprovementPartMaxConflicts(typed_props.getLong("sm.improve.partMaxConflicts", Long.MAX_VALUE));
            sm.setImprovementRelaxRate(typed_props.getDouble("sm.improve.relaxRate", 0.5));
            sm.setLitWeightRatio(typed_props.getDouble("sm.improve.lwr", 15.0));
            if (typed_props.contains("sm.timeout")) {
                sm.setTimeout(typed_props.getLong("sm.timeout", Long.MAX_VALUE));
            }
            return sm;
        }
        return null;
    }

}
