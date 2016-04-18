package magpie.optimization.rankers;

import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PropertyRankerTest {

    @Test
    public void test() throws Exception {
        // Make a test dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("x");
        data.addProperty("y");
        
        for (int i=0; i<3; i++) {
            data.addEntry(new MultiPropertyEntry());
        }
        
        data.addAttribute("X", new double[]{-1,-2,-3});
        
        data.setMeasuredClasses(new double[]{0,1,2});
        data.setTargetProperty(0, true);
        data.setMeasuredClasses(new double[]{2,0,1});
        data.setTargetProperty(1, true);
        data.setMeasuredClasses(new double[]{1,0,2});
        data.setPredictedClasses(new double[]{2,1,0});
        data.setTargetProperty(-1, true);
        
        // Make the ranker
        PropertyRanker ranker = new PropertyRanker();
        ranker.setUseMeasured(true);
        ranker.setMaximizeFunction(true);
        
        List<Object> options = new LinkedList<>();
        options.add("x");
        options.add("TargetEntryRanker");
        options.add(2.0);
        
        ranker.setOptions(options);
        System.out.println(ranker.printUsage());
        
        // Test that it catches users forgetting to train
        boolean threwException = false;
        try {
            ranker.objectiveFunction(data.getEntry(0));
        } catch (Exception e) {
            threwException = true;
        }
        assertTrue(threwException);
        
        // Train and test results
        ranker.train(data);
        assertEquals(0, ranker.objectiveFunction(data.getEntry(0)), 1e-6);
        assertArrayEquals(new int[]{1,2,0}, ranker.rankEntries(data));
        ranker.setMaximizeFunction(false);
        assertArrayEquals(new int[]{0,2,1}, ranker.rankEntries(data));
        
        // Make sure target properties did not change
        assertEquals(-1, data.getTargetPropertyIndex());
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(-1, ((MultiPropertyEntry) entry).getTargetProperty());
        }
        
        // Switch to proeprty y
        options.set(0, "y");
        options.set(2, 0);
        ranker.setOptions(options);
        
        // Make sure it reset the index
        threwException = false;
        try {
            ranker.objectiveFunction(data.getEntry(0));
        } catch (Exception e) {
            threwException = true;
        }
        assertTrue(threwException);
        
        // Train and test results
        ranker.train(data);
        assertEquals(1, ranker.objectiveFunction(data.getEntry(0)), 1e-6);
        assertArrayEquals(new int[]{1,0,2}, ranker.rankEntries(data));
        ranker.setUseMeasured(false);
        assertArrayEquals(new int[]{2,1,0}, ranker.rankEntries(data));
    }
    
}
