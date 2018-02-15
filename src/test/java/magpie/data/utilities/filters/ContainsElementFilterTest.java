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
public class ContainsElementFilterTest {

    @Test
    public void test() throws Exception {
        // Create test dataset
        CompositionDataset data = new  CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Cl");
        data.addEntry("Na");
        data.addEntry("NaFe");
        data.addEntry("FeAl");
        
        // Create filter
        ContainsElementFilter filter = new ContainsElementFilter();
        
        List<Object> options = new ArrayList<>();
        options.add("Na");
        
        filter.setOptions(options);
        
        // Run filter
        boolean[] res = filter.label(data);
        
        assertTrue(res[0]);
        assertFalse(res[1]);
        assertTrue(res[2]);
        assertTrue(res[3]);
        assertFalse(res[4]);
        
        // Test #2, add Cl
        options.add("Cl");
        
        filter.setOptions(options);
        
        res = filter.label(data);
        
        assertTrue(res[0]);
        assertTrue(res[1]);
        assertTrue(res[2]);
        assertTrue(res[3]);
        assertFalse(res[4]);
    }

    @Test
    public void testSetElements() throws Exception {
        ContainsElementFilter filter = new ContainsElementFilter();

        // Test set by index
        filter.setElementListByIndex(new int[]{12,0});
        assertArrayEquals(new int[]{12,0}, filter.ExcludedIndex);
        assertArrayEquals(new String[]{"Al","H"}, filter.ElementList);

        // Test set by name
        filter.setElementList(new String[]{"Al","H"});
        assertArrayEquals(new int[]{12,0}, filter.ExcludedIndex);
        assertArrayEquals(new String[]{"Al","H"}, filter.ElementList);
    }
}
