package magpie.attributes.generators.composition;

import magpie.attributes.generators.composition.IonicityAttributeGenerator;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class IonicityAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Al2MnCu");
        data.addEntry("Fe");
        
        // Make generator
        IonicityAttributeGenerator gen = new IonicityAttributeGenerator();
        
        // Run generator
        gen.addAttributes(data);
        
        // Test results
        assertEquals(3, data.NAttributes());
        assertEquals(3, data.getEntry(0).NAttributes());
        
        // Results for NaCl
        assertEquals(data.getAttributeName(0),      1, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 0.7115, data.getEntry(0).getAttribute(1), 1e-2);
        assertEquals(data.getAttributeName(2), 0.3557, data.getEntry(0).getAttribute(2), 1e-2);
        
        // Results for Al2MnCu
        assertEquals(data.getAttributeName(0),      0, data.getEntry(1).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 0.0301, data.getEntry(1).getAttribute(1), 1e-2);
        assertEquals(data.getAttributeName(2), 0.0092, data.getEntry(1).getAttribute(2), 1e-2);
        
        // Results for Fe
        assertEquals(data.getAttributeName(0), 0, data.getEntry(2).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 0, data.getEntry(2).getAttribute(1), 1e-2);
        assertEquals(data.getAttributeName(2), 0, data.getEntry(2).getAttribute(2), 1e-2);
        
        // Print description
        System.out.println(gen.printDescription(true));
    }
    
}
