package magpie.attributes.generators.composition;

import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MeredigAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("FeO");
        
        // Set generators
        data.clearAttributeGenerators();
        data.addAttribueGenerator(new ElementFractionAttributeGenerator());
        data.addAttribueGenerator(new MeredigAttributeGenerator());
        data.addAttribueGenerator(new ValenceShellAttributeGenerator());
        
        // Create attributes
        data.generateAttributes();
        
        // Check results
        assertEquals(129, data.NAttributes());
        assertEquals(data.NAttributes(), data.getEntry(0).NAttributes());
        CompositionEntry entry = data.getEntry(0);
        assertEquals(0.5, entry.getAttribute(25), 1e-5);
        assertEquals(0.5, entry.getAttribute(7), 1e-5);
        assertEquals(data.getAttributeName(112), 35.92, entry.getAttribute(112), 1e-2);
        assertEquals(data.getAttributeName(113), 12.0, entry.getAttribute(113), 1e-2);
        assertEquals(data.getAttributeName(114), 3.0, entry.getAttribute(114), 1e-2);
        assertEquals(data.getAttributeName(115), 18, entry.getAttribute(115), 1e-2);
        assertEquals(data.getAttributeName(116), 17, entry.getAttribute(116), 1e-2);
        assertEquals(data.getAttributeName(117), 66, entry.getAttribute(117), 1e-2); // Different R than PRB paper
        assertEquals(data.getAttributeName(118), 99, entry.getAttribute(118), 1e-2); // Different R than PRB paper
        assertEquals(data.getAttributeName(119), 1.61, entry.getAttribute(119), 1e-2);
        assertEquals(data.getAttributeName(120), 2.635, entry.getAttribute(120), 1e-2);
        assertEquals(data.getAttributeName(121), 2, entry.getAttribute(121), 1e-2); // Fe only has 2 s valence
        assertEquals(data.getAttributeName(122), 2, entry.getAttribute(122), 1e-2);
        assertEquals(data.getAttributeName(123), 3, entry.getAttribute(123), 1e-2);
        assertEquals(data.getAttributeName(124), 0, entry.getAttribute(124), 1e-2);
        assertEquals(data.getAttributeName(125), 2.0 / 7.0, entry.getAttribute(125), 1e-2); // Fe only has 2 s valence
        assertEquals(data.getAttributeName(126), 2.0 / 7.0, entry.getAttribute(126), 1e-2); // Fe only has 2 s valence
        assertEquals(data.getAttributeName(127), 3.0 / 7.0, entry.getAttribute(127), 1e-2); // Fe only has 2 s valence
        assertEquals(data.getAttributeName(128), 0., entry.getAttribute(128), 1e-2);
        
        
    }
    
}
