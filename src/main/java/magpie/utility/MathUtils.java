package magpie.utility;

import java.math.BigInteger;
import org.apache.commons.math3.stat.StatUtils;

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
}
