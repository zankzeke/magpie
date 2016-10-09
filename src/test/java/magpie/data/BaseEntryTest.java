package magpie.data;

import org.json.JSONObject;
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
    
    @Test
    public void testJSON() {
        BaseEntry e = new BaseEntry();
        
        // Empty entry
        JSONObject j = e.toJSON();
        assertArrayEquals((double[]) j.get("attributes"), e.getAttributes(), 1e-6);
        assertEquals(j.getJSONObject("class").has("measured"), e.hasMeasurement());
        assertEquals(j.getJSONObject("class").has("predicted"), e.hasPrediction());
        assertEquals(j.getJSONObject("class").has("probabilities"), e.hasClassProbabilities());
        
        // Entry with some attributes
        e.addAttributes(new double[]{1.2, -3});
        j = e.toJSON();
        
        assertArrayEquals((double[]) j.get("attributes"), e.getAttributes(), 1e-6);
        assertEquals(j.getJSONObject("class").has("measured"), e.hasMeasurement());
        assertEquals(j.getJSONObject("class").has("predicted"), e.hasPrediction());
        assertEquals(j.getJSONObject("class").has("probabilities"), e.hasClassProbabilities());
        
        // Entry with some classes
        e.setMeasuredClass(0);
        e.setClassProbabilities(new double[]{0.1,0.9});
        j = e.toJSON();
        
        assertArrayEquals((double[]) j.get("attributes"), e.getAttributes(), 1e-6);
        assertEquals(j.getJSONObject("class").has("measured"), e.hasMeasurement());
        assertEquals(j.getJSONObject("class").getDouble("measured"), e.getMeasuredClass(), 1e-6);
        assertEquals(j.getJSONObject("class").has("predicted"), e.hasPrediction());
        assertEquals(j.getJSONObject("class").getDouble("predicted"), e.getPredictedClass(), 1e-6);
        assertEquals(j.getJSONObject("class").has("probabilities"), e.hasClassProbabilities());
    }
    
}
