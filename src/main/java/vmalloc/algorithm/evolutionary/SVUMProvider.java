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
