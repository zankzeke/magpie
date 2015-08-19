package magpie.attributes.generators.composition;

import magpie.attributes.generators.composition.IonicCompoundAttributeGenerator;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class IonicCompoundAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        
        // Compute attributes
        IonicCompoundAttributeGenerator gen = new IonicCompoundAttributeGenerator();
        gen.addAttributes(data);
        
        // Test results
        double[] attr = data.getEntry(0).getAttributes();
        assertEquals(data.getAttributeName(0), -1, attr[0], 1e-6);
        assertEquals(data.getAttributeName(1), 1, attr[1], 1e-6);
        assertEquals(data.getAttributeName(2), 2, attr[2], 1e-6);
        assertEquals(data.getAttributeName(3), 0, attr[3], 1e-6);
        assertEquals(data.getAttributeName(4), 1, attr[4], 1e-6);
        assertEquals(data.getAttributeName(5), 5.139076, attr[5], 1e-6);
        assertEquals(data.getAttributeName(6), 349, attr[6], 1e-6);
        assertEquals(data.getAttributeName(7), 3.16-0.93, attr[7], 1e-6);
    }
    
}
