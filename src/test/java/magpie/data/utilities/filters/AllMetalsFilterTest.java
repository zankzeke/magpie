package magpie.data.utilities.filters;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class AllMetalsFilterTest {
    
    @Test
    public void test() throws Exception {
        // Make a make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Na");
        data.addEntry("NaFe");
        data.addEntry("NaCl");
        data.addEntry("FeAlO");
        data.addEntry("FeAlCrZr");
        
        // Make filter 
        AllMetalsFilter filter = new AllMetalsFilter();
        
        // Test results
        boolean[] label = filter.label(data);
        assertTrue(label[0]);
        assertTrue(label[1]);
        assertFalse(label[2]);
        assertFalse(label[3]);
        assertTrue(label[4]);
    }
    
}
