package magpie.attributes.generators.composition;

import magpie.data.materials.CompositionDataset;
import magpie.data.materials.util.LookupData;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ElementFractionAttributeGeneratorTest {
    
    @Test
    public void test() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Fe");
        
        // Make generator
        ElementFractionAttributeGenerator gen = new ElementFractionAttributeGenerator();
        
        // Run generator
        gen.addAttributes(data);
        
        // Check results
        assertEquals(LookupData.ElementNames.length, data.NAttributes());
        assertEquals(data.NAttributes(), data.getEntry(0).NAttributes());
        assertEquals(1.0, StatUtils.sum(data.getEntry(0).getAttributes()), 1e-6);
        assertEquals(0.5, data.getEntry(0).getAttribute(10), 1e-6);
        assertEquals(1.0, StatUtils.sum(data.getEntry(1).getAttributes()), 1e-6);
        assertEquals(1.0, data.getEntry(1).getAttribute(25), 1e-6);
    }
    
}
