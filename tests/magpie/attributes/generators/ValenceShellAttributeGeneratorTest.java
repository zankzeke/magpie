package magpie.attributes.generators;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ValenceShellAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("CeFeO3");
        data.addElementalProperty("Number");
        
        // Make generator
        ValenceShellAttributeGenerator gen = new ValenceShellAttributeGenerator();
        
        // Run generator
        gen.addAttributes(data);
        
        // Test results
        assertEquals(4, data.NAttributes());
        assertEquals(4, data.getEntry(0).NAttributes());
        
        // Results for NaCl
        assertEquals(data.getAttributeName(0), 0.375,data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 0.625, data.getEntry(0).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2),  0, data.getEntry(0).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3),  0, data.getEntry(0).getAttribute(3), 1e-6);
        
        // Results for Fe2O3
        assertEquals(data.getAttributeName(0), 2.0/6, data.getEntry(1).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 2.4/6, data.getEntry(1).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 1.4/6, data.getEntry(1).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 0.2/6, data.getEntry(1).getAttribute(3), 1e-6);
    }
    
}
