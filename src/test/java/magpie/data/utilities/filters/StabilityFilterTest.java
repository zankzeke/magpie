package magpie.data.utilities.filters;

import java.util.LinkedList;
import java.util.List;
import magpie.Magpie;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class StabilityFilterTest {

    @Test
    public void test() throws Exception {
        // Create the hull dataset
        CompositionDataset hullData = new CompositionDataset();
        hullData.addEntry("NiAl");
        hullData.getEntry(0).setMeasuredClass(-1);
        
        // Make the filter
        StabilityFilter filter = new StabilityFilter();
        
        List<Object> options = new LinkedList<>();
        options.add(hullData);
        options.add("predicted");
        options.add(0.025);
        
        filter.setOptions(options);
        
        // Make the set to be filtered
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NiAl");
        data.getEntry(0).setPredictedClass(-1 + 0.03);
        data.addEntry("Ni3Al");
        data.getEntry(1).setPredictedClass(-1.5);
        data.addEntry("NiAl");
        data.getEntry(2).setPredictedClass(-1);
        
        // Test the filter
        boolean[] res = filter.label(data);
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertTrue(res[2]);
        
        // Test parallel implementation
        Magpie.NThreads = 2;
        res = filter.parallelLabel(data);
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertTrue(res[2]);
    }
    
}
