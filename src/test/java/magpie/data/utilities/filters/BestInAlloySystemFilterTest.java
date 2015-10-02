package magpie.data.utilities.filters;

import java.util.LinkedList;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class BestInAlloySystemFilterTest {

    @Test
    public void test() throws Exception {
        // Make test dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaTi");
        data.getEntry(0).setMeasuredClass(1.0);
        data.addEntry("NaTi2");
        data.getEntry(1).setMeasuredClass(2.0);
        data.addEntry("NaTi2Fe");
        data.getEntry(2).setMeasuredClass(1.0);
        
        // Make filter
        BestInAlloySystemFilter filter = new BestInAlloySystemFilter();
        List<Object> options = new LinkedList<>();
        options.add("1");
        options.add("maximize");
        options.add("measured");
        options.add("TargetEntryRanker");
        options.add("0.5");
        filter.setOptions(options);
        filter.setExclude(false);
        
        // Run filter
        boolean[] res = filter.label(data);
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertTrue(res[2]);
        
        // Reverse direction of filter, add another entry
        data.addEntry("NaTi4");
        data.getEntry(3).setMeasuredClass(0.75);
        options.set(1, "minimize");
        filter.setOptions(options);
        res = filter.label(data);
        assertFalse(res[0]);
        assertFalse(res[1]);
        assertTrue(res[2]);
        assertTrue(res[3]);
    }
    
}
