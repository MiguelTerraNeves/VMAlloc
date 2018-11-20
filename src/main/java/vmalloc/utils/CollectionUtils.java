package vmalloc.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachineVec;

/**
 * Class with several static utility methods for operations on collections of objects.
 * @author Miguel Terra-Neves
 */
public class CollectionUtils {

    /**
     * Checks if an integer is contained in a vector of integers.
     * @param val The integer.
     * @param vec The vector.
     * @return True if {@code val} is contained in {@code vec}, false otherwise.
     */
    public static boolean valueInVec(int val, IVecInt vec) {
        boolean result = false;
        for (int i = 0; !result && i < vec.size(); ++i) {
            if (val == vec.get(i)) {
                result = true;
            }
        }
        return result;
    }
    
    /**
     * Checks if the intersection of two vectors of integers is not empty.
     * @param vec1 A vector.
     * @param vec2 Another vector.
     * @return True if at least one value in {@code vec1} is in {@code vec2}, false otherwise.
     */
    public static boolean valuesIntersect(IVecInt vec1, IVecInt vec2) {
        boolean result = false;
        for (int i = 0; !result && i < vec1.size(); ++i) {
            if (valueInVec(vec1.get(i), vec2)) {
                result = true;
            }
        }
        return result;
    }
    
    /**
     * Converts an array of integers to an array of ints.
     * @param array The array of integers.
     * @return The elements in {@code array} as ints, in order.
     */
    public static int[] unwrapIntegerArray(Integer[] array) {
        int[] int_array = new int[array.length];
        for (int i = 0; i < array.length; ++i) {
            int_array[i] = array[i].intValue();
        }
        return int_array;
    }
    
    /**
     * Converts an array of double wrapper objects to an array of doubles.
     * @param array The array of double wrapper objects.
     * @return The elements in {@code array} as doubles, in order.
     */
    // FIXME: identical to unwrapIntegerArray(Integer[] array)
    public static double[] unwrapDoubleArray(Double[] array) {
        double[] double_array = new double[array.length];
        for (int i = 0; i < array.length; ++i) {
            double_array[i] = array[i].doubleValue();
        }
        return double_array;
    }
    
    /**
     * Converts a vector of doubles to an array of doubles.
     * @param vec The vector of doubles.
     * @return The vector of doubles as an array.
     */
    public static Double[] doubleVecToArray(IVec<Double> vec) {
        Double[] array = new Double[vec.size()];
        vec.copyTo(array);
        return array;
    }
    
    /**
     * Converts an vector of double wrapper objects to an array of doubles.
     * @param vec The vector of double wrapper objects.
     * @return The elements in {@code vec} as doubles, in order.
     */
    public static double[] unwrapDoubleVec(IVec<Double> vec) {
        return unwrapDoubleArray(doubleVecToArray(vec));
    }
    
    /**
     * Converts an array of big integers to an array of longs.
     * @param array The array of big integers.
     * @return The elements in {@code array} as longs, in order.
     */
    public static long[] bigIntegerArrayToLongArray(BigInteger[] array) {
        long[] long_array = new long[array.length];
        for (int i = 0; i < array.length; ++i) {
            long_array[i] = array[i].longValue();
        }
        return long_array;
    }
    
    /**
     * Converts a vector of big integers to a vector of big decimals.
     * @param vec The vector of big integers.
     * @return The elements in {@code vec} as big decimals, in order.
     */
    public static IVec<BigDecimal> bigIntegerVecToBigDecimalVec(IVec<BigInteger> vec) {
        IVec<BigDecimal> dec_vec = new Vec<BigDecimal>();
        for (int i = 0; i < vec.size(); ++i) {
            dec_vec.push(new BigDecimal(vec.get(i)));
        }
        return dec_vec;
    }
    
    /**
     * Builds a map of physical machine IDs to indexes in a given vector.
     * @param pms The vector of physical machines.
     * @return A map of IDs to the corresponding indexes in {@code pms}.
     */
    public static Map<Integer, Integer> makePhysicalMachineIDtoIndexMap(PhysicalMachineVec pms) {
        Map<Integer, Integer> pm_id_to_idx = new HashMap<Integer, Integer>();
        for (int i = 0; i < pms.size(); ++i) {
            pm_id_to_idx.put(new Integer(pms.get(i).getID()), new Integer(i));
        }
        return pm_id_to_idx;
    }
    
    /**
     * Builds a map of virtual machine IDs to indexes in a given vector.
     * @param vms The vector of virtual machines.
     * @return A map of IDs to the corresponding indexes in {@code vms}.
     */
    public static Map<String, Integer> makeVirtualMachineIDtoIndexMap(VirtualMachineVec vms) {
        Map<String, Integer> vm_id_to_idx = new HashMap<String, Integer>();
        for (int i = 0; i < vms.size(); ++i) {
            vm_id_to_idx.put(vms.get(i).getID(), new Integer(i));
        }
        return vm_id_to_idx;
    }
    
    /**
     * Creates an array of {@link Integer} indexes, i.e., the array's i-th position contains the value 'i'.
     * @param size The size of the array.
     * @return The array of indexes as {@link Integer} objects.
     */
    private static Integer[] mkIndexArray(int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < size; ++i) {
            array[i] = i;
        }
        return array;
    }
    
    /**
     * A comparator that comparates vector indexes, in decreasing order, according to a given vector of big
     * decimals. Used in {@link CollectionUtils#getSortedDecreasingBigDecimalVecIndexes(IVec)}.
     * @author Admin
     */
    private static class BigDecimalVecIndexDecreasingComparator implements Comparator<Integer> {
        
        /**
         * The vector of big decimals used to comparate vector indexes.
         */
        private IVec<BigDecimal> vec = null;
        
        /**
         * Creates a big decimal vector index decreasing comparator.
         * @param vec The vector of big decimals.
         */
        public BigDecimalVecIndexDecreasingComparator(IVec<BigDecimal> vec) {
            this.vec = vec;
        }
        
        public int compare(Integer i1, Integer i2) {
            return -this.vec.get(i1).compareTo(this.vec.get(i2));
        }
        
    }
    
    /**
     * Computes an array of indexes sorted in decreasing order according to a given vector of big decimals.
     * More precisely, let {@code idxs} be the array returned by the method, we have that
     * {@code values.get(idxs[i]) >= values.get(idxs[i+1])}.
     * @param values The vector of big decimals.
     * @return The sorted array of indexes.
     */
    public static int[] getSortedDecreasingBigDecimalVecIndexes(IVec<BigDecimal> values) {
        Integer[] idxs = mkIndexArray(values.size());
        Arrays.sort(idxs, new BigDecimalVecIndexDecreasingComparator(values));
        return unwrapIntegerArray(idxs);
    }
    
    /**
     * Computes an array of indexes sorted in decreasing order according to a given vector of big integers.
     * More precisely, let {@code idxs} be the array returned by the method, we have that
     * {@code values.get(idxs[i]) >= values.get(idxs[i+1])}.
     * @param values The vector of big integers.
     * @return The sorted array of indexes.
     */
    public static int[] getSortedDecreasingBigIntegerVecIndexes(IVec<BigInteger> values) {
        return getSortedDecreasingBigDecimalVecIndexes(bigIntegerVecToBigDecimalVec(values));
    }
    
}
