package magpie.attributes.generators.element;

import magpie.data.materials.ElementDataset;
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
        ElementDataset data = new ElementDataset();
        data.addEntry("Fe");
        data.addElementalProperty("Number");
        data.addElementalProperty("Electronegativity");
        
        // Make generator
        ElementalPropertyAttributeGenerator gen = new ElementalPropertyAttributeGenerator();
        
        // Run generator
        gen.addAttributes(data);
        
        // Test results
        assertEquals(2, data.NAttributes());
        assertEquals(2, data.getEntry(0).NAttributes());
        
        // Results 
        assertEquals(data.getAttributeName(0), 26, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 1.83, data.getEntry(0).getAttribute(1), 1e-6);
        
        // Print results
        System.out.println(gen.printDescription(true));
    }
    
}
