package magpie.attributes.generators.composition;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class IonicCompoundProximityAttributeGeneratorTest {
    
    @Test
    public void testAttributeGenerator() throws Exception {
        // Make a fake dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Na0.9Cl1.1");
        data.addEntry("CuCl2");
        data.addEntry("FeAl");
        data.addEntry("Fe");
        
        // Make attribute generator
        List<Object> options = new ArrayList<>();
        options.add(10);
        
        IonicCompoundProximityAttributeGenerator gen = 
                new IonicCompoundProximityAttributeGenerator();
        gen.setOptions(options);
        
        // Run generation and check results
        gen.addAttributes(data);
        
        assertEquals(1, data.NAttributes());
        assertArrayEquals(new double[]{0.1, 0, 2, 1}, data.getSingleAttributeArray(0), 1e-6);
        
        // Now, decrease the maximum size to 2, which means CuCl2 should match CuCl
        options.set(0, 2);
        gen.setOptions(options);
        
        data.clearAttributes();
        gen.addAttributes(data);
        
        assertEquals(1, data.NAttributes());
        assertArrayEquals(new double[]{0.1, 1f/3, 2, 1}, data.getSingleAttributeArray(0), 1e-6);
    }
    
}
