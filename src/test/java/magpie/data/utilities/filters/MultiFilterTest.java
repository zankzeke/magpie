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
public class MultiFilterTest {

    @Test
    public void test() throws Exception {
        // Create a sample dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("NaF");
        data.addEntry("BeF");
        data.addEntry("BeCl");
        
        // Create the filter
        MultiFilter filter = new MultiFilter();
        
        List<Object> options = new ArrayList<>();
        options.add("-filter");
        options.add("include");
        options.add("ContainsElementFilter");
        options.add("Na");
        options.add("-filter");
        options.add("exclude");
        options.add("ContainsElementFilter");
        options.add("Cl");
        
        filter.setOptions(options);
        
        // Run the filter
        boolean[] res = filter.label(data);
        
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertFalse(res[2]);
        assertFalse(res[3]);
        
    }
    
}
