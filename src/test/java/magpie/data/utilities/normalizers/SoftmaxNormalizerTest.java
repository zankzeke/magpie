package magpie.data.utilities.normalizers;

import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class SoftmaxNormalizerTest extends RescalingNormalizerTest {

    @Override
    public BaseDatasetNormalizer getNormalizer() {
        return new SoftmaxNormalizer();
    }

    @Override
    public void testAccuracy() {
        // Get a trial dataset
        Dataset data = generateTestSet();
        
        // Make the normalizer
        SoftmaxNormalizer norm = new SoftmaxNormalizer();
        norm.setR(1.0);
        norm.setToNormalizeAttributes(true);
        norm.setToNormalizeClass(true);
        
        // Run the normalization
        norm.train(data);
        norm.normalize(data);
        
        assertArrayEquals(new double[]{0.220129638480327,0.346953962059232,0.5,0.653046037940768,0.779870361519673},
                data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{0.5,0.5,0.5,0.5,0.5}, data.getSingleAttributeArray(1), 1e-6);
        assertArrayEquals(new double[]{0.779870361519673,0.653046037940768,0.5,0.346953962059232,0.220129638480327},
                data.getMeasuredClassArray(), 1e-6);
        assertArrayEquals(new double[]{0.5,0.346953962059232,0.220129638480327,0.779870361519673,0.653046037940768},
                data.getPredictedClassArray(), 1e-6);
    }

    @Override
    public void testSetOptions() throws Exception {
        // Get the normalizer
        SoftmaxNormalizer norm = new SoftmaxNormalizer();
        
        // Make the options
        List<Object> options = new LinkedList<>();
        options.add(3.5);
        
        norm.setOptions(options);
        
        // Test options
        assertEquals(3.5, norm.getR(), 1e-6);
        
        // Print usage
        System.out.println(norm.printUsage());
    }
}
