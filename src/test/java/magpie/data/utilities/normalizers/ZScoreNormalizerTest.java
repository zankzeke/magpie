package magpie.data.utilities.normalizers;

import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ZScoreNormalizerTest extends RescalingNormalizerTest {

    @Override
    public BaseDatasetNormalizer getNormalizer() {
        return new ZScoreNormalizer();
    }

    @Override
    public void testAccuracy() {
        // Get a trial dataset
        Dataset data = generateTestSet();
        
        // Make the normalizer
        ZScoreNormalizer norm = new ZScoreNormalizer();
        norm.setToNormalizeAttributes(true);
        norm.setToNormalizeClass(true);
        
        // Run the normalization
        norm.train(data);
        norm.normalize(data);
        
        assertArrayEquals(new double[]{-1.26491106406735,-0.632455532033676,0,0.632455532033676,1.26491106406735},
                data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{0,0,0,0,0}, data.getSingleAttributeArray(1), 1e-6);
        assertArrayEquals(new double[]{1.26491106406735,0.632455532033676,0,-0.632455532033676,-1.26491106406735},
                data.getMeasuredClassArray(), 1e-6);
        assertArrayEquals(new double[]{0,-0.632455532033676,-1.26491106406735,1.26491106406735,0.632455532033676},
                data.getPredictedClassArray(), 1e-6);
    }

    @Override
    public void testSetOptions() throws Exception {
        System.out.println(new ZScoreNormalizer().printUsage());
    }
    
}
