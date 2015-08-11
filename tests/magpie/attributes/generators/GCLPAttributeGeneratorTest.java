package magpie.attributes.generators;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class GCLPAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Create the hull dataset
        CompositionDataset hullData = new CompositionDataset();
        hullData.addEntry("NiAl");
        hullData.getEntry(0).setMeasuredClass(-1);
        
        // Create the test set
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Al");
        data.addEntry("Ni3Al");
        data.addEntry("NiAl");
        
        // Compute attributes
        GCLPAttributeGenerator gen = new GCLPAttributeGenerator();
        gen.setPhases(hullData);
        gen.addAttributes(data);
        
        // Test results
        assertArrayEquals(new double[]{0,1,0,0,0},
                data.getEntry(0).getAttributes(), 1e-6);
        assertArrayEquals(new double[]{-0.5,2,Math.sqrt(0.125),
            Math.sqrt(0.125),Math.log(0.5)}, data.getEntry(1).getAttributes(), 1e-6);
        assertArrayEquals(new double[]{-1,1,0,0,0},
                data.getEntry(2).getAttributes(), 1e-6);
        
        // Print description
        System.out.println(gen.printDescription(true));
    }
    
}
