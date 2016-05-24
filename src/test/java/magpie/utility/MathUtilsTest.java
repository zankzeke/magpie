package magpie.utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MathUtilsTest {
    
    @Test
    public void testTranspose() {
        double[][] x = new double[2][];
        x[0] = new double[]{1,2};
        x[1] = new double[]{3,4};
        
        double[][] xT = MathUtils.transpose(x);
        for (int i=0; i<x.length; i++) {
            for (int j=0; j<x[i].length; j++) {
                assertEquals(x[i][j], xT[j][i], 1e-5);
            }
        }
    }
    
    @Test
    public void testGCD() {
        assertEquals(4, MathUtils.gcd(4, 8));
        assertEquals(4, MathUtils.gcd(8, 4));
        assertEquals(1, MathUtils.gcd(31, 3));
    }
    
    @Test
    public void testMeanAbsoluteDeviation() {
        double[] x = new double[]{0,1,2};
        assertEquals(2.0/3.0, MathUtils.meanAbsoluteDeviation(x), 1e-6);
        x[0] = 1;
        assertEquals(4.0/9.0, MathUtils.meanAbsoluteDeviation(x), 1e-6);
        x[2] = 1;
        assertEquals(0, MathUtils.meanAbsoluteDeviation(x), 1e-6);
    }
    
    @Test
    public void testPermutations() {
        // Test 3 choose 2
        Set<String> perms = new TreeSet<>();
        Iterator<int[]> iter = MathUtils.permutationIterator(3, 2);
        while (iter.hasNext()) {
            perms.add(ArrayUtils.toString(iter.next()));
        }
        assertEquals(6, perms.size());
        assertTrue(perms.contains(ArrayUtils.toString(new int[]{0,1})));
        assertTrue(perms.contains(ArrayUtils.toString(new int[]{1,0})));
        assertTrue(perms.contains(ArrayUtils.toString(new int[]{0,2})));
        assertTrue(perms.contains(ArrayUtils.toString(new int[]{2,0})));
        assertTrue(perms.contains(ArrayUtils.toString(new int[]{1,2})));
        assertTrue(perms.contains(ArrayUtils.toString(new int[]{2,1})));
        
        // Bigger test
        for (int n=3; n<=5; n++) {
            for (int k=1; k<=n; k++) {
                perms.clear();
                iter = MathUtils.permutationIterator(n, k);
                while (iter.hasNext()) {
                    perms.add(ArrayUtils.toString(iter.next()));
                }
                assertEquals("Failed for: " + n + "," + k, 
                        CombinatoricsUtils.binomialCoefficient(n, k) * 
                        CombinatoricsUtils.factorial(k), perms.size());
            }
        }
    }
}
