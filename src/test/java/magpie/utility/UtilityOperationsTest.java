package magpie.utility;

import org.json.JSONArray;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class UtilityOperationsTest {
    
    @Test
    public void testIsInteger() {
        assertTrue(UtilityOperations.isInteger("1"));
        assertTrue(UtilityOperations.isInteger("-1500123"));
        assertFalse(UtilityOperations.isInteger("-1.0"));
        assertFalse(UtilityOperations.isInteger("1.0"));
        assertFalse(UtilityOperations.isInteger("word"));
    }
    
    @Test
    public void testFindFile() {
        File file = UtilityOperations.findFile("py/lasso_attribute_selection.py");
        assertTrue(file.exists());
        
        file = UtilityOperations.findFile("asdfl;kjasdfaer.asdf");
        assertNull(file);// Unless I'm really unlucky
    }
    
    @Test
    public void testPartition() {
        List<Integer> toSplit = new ArrayList<>();
        for (int i=0; i<100; i++) {
            toSplit.add(i);
        }
        
        // Split into two chunks
        List<List<Integer>> output = UtilityOperations.partitionList(toSplit, 2);
        assertEquals(2, output.size());
        assertEquals(50, output.get(1).size());
        assertEquals(100, output.get(0).size() + output.get(1).size());
        assertFalse(output.get(0).removeAll(output.get(1)));
        
        // Split into three chunks
        output = UtilityOperations.partitionList(toSplit, 3);
        assertEquals(3, output.size());
        assertEquals(100, output.get(0).size() 
                + output.get(1).size() + output.get(2).size());
    }

    @Test
    public void toJSONTest() {
        double[] testArray = new double[]{0, 1.5, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};

        JSONArray output = UtilityOperations.toJSONArray(testArray);

        assertEquals(output.getDouble(0), 0, 1e-6);
        assertEquals(output.getDouble(1), 1.5, 1e-6);
        assertEquals("NaN", output.getString(2));
        assertEquals("inf", output.getString(3));
        assertEquals("-inf", output.getString(4));
    }

    @Test
    public void timePrinterTest() {
        assertEquals("0 days, 00 hours, 00 minutes, 00.000 seconds", UtilityOperations.millisecondsToString(0));
        assertEquals("0 days, 00 hours, 00 minutes, 00.400 seconds", UtilityOperations.millisecondsToString(400));
        assertEquals("0 days, 00 hours, 01 minutes, 00.025 seconds", UtilityOperations.millisecondsToString(60 * 1000 + 25));
        assertEquals("4 days, 18 hours, 23 minutes, 58.022 seconds",
                UtilityOperations.millisecondsToString(TimeUnit.DAYS.toMillis(4)
                        + TimeUnit.HOURS.toMillis(18) + TimeUnit.MINUTES.toMillis(23)
                        + 58 * 1000 + 22));
    }
    
}
