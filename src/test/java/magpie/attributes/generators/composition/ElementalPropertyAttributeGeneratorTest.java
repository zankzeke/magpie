package magpie.attributes.generators.composition;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Logan Ward
 */
public class ElementalPropertyAttributeGeneratorTest {

    @Test
    public void testEasy() throws Exception {
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

    @Test
    public void testWithMissing() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("HBr");
        data.addElementalProperty("Number");
        data.addElementalProperty("ZungerPP-r_d");
        data.addElementalProperty("Row");

        // Make generator
        ElementalPropertyAttributeGenerator gen = new ElementalPropertyAttributeGenerator();

        // Run generator
        gen.addAttributes(data);

        // Test results
        assertEquals(18, data.NAttributes());
        assertEquals(18, data.getEntry(0).NAttributes());

        // Results for HBr, only bothering testing mean and max
        assertEquals(data.getAttributeName(0), 18, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(3), 35, data.getEntry(0).getAttribute(3), 1e-6);
        assertTrue(data.getAttributeName(6), Double.isNaN(data.getEntry(0).getAttribute(6)));
        assertTrue(data.getAttributeName(9), Double.isNaN(data.getEntry(0).getAttribute(9)));
        assertEquals(data.getAttributeName(12), 2.5, data.getEntry(0).getAttribute(12), 1e-6);
        assertEquals(data.getAttributeName(15), 4, data.getEntry(0).getAttribute(15), 1e-6);
    }
    
}
