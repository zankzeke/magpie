package magpie.data.utilities.normalizers;

import java.util.LinkedList;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for {@linkplain RegularSolutionNormalizer}. As this is specific 
 * to {@linkplain CompositionDataset}, we do not utilize these tests in
 * {@linkplain BaseDatasetNormalizerTest}.
 * 
 * @author Logan Ward
 */
public class RegularSolutionNormalizerTest {
    
    @Test
    public void test() throws Exception {
        // Make a fake dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Cu3Au");
        data.addEntry("Al2FeZr");
        
        data.addAttribute("x", new double[]{0,4,6});
        data.setPredictedClasses(new double[]{-1,-2,-3});
        data.getEntry(0).setMeasuredClass(1);
        
        // Make normalizer
        RegularSolutionNormalizer norm = new RegularSolutionNormalizer();
        
        norm.setOptions(new LinkedList<>());
        
        System.out.println(norm.printUsage());
        
        // Run the normalization
        norm.setToNormalizeAttributes(true);
        norm.setToNormalizeClass(true);
        
        norm.train(data);
        norm.normalize(data);
        
        // Test results
        assertArrayEquals(new double[]{0,4/(0.25 * 0.75),6/(0.5*0.25*2 + 0.25*0.25)},
                data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{-1/0.25,-2/(0.25 * 0.75),-3/(0.5*0.25*2 + 0.25*0.25)},
                data.getPredictedClassArray(), 1e-6);
        assertEquals(1/0.25, data.getEntry(0).getMeasuredClass(), 1e-6);
        
        // Turn it back
        norm.restore(data);
        
        assertArrayEquals(new double[]{0,4,6}, data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{-1,-2,-3}, data.getPredictedClassArray(), 1e-6);
        assertEquals(1, data.getEntry(0).getMeasuredClass(), 1e-6);
    }
    
}
