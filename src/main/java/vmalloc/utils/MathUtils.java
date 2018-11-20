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

package vmalloc.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.moeaframework.core.PRNG;
import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;

/**
 * Class with several static utility methods for mathematical operations.
 * @author Miguel Terra-Neves
 */
public class MathUtils {

    /**
     * Normalizes a given percentile value, i.e., sets it to the closest value in the [0, 1] interval.
     * @param percentile The percentile.
     * @return The normalized percentile.
     */
    public static double normalizePercentile(double percentile) {
        if (percentile > 1.0) {
            percentile = 1.0;
        }
        else if (percentile < 0.0) {
            percentile = 0.0;
        }
        return percentile;
    }
    
    /**
     * Facilitates the division of two big integers.
     * @param int1 An integer.
     * @param int2 Another integer.
     * @param mode The rounding mode.
     * @return The result of {@code int1} divided by {@code int2}.
     */
    public static BigDecimal divideBigIntegers(BigInteger int1, BigInteger int2, RoundingMode mode) {
        return new BigDecimal(int1).divide(new BigDecimal(int2), 10, mode);
    }
    
    /**
     * Facilitates the division of two big decimals.
     * @param d1 A decimal.
     * @param d2 Another decimal.
     * @param mode The rounding mode.
     * @return The result of {@code d1} divided by {@code d2}.
     */
    public static BigDecimal divideBigDecimals(BigDecimal d1, BigDecimal d2, RoundingMode mode) {
        return d1.divide(d2, 10, mode);
    }
    
    /**
     * Computes the floor of a big decimal.
     * @param n The decimal.
     * @return The floor of {@code n}.
     */
    public static BigDecimal floor(BigDecimal n) {
        return n.setScale(0, RoundingMode.FLOOR);
    }
    
    /**
     * Computes the ceiling of a big decimal.
     * @param n The decimal.
     * @return The ceiling of {@code n}.
     */
    public static BigDecimal ceil(BigDecimal n) {
        return n.setScale(0, RoundingMode.CEILING);
    }
    
    /**
     * Divides two big integers, rounding up, and returns the result as a double.
     * @param q The numerator.
     * @param d The denominator.
     * @return {@code q} divided by {@code d} as a double.
     */
    public static double toPercentile(BigInteger q, BigInteger d) {
        return divideBigIntegers(q, d, RoundingMode.UP).doubleValue();
    }
    
    /**
     * Divides two big decimals, rounding up, and returns the result as a double.
     * @param q The numerator.
     * @param d The denominator.
     * @return {@code q} divided by {@code d} as a double.
     */
    public static double toPercentile(BigDecimal q, BigDecimal d) {
        return divideBigDecimals(q, d, RoundingMode.UP).doubleValue();
    }
    
    /**
     * Given a percentile computed under a given reference value, re-computes the percentile using another
     * reference value.
     * @param before_ref The previous reference value.
     * @param before_per The percentile.
     * @param new_ref The new reference value.
     * @return The percentile in respect to {@code new_ref}.
     */
    public static double scalePercentile(BigInteger before_ref, double before_per, BigInteger new_ref) {
        BigDecimal inv_k = new BigDecimal(before_per).multiply(new BigDecimal(before_ref));
        return divideBigDecimals(inv_k, new BigDecimal(new_ref), RoundingMode.DOWN).doubleValue();
    }
    
