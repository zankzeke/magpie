package magpie.data.utilities.filters;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionDistanceFilterTest {

    @Test
    public void test() throws Exception {
        // Make a sample dataset
        CompositionDataset toLabel = new CompositionDataset();
        toLabel.addEntry("Fe50Al50");
        toLabel.addEntry("Fe55Al45");
        toLabel.addEntry("Fe25Al75");
        
        // Create filter
        CompositionDistanceFilter filter = new CompositionDistanceFilter();
        
        List<Object> options = new ArrayList<>();
        options.add("Fe49Al51");
        options.add(1.1);
        filter.setOptions(options);
        
        // Test #1
        boolean[] res = filter.label(toLabel);
        
        assertTrue(res[0]);
        assertFalse(res[1]);
        assertFalse(res[2]);
        
        // Test #2
        filter.setDistanceThreshold(7);
        
        res = filter.label(toLabel);
        
        assertTrue(res[0]);
        assertTrue(res[1]);
        assertFalse(res[2]);
    }
    
}
