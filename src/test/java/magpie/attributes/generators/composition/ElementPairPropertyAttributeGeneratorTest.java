package magpie.attributes.generators.composition;

import java.util.LinkedList;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ElementPairPropertyAttributeGeneratorTest {
    
    @Test
    public void test() throws Exception {
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Fe");
        data.addEntry("FeAl");
        data.addEntry("Fe2AlZr");
        data.addEntry("FeAl2Zr");
        
        data.addElementPairProperty("B2Volume");
        
        // Make the attribute generator
        ElementPairPropertyAttributeGenerator gen = new ElementPairPropertyAttributeGenerator();
        gen.setOptions(new LinkedList<>());
        System.out.println(gen.printUsage());
        
        // Run the attribute generation
        gen.addAttributes(data);
        
        // Check results
        assertEquals(5, data.NAttributes());
        
        for (int i=0; i<5; i++) { // Fe
            assertTrue(Double.isNaN(data.getEntry(0).getAttribute(i)));
        }
        
        // AlFe
        assertEquals(data.getAttributeName(0), 11.8028, data.getEntry(1).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 11.8028, data.getEntry(1).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 0, data.getEntry(1).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 11.8028, data.getEntry(1).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 0, data.getEntry(1).getAttribute(4), 1e-6);
        
        // Fe2AlZr
        assertEquals(data.getAttributeName(0), 19.3989, data.getEntry(2).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 11.8028, data.getEntry(2).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 7.5961, data.getEntry(2).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 14.94118, data.getEntry(2).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 2.510704, data.getEntry(2).getAttribute(4), 1e-6);
        
        // Fe2AlZr
        assertEquals(data.getAttributeName(0), 19.3989, data.getEntry(3).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 11.8028, data.getEntry(3).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 7.5961, data.getEntry(3).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 15.65082, data.getEntry(3).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 3.078416, data.getEntry(3).getAttribute(4), 1e-6);
        
        // Print description
        System.out.println(gen.printDescription(true));
        System.out.println(gen.printDescription(false));
    }
}
