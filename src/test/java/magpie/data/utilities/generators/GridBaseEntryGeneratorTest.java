package magpie.data.utilities.generators;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class GridBaseEntryGeneratorTest {
    
    @Test
    public void test() throws Exception {
        // Create generator
        GridBaseEntryGenerator gen = new GridBaseEntryGenerator();
        
        // Set options
        List<Object> opts = new LinkedList<>();
        opts.add(3);
        opts.add(-1);
        opts.add(0.5);
        opts.add(1);
        
        System.out.println(gen.printUsage());
        
        gen.setOptions(opts);
        
        // Generate entries
        Dataset data = new Dataset();
        data.addAttributes(Arrays.asList(new String[]{"x","y","z"}));
        gen.addEntriesToDataset(data);
        
        // Test results
        assertEquals(125, data.NEntries());
        data.removeDuplicates();
        assertEquals(125, data.NEntries());
        for (int d=0; d<3; d++) {
            double[] attrs = data.getSingleAttributeArray(d);
            
            assertEquals(-1, StatUtils.min(attrs), 1e-6);
            assertEquals(1, StatUtils.max(attrs), 1e-6);
            assertEquals(0, StatUtils.mean(attrs), 1e-6);
        }
    }
    
}
