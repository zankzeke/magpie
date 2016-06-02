package magpie.attributes.generators.composition;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ChargeDependentAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Fe");
        data.addEntry("ZrO2");
        data.addEntry("UF6");
        data.addEntry("Na2CoOSe");
        
        // Compute attributes
        ChargeDependentAttributeGenerator gen = new ChargeDependentAttributeGenerator();
        gen.addAttributes(data);
        
        // Test results
        double[] attr = data.getEntry(0).getAttributes();
        assertEquals(data.getAttributeName(0), -1, attr[0], 1e-6);
        assertEquals(data.getAttributeName(1), 1, attr[1], 1e-6);
        assertEquals(data.getAttributeName(2), 2, attr[2], 1e-6);
        assertEquals(data.getAttributeName(3), 1.0, attr[3], 1e-6);
        assertEquals(data.getAttributeName(4), 0, attr[4], 1e-6);
        assertEquals(data.getAttributeName(5), 5.139076, attr[5], 1e-6);
        assertEquals(data.getAttributeName(6), 349, attr[6], 1e-6);
        assertEquals(data.getAttributeName(7), 3.16-0.93, attr[7], 1e-6);
        
        for (double a : data.getEntry(1).getAttributes()) {
            assertTrue(Double.isNaN(a));
        }
        
        attr = data.getEntry(2).getAttributes(); // ZrO2
        assertEquals(data.getAttributeName(0), -2, attr[0], 1e-6);
        assertEquals(data.getAttributeName(1), 4, attr[1], 1e-6);
        assertEquals(data.getAttributeName(2), 6, attr[2], 1e-6);
        assertEquals(data.getAttributeName(3), 8f/3, attr[3], 1e-6);
        assertEquals(data.getAttributeName(4), 8f/9, attr[4], 1e-6);
        assertEquals(data.getAttributeName(5), 77.0639, attr[5], 1e-6);
        assertEquals(data.getAttributeName(6), 141*2, attr[6], 1e-6);
        assertEquals(data.getAttributeName(7), 3.44-1.33, attr[7], 1e-6);
        
        attr = data.getEntry(3).getAttributes(); // UF6
        assertEquals(data.getAttributeName(0), -1, attr[0], 1e-6);
        assertEquals(data.getAttributeName(1), 6, attr[1], 1e-6);
        assertEquals(data.getAttributeName(2), 7, attr[2], 1e-6);
        assertEquals(data.getAttributeName(3), 12f/7, attr[3], 1e-6);
        assertEquals(data.getAttributeName(4), 1.224489796, attr[4], 1e-6);
        assertTrue(Double.isNaN(attr[5]));
        assertTrue(Double.isNaN(attr[6]));
        assertTrue(Double.isNaN(attr[7]));
        
        attr = data.getEntry(4).getAttributes(); // Na2CoOSe
        assertEquals(data.getAttributeName(0), -2, attr[0], 1e-6);
        assertEquals(data.getAttributeName(1), 2, attr[1], 1e-6);
        assertEquals(data.getAttributeName(2), 4, attr[2], 1e-6);
        assertEquals(data.getAttributeName(3), 1.6, attr[3], 1e-6);
        assertEquals(data.getAttributeName(4), 0.48, attr[4], 1e-6);
        assertEquals(data.getAttributeName(5), 5.139076 * 2f/3 + 24.96501/3 , attr[5], 1e-6);
        assertEquals(data.getAttributeName(6), 141f + 195f, attr[6], 1e-6);
        assertEquals(data.getAttributeName(7), 2.995-1.246666667, attr[7], 1e-6);
        
        // Print description
        System.out.println(gen.printDescription(true));
        System.out.println(gen.getCitations());
    }
    
}
