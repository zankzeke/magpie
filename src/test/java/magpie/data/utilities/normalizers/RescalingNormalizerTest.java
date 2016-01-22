package magpie.data.utilities.normalizers;

import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class RescalingNormalizerTest extends InverseNormalizerTest {

    @Override
    public BaseDatasetNormalizer getNormalizer() {
        return new RescalingNormalizer();
    }

    @Override
    public void testAccuracy() {
        // Get trial dataset
        Dataset data = generateTestSet();
        
        // Make the normalizer
        RescalingNormalizer norm = new RescalingNormalizer();
        norm.setToNormalizeAttributes(true);
        norm.setToNormalizeClass(true);
        
        // Run the normalization
        norm.train(data);
        norm.normalize(data);
        
        assertArrayEquals(new double[]{-1,-0.5,0,0.5,1}, data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{-1,-1,-1,-1,-1}, data.getSingleAttributeArray(1), 1e-6);
        assertArrayEquals(new double[]{1,0.5,0,-0.5,-1}, data.getMeasuredClassArray(), 1e-6);
        assertArrayEquals(new double[]{0,-0.5,-1,1,0.5}, data.getPredictedClassArray(), 1e-6);
    }

    @Override
    public void testSetOptions() throws Exception {
        // Get normalizer
        RescalingNormalizer norm = new RescalingNormalizer();
        
        // Print usage
        System.out.println(norm.printUsage());
    }

    @Override
    protected Dataset generateTestSet() {
        Dataset data = super.generateTestSet(); 
        
        // Add a series of all same number
        data.addAttribute("y", new double[]{2,2,2,2,2});
        
        return data;
    }
    
    
    
}
