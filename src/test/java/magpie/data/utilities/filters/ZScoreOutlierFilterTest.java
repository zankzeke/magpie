package magpie.data.utilities.filters;

import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ZScoreOutlierFilterTest {

    @Test
    public void test() throws Exception {
        // Create fake dataset
        Dataset data = new Dataset();
        for (int i=0; i<11; i++) {
            data.addEntry(new BaseEntry());
        }
        data.addAttribute("x", new double[]{0,0.1,0.2,-0.1,0.05,-0.05,0.3,0.4,1,0,0.15});
        data.addAttribute("y", new double[]{1,0.1,0.2,-0.1,0.05,-0.05,0.3,0.4,0,0,0.15});
        data.addAttribute("z", new double[]{1,1,1,1,1,1,1,1,1,1,1});
        
        data.setMeasuredClasses(new double[]{-0.5,2,0.2,-0.1,0.05,-0.05,0.3,0.4,0,0,0.15});
        
        // Generate filter
        ZScoreOutlierFilter filter = new ZScoreOutlierFilter();
        
        System.out.println(filter.printUsage());
        
        List<Object> command = new LinkedList<>();
        command.add(1);
        command.add("-class");
        command.add("-attributes");
        
        filter.setOptions(command);
        
        // Test results
        filter.train(data);
        boolean[] labels = filter.label(data);
        assertTrue(labels[0]);
        assertTrue(labels[1]);
        assertFalse(labels[2]);
        assertFalse(labels[3]);
        assertFalse(labels[4]);
        assertFalse(labels[5]);
        assertFalse(labels[6]);
        assertFalse(labels[7]);
        assertTrue(labels[8]);
        assertFalse(labels[9]);
        assertFalse(labels[10]);
        
        // Change tolerance to 2.9
        command.set(0, 2.7);
        filter = new ZScoreOutlierFilter();
        filter.setOptions(command);
        
        // Test results
        filter.train(data);
        labels = filter.label(data);
        assertFalse(labels[0]);
        assertTrue(labels[1]);
        assertFalse(labels[2]);
        assertFalse(labels[3]);
        assertFalse(labels[4]);
        assertFalse(labels[5]);
        assertFalse(labels[6]);
        assertFalse(labels[7]);
        assertFalse(labels[8]);
        assertFalse(labels[9]);
        assertFalse(labels[10]);
        
        // Test only attributes
        command.remove("-class");
        filter.setOptions(command);
        
        // Test results
        filter.train(data);
        labels = filter.label(data);
        assertFalse(labels[0]);
        assertFalse(labels[1]);
        assertFalse(labels[2]);
        assertFalse(labels[3]);
        assertFalse(labels[4]);
        assertFalse(labels[5]);
        assertFalse(labels[6]);
        assertFalse(labels[7]);
        assertFalse(labels[8]);
        assertFalse(labels[9]);
        assertFalse(labels[10]);
        
        // Test only class
        command.remove("-attributes");
        command.add("-class");
        filter = new ZScoreOutlierFilter();
        filter.setOptions(command);
        
        // Test results
        filter.train(data);
        labels = filter.label(data);
        assertFalse(labels[0]);
        assertTrue(labels[1]);
        assertFalse(labels[2]);
        assertFalse(labels[3]);
        assertFalse(labels[4]);
        assertFalse(labels[5]);
        assertFalse(labels[6]);
        assertFalse(labels[7]);
        assertFalse(labels[8]);
        assertFalse(labels[9]);
        assertFalse(labels[10]);

    }
    
}
