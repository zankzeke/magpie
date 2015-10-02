package magpie.optimization.algorithms;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Operations that can be useful during optimization.
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class OptimizationHelper {
    
    /**
     * Sort an array and return the original indices of the each value. 
     * @param x Array to be sorted
     * @param descending Whether to sort the list in descending order
     * @return List of indices, sorted in same order as x
     */
    public static int[] sortAndGetRanks(double[] x, boolean descending) {
        // Put each entry in a sorted set
        final boolean desFinal = descending;
        Comparator<Double> comp = new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return desFinal ? o2.compareTo(o1) : o1.compareTo(o2);
            }
        };
        
        return sortAndGetRanks(x, comp);
    }
    
    /**
     * Sort an array and return the original index of each member. After 
     * operation, x will be sorted.
     * @param x Array to be sorted
     * @param comp How to compare entries in the array
     * @return Original index of each point
     */
    public static int[] sortAndGetRanks(double[] x, Comparator<Double> comp) {
        // Initialize the output array
        Integer[] output = new Integer[x.length];
        for (int i=0; i<x.length; i++) {
            output[i] = i;
        }
        
        // Create a copy of x that won't be sorted
        final double[] xOriginal = x.clone();
        
        // Make a comparator for the indicies
        final Comparator<Double> compFinal = comp;
        Comparator<Integer> indexComp = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return compFinal.compare(xOriginal[o1], xOriginal[o2]);
            }
        };
        Arrays.sort(output, indexComp);
        
        // Sort x and return list
        for (int i=0; i<output.length; i++) {
            x[i] = xOriginal[output[i]];
        }
        return ArrayUtils.toPrimitive(output);
    }
}
