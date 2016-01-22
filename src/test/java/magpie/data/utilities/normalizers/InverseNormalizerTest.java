package magpie.data.utilities.normalizers;

import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class InverseNormalizerTest extends BaseDatasetNormalizerTest {

    @Override
    public BaseDatasetNormalizer getNormalizer() {
        return new InverseNormalizer();
    }

    @Override
    public void testAccuracy() {
        // Get a trial dataset
        Dataset data = generateTestSet();
        
        // Make the normalizer
        InverseNormalizer norm = new InverseNormalizer();
        norm.setScale(1.0);
        norm.setToNormalizeAttributes(true);
        norm.setToNormalizeClass(true);
        
        // Run the normalization
        norm.train(data);
        norm.normalize(data);
        
        assertArrayEquals(new double[]{1,0.5,1f/3,0.25,0.2}, data.getSingleAttributeArray(0), 1e-6);
        assertArrayEquals(new double[]{0.2,0.25,1f/3,0.5,1}, data.getMeasuredClassArray(), 1e-6);
        assertArrayEquals(new double[]{1f/3,0.5,1,0.2,0.25}, data.getPredictedClassArray(), 1e-6);
    }

    /**
     * Get the dataset using for the normalization test
     * @return Dataset with a single attribute, measured and predicted class
     */
    protected Dataset generateTestSet() {
        // Make a test dataset
        Dataset data = new Dataset();
        for (int i=0; i<5; i++) {
            data.addEntry(new BaseEntry());
        }
        data.addAttribute("x", new double[]{1,2,3,4,5});
        data.setMeasuredClasses(new double[]{5,4,3,2,1});
        data.setPredictedClasses(new double[]{3,2,1,5,4});
        return data;
    }

    @Override
    public void testSetOptions() throws Exception {
        // Make the normalizer
        InverseNormalizer norm = new InverseNormalizer();
        
        // Make the options
        List<Object> options = new LinkedList<>();
        options.add(5.0);
        
        norm.setOptions(options);
        System.out.println(norm.printUsage());
        
        // Make sure it set
        assertEquals(5.0, norm.getScale(), 1e-6);
    }
    
    
    
    
    
}
