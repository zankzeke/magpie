package magpie.data;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class BaseEntryTest {
    
    @Test
    public void testSetDeleteClass() {
        BaseEntry entry = new BaseEntry();
        
        // Check the measured class
        assertFalse(entry.hasMeasurement());
        entry.setMeasuredClass(-1);
        assertTrue(entry.hasMeasurement());
        assertEquals(-1, entry.getMeasuredClass(), 1e-6);
        
        entry.deleteMeasuredClass();
        assertFalse(entry.hasMeasurement());
        
        // Check the measured class
        assertFalse(entry.hasPrediction());
        entry.setPredictedClass(-1);
        assertTrue(entry.hasPrediction());
        assertEquals(-1, entry.getPredictedClass(), 1e-6);
        
        entry.deletePredictedClass();
        assertFalse(entry.hasPrediction());
        
        entry.setClassProbabilities(new double[]{0.9,0.1});
        assertTrue(entry.hasPrediction());
        assertEquals(0, entry.getPredictedClass(), 1e-6);
    }
    
}
