package magpie.utility;

import java.math.BigInteger;
import java.util.Iterator;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Math operations that are simple and useful for many operations
 * @author Logan Ward
 */
public class MathUtils {
    /** 
     * Transpose a rectangular array
     * @param x Array to transpose
     * @return Transposed array
     */
    public static double[][] transpose(double[][] x) {
        double[][] y = new double[x[0].length][x.length];
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                y[j][i] = x[i][j];
            }
        }
        return y;
    }
    
    /**
     * Calculate the greatest common denominator of two integers.
     * @param a Integer #1
     * @param b Integer #1
     * @return gcd(a,b)
     */
    public static int gcd(int a, int b) {
        BigInteger A = BigInteger.valueOf(a);
        BigInteger B = BigInteger.valueOf(b);
        return A.gcd(B).intValue();
    }
    
    /**
     * Compute the mean absolute deviation from the mean. 
     * @param x List of values
     * @param mean Mean of x
     * @return Mean absolution deviation
     */
    public static double meanAbsoluteDeviation(double[] x, double mean) {
        double[] dev = new double[x.length];
        for (int i=0; i<x.length; i++) {
            dev[i] = Math.abs(x[i] - mean);
        }
        return StatUtils.mean(dev);
    }
    /**
     * Compute the mean absolute deviation from the mean. 
     * @param x List of values
     * @return Mean absolution deviation
     */
    public static double meanAbsoluteDeviation(double[] x) {
       return meanAbsoluteDeviation(x, StatUtils.mean(x));
    }
    
    /**
     * Iterator over all permutations of <i>k</i> choices of <i>n</i> integers.
     * 
     * @param n Size of subsets from which permutations are selected
     * @param k Number of entries to select
     * @return Iterators over all permutations, no order guaranteed
     */
    static public Iterator<int[]> permutationIterator(int n, int k) {
        // Create an interator over combintations
        final Iterator<int[]> combIterator = CombinatoricsUtils.combinationsIterator(n, k);
        
        // Initialize the permutation iterator
        final Iterator<int[]> permIterator = 
                DistinctPermutationGenerator.generatePermutations(combIterator.next()).iterator();
        
        // Create an iterator over permutations
        return new Iterator<int[]>() {
            Iterator<int[]> permIter = permIterator;

            @Override
            public boolean hasNext() {
                return combIterator.hasNext() || permIter.hasNext();
            }

            @Override
            public int[] next() {
                if (permIter.hasNext()) {
                    return permIter.next();
                } else {
                    // Refresh the permutation iterator
                    permIter = DistinctPermutationGenerator.generatePermutations(combIterator.next()).iterator();
                    return permIter.next();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
