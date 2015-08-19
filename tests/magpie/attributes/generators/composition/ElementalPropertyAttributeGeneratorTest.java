package magpie.attributes.generators.composition;

import magpie.attributes.generators.composition.ElementalPropertyAttributeGenerator;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ElementalPropertyAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Fe2O3");
        data.addElementalProperty("Number");
        
        // Make generator
        ElementalPropertyAttributeGenerator gen = new ElementalPropertyAttributeGenerator();
        
        // Run generator
        gen.addAttributes(data);
        
        // Test results
        assertEquals(6, data.NAttributes());
        assertEquals(6, data.getEntry(0).NAttributes());
        
        // Results for NaCl
        assertEquals(data.getAttributeName(0), 14, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1),  6, data.getEntry(0).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2),  3, data.getEntry(0).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 17, data.getEntry(0).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 11, data.getEntry(0).getAttribute(4), 1e-6);
        assertEquals(data.getAttributeName(5), 14, data.getEntry(0).getAttribute(5), 1e-6);
        
        // Results for Fe2O3
        assertEquals(data.getAttributeName(0), 15.2, data.getEntry(1).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1),   18, data.getEntry(1).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 8.64, data.getEntry(1).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3),   26, data.getEntry(1).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4),    8, data.getEntry(1).getAttribute(4), 1e-6);
        assertEquals(data.getAttributeName(5),    8, data.getEntry(1).getAttribute(5), 1e-6);
        
        // Print results
        System.out.println(gen.printDescription(true));
    }
    
}
