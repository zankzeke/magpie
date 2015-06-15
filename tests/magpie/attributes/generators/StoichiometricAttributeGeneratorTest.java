package magpie.attributes.generators;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author Logan Ward
 */
public class StoichiometricAttributeGeneratorTest {
    
    @Test
    public void test() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        
        // Make generator
        StoichiometricAttributeGenerator gen = new StoichiometricAttributeGenerator();
        gen.addPNorm(2);
        gen.addPNorm(3);
        
        // Run generator
        gen.addAttributes(data);
        
        // Test results
        assertEquals(3, data.NAttributes());
        assertEquals(3, data.getEntry(0).NAttributes());
        assertEquals(2, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(Math.sqrt(1.0/2), data.getEntry(0).getAttribute(1), 1e-6);
        assertEquals(Math.pow(0.25, 1.0/3), data.getEntry(0).getAttribute(2), 1e-6);
        
        // Print results
        System.out.println(gen.printDescription(true));
    }
    
}
