package magpie.optimization.algorithms;

import java.util.Comparator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class OptimizationHelperTest {
    
    @Test
    public void test() {
        // Simple test: sort descending and then ascending again
        double[] x = new double[]{2,1,3};
        int[] res = OptimizationHelper.sortAndGetRanks(x, true);
        assertArrayEquals(new double[]{3,2,1}, x, 1e-6);
        assertArrayEquals(new int[]{2,0,1}, res);
        
        res = OptimizationHelper.sortAndGetRanks(x, false);
        assertArrayEquals(new double[]{1,2,3}, x, 1e-6);
        assertArrayEquals(new int[]{2,1,0}, res);
        
        // Complex: distance from 2.1
        res = OptimizationHelper.sortAndGetRanks(x, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return Double.compare(Math.abs(o1 - 2.1), Math.abs(o2 - 2.1));
            }
        });
        assertArrayEquals(new double[]{2,3,1}, x, 1e-6);
        assertArrayEquals(new int[]{1,2,0}, res);
    }
    
}
