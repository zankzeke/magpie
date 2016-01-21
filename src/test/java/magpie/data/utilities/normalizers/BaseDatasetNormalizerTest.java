package magpie.data.utilities.normalizers;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class BaseDatasetNormalizerTest {
    
    /**
     * Get a fresh instantiation of the normalizer, set to default settings
     * @return 
     */
    public BaseDatasetNormalizer getNormalizer() {
        return new RescalingNormalizer();
    }
    
    /**
     * Test setting the options of the normalizer. Must be overloaded.
     * 
     * <p>Should test all of the {@linkplain BaseDatasetNormalizer#setOptions(java.util.List) }
     * possibilities, run {@linkplain BaseDatasetNormalizer#printUsage() },
     * and test the {@linkplain BaseDatasetNormalizer#printDescription(boolean) }
     */
    @Test
    public void testSetOptions() {
        // To be overloaded
        assertTrue(getClass().getSimpleName().equals("BaseDatasetNormalizerTest"));
    }
    
    @Test
    public void testNormalizationRestore() {
        // Create two example datasets with 5 entries
        Dataset data = new Dataset();
        
        for (int i=0; i<5; i++) {
            data.addEntry(new BaseEntry());
        }
        
        // Give them different attributes
        data.addAttribute("x", new double[]{5,4,3,2,1});
        data.setMeasuredClasses(new double[]{1,2,3,4,5});
        data.setPredictedClasses(new double[]{1,2,4,4,4});
        
        // Get normalizer, run internal test
        BaseDatasetNormalizer norm = getNormalizer();
        assertTrue(norm.test(data));
    }
    
    @Test
    public void testClone() {
        // Create two example datasets with 5 entries
        Dataset data1 = new Dataset();
        Dataset data2 = new Dataset();
        
        for (int i=0; i<5; i++) {
            data1.addEntry(new BaseEntry());
            data2.addEntry(new BaseEntry());
        }
        
        // Give them different attributes
        data1.addAttribute("x", new double[]{5,4,3,2,1});
        data2.addAttribute("y", new double[]{1,2,3,4,5});
        
        // Create a noramlizer, train it on data1
        BaseDatasetNormalizer norm = getNormalizer();
        norm.setToNormalizeAttributes(true);
        norm.setToNormalizeClass(false);
        norm.train(data1);
        
        assertTrue(norm.isTrained());
        
        // Make sure it throws an exception if you pass norm2
        boolean failed = false;
        
        try {
            norm.normalize(data2);
        } catch (Exception e) {
            failed = true;
        }
        
        assertTrue(failed);
        
        failed = false;
        
        try {
            norm.normalize(data2);
        } catch (Exception e) {
            failed = true;
        }
        
        assertTrue(failed);
        
        // Normalize data1, save result, return
        norm.normalize(data1);
        double[] originalResult = data1.getSingleAttributeArray(0);
        norm.restore(data1);
        
        // Make a clone of the normalizer
        BaseDatasetNormalizer normClone = norm.clone();
        
        // Make sure it gives same results as original
        normClone.normalize(data1);
        assertArrayEquals(originalResult, data1.getSingleAttributeArray(0), 1e-6);
        normClone.restore(data1);
        
        // Train clone, make sure original's behavior doesn't change
        normClone.train(data2);
        
        norm.normalize(data1);
        assertArrayEquals(originalResult, data1.getSingleAttributeArray(0), 1e-6);
        norm.restore(data1);
    }
    
}
