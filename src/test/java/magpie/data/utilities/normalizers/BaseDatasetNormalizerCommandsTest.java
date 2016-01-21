package magpie.data.utilities.normalizers;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the command line functions for {@linkplain BaseDatasetNormalizer}.
 * @author Logan Ward
 */
public class BaseDatasetNormalizerCommandsTest {

    @Test
    public void testCommands() throws Exception {
        // Make a simple normalizer
        RescalingNormalizer norm = new RescalingNormalizer();
        
        // Create a dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x", new double[]{0,1,2});
        data.addAttribute("y", new double[]{2,1,0});
        
        data.setMeasuredClasses(new double[]{0,4,5});
        data.setPredictedClasses(new double[]{-1,3,5});
        
        // Test training
        List<Object> command = new ArrayList<>();
        
        command.add("train");
        command.add("attributes");
        command.add("class");
        command.add(data);
        
        norm.runCommand(command);
        
        // Check results
        assertTrue(norm.isTrained());
        assertTrue(norm.willNormalizeClass());
        assertTrue(norm.willNormalizeAttributes());
        
        // Make sure it throws an exception if neither attributes nor class were passed
        boolean hitExcept = false;
        
        command.remove(1);
        command.remove(1);
        try {
            norm.runCommand(command);
        } catch (Exception e) {
            hitExcept = true;
            assertTrue(e.getMessage().contains("Must train attributes or class."));
        }
        
        assertTrue(hitExcept);
        
        // Test run
        command.clear();
        command.add("normalize");
        command.add(data);
        
        norm.runCommand(command);
        
        //    Check results
        assertArrayEquals(new double[]{-1,0,1}, data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{1,0,-1}, data.getSingleAttributeArray(1), 1e-6);
        assertArrayEquals(new double[]{-1,0.6,1}, data.getMeasuredClassArray(), 1e-6);
        assertArrayEquals(new double[]{-1.4,0.2,1}, data.getPredictedClassArray(), 1e-6);
        
        // Test restore
        command.clear();
        command.add("restore");
        command.add(data);
        
        norm.runCommand(command);
        
        //    Check results
        assertArrayEquals(new double[]{0,1,2}, data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{2,1,0}, data.getSingleAttributeArray(1), 1e-6);
        assertArrayEquals(new double[]{0,4,5}, data.getMeasuredClassArray(), 1e-6);
        assertArrayEquals(new double[]{-1,3,5}, data.getPredictedClassArray(), 1e-6);
        
        // Test test
        command.clear();
        command.add("test");
        command.add(data);
        
        norm.runCommand(command);
    }
    
}
