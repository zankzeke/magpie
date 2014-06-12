/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.utility;

import java.math.BigInteger;

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
}