    /**
     * Retrieves the minimum in an array of doubles.
     * @param array The array.
     * @return The minimum value in {@code array}.
     */
    public static double min(double[] array) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < array.length; ++i) {
            min = (array[i] < min) ? array[i] : min;
        }
        return min;
    }
    
    /**
     * Retrieves the maximum in an array of doubles.
     * @param array The array.
     * @return The maximum value in {@code array}.
     */
    public static double max(double[] array) {
        double min = Double.MIN_VALUE;
        for (int i = 0; i < array.length; ++i) {
            min = (array[i] > min) ? array[i] : min;
        }
        return min;
    }
    
    /**
     * Retrieves the sum of an array of doubles.
     * @param array The array.
     * @return The sum of the values in {@code array}.
     */
    public static double sum(double[] array) {
        double sum = 0.0;
        for (int i = 0; i < array.length; ++i) {
            sum += array[i];
        }
        return sum;
    }
    
    /**
     * Retrieves the sum of an array of doubles.
     * @param array The array.
     * @return The sum of the values in {@code array}.
     */
    public static double sum(Double[] array) {
        return sum(CollectionUtils.unwrapDoubleArray(array));
    }
    
    /**
     * Retrieves the sum of a vector of doubles.
     * @param vec The vector.
     * @return The sum of the values in {@code vec}.
     */
    public static double sum(IVec<Double> vec) {
        return sum(vec.toArray());
    }
    
    /**
     * Retrieves the sum of an array of longs.
     * @param array The array.
     * @return The sum of the values in {@code array}.
     */
    // FIXME: identical to sum(double[])
    public static long sum(long[] array) {
        long sum = 0;
        for (int i = 0; i < array.length; ++i) {
            sum += array[i];
        }
        return sum;
    }
    
    /**
     * Calculates the average of the values in an array of doubles.
     * @param array The array.
     * @return The average of the values in {@code array}.
     */
    public static double avg(double[] array) {
        return sum(array) / array.length;
    }
    
    /**
     * Calculates the sum of the big integers in a vector.
     * @param vec The vector.
     * @return The sum of the values in {@code vec}.
     */
    public static BigInteger bigIntegerVecSum(IVec<BigInteger> vec) {
        BigInteger sum = BigInteger.ZERO;
        for (int i = 0; i < vec.size(); ++i) {
            sum = sum.add(vec.get(i));
        }
        return sum;
    }
    
    /**
     * Calculates the sum of the big decimals in a vector.
     * @param vec The vector.
     * @return The sum of the values in {@code vec}.
     */
    // FIXME: identical to bigIntegerVecSum
    public static BigDecimal bigDecimalVecSum(IVec<BigDecimal> vec) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < vec.size(); ++i) {
            sum = sum.add(vec.get(i));
        }
        return sum;
    }
    
    /**
     * Normalizes an array of doubles such that the sum of the values in the array is 1 after
     * normalization.
     * @param array The array of doubles.
     * @return The normalized array of doubles.
     */
    public static double[] normalize(double[] array) {
        double sum = 0.0;
        double[] norm_array = new double[array.length];
        for (int i = 0; i < array.length; ++i) {
            sum += array[i];
        }
        for (int i = 0; i < array.length; ++i) {
            norm_array[i] = array[i] / sum;
        }
        return norm_array;
    }
    
    /**
     * Performs roulette wheel selection based on a given array of probabilities. It is assumed that the sum
     * of the probabilities is 1.
     * @param probabilities The probabilities.
     * @return The index selected by roulette wheel.
     */
    public static int rouletteWheelSelection(double[] probabilities) {
        assert(sum(probabilities) == 1.0);
        double prob_sum = 0.0, roulette_val = PRNG.nextDouble();
        for (int i = 0; i < probabilities.length; ++i) {
            prob_sum += probabilities[i];
            if (roulette_val <= prob_sum) {
                return i;
            }
        }
        throw new RuntimeException("Unreachable code was reached");     // UNREACHABLE
    }
    
    /**
     * Performs roulette wheel selection based on a given array of probabilities. It is assumed that the sum
     * of the probabilities is 1.
     * @param probabilities The probabilities.
     * @return The index selected by roulette wheel.
     */
    public static int rouletteWheelSelection(Double[] probabilities) {
        return rouletteWheelSelection(CollectionUtils.unwrapDoubleArray(probabilities));
    }
    
    /**
     * Performs roulette wheel selection based on a given vector of probabilities. It is assumed that the sum
     * of the probabilities is 1.
     * @param probabilities The probabilities.
     * @return The index selected by roulette wheel.
     */
    public static int rouletteWheelSelection(IVec<Double> probabilities) {
        return rouletteWheelSelection(CollectionUtils.unwrapDoubleVec(probabilities));
    }
    
    /**
     * Given a set of possible non-integral values, converts those values to integers by applying a scaling
     * factor.
     * @param coeffs The values to scale.
     * @return The vector of scaled values.
     */
    public static IVec<BigInteger> scaleToInteger(IVec<BigDecimal> vals) {
        int factor = 0;
        for (int i = 0; i < vals.size(); ++i) {
            int scale = vals.get(i).scale();
            factor = (scale > factor) ? scale : factor;
        }
        IVec<BigInteger> scaled = new Vec<BigInteger>();
        for (int i = 0; i < vals.size(); ++i) {
            scaled.push(vals.get(i).scaleByPowerOfTen(factor).toBigIntegerExact());
        }
        return scaled;
    }
    
}
