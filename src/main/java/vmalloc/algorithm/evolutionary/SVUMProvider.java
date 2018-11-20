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
 * Operator factory class for single-value uniform mutation ({@link SVUM}). Required in order to use the
 * operator.
 * @author Miguel Terra-Neves
 */
public class SVUMProvider extends OperatorProvider {

    @Override
    public String getMutationHint(Problem problem) { return null; }
    @Override
    public String getVariationHint(Problem problem) { return null; }

    /**
     * If the {@link SVUM} operator name ("svum") is given as input, produces an instance of it. Otherwise,
     * returns null.
     * @param name The operator name.
     * @param properties A set of configuration properties. Should contain SVUM's mutation rate, which is a
     * property with the key "svum.rate".
     * @param problem The problem instance.
     * @return An instance of the {@link SVUM} operator if provided with the name "svum", null otherwise.
     */
    @Override
    public Variation getVariation(String name, Properties properties, Problem problem) {
        if (name.equals("svum")) {
            TypedProperties typed_props = new TypedProperties(properties);
            return new SVUM(typed_props.getDouble("svum.rate", 0.05));
        }
        return null;
    }

}
