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
public class PhaseDiagramExclusionFilterTest {
    
    @Test
    public void test() throws Exception {
        // Create test dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Al");
        data.addEntry("AlNi");
        data.addEntry("AlZr");
        data.addEntry("AlNiZr");
        data.addEntry("FeNiZr");
        
        // Create filter
        PhaseDiagramExclusionFilter filter = new PhaseDiagramExclusionFilter();
        
        List<Object> options = new ArrayList<>();
        options.add("Al");
        options.add("Ni");
        
        filter.setOptions(options);
        
        filter.setExclude(true);
        
        // Test results
        boolean[] res = filter.label(data);
        assertTrue(data.getEntry(0).toString(), res[0]);
        assertTrue(data.getEntry(1).toString(), res[1]);
        assertTrue(data.getEntry(2).toString(), !res[2]);
        assertTrue(data.getEntry(3).toString(), res[3]);
        assertTrue(data.getEntry(4).toString(), !res[4]);
    }
    
}
