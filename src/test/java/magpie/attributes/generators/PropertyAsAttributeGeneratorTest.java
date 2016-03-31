package magpie.attributes.generators;

import java.util.LinkedList;
import java.util.List;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PropertyAsAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Create the MultiPropertyDataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("x");
        
        data.addEntry(new MultiPropertyEntry());
        data.addEntry(new MultiPropertyEntry());
        
        data.getEntry(0).setMeasuredProperty(0, -1);
        
        // Create the attribute generator
        PropertyAsAttributeGenerator gen = new PropertyAsAttributeGenerator();
        
        List<Object> options = new LinkedList<>();
        options.add("x");
        
        gen.setOptions(options);
        System.out.println(gen.printUsage());
        System.out.println(gen.printDescription(true));
        
        // Run the generator
        gen.addAttributes(data);
        
        // Test results
        assertEquals(1, data.NAttributes());
        assertArrayEquals(new String[]{"x"}, data.getAttributeNames());
        assertEquals(-1, data.getEntry(0).getAttribute(0), 1e-6);
        assertTrue(Double.isNaN(data.getEntry(1).getAttribute(0)));
    }
    
}
