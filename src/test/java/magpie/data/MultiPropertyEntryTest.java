package magpie.data;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MultiPropertyEntryTest {
    
    @Test
    public void testSetMeasurements() throws Exception {
        // Make a sample entry
        MultiPropertyEntry entry = new MultiPropertyEntry();
        
        // Test setting measured class
        entry.setMeasuredClass(-1);
        assertTrue(entry.hasMeasurement());
        assertEquals(-1, entry.getMeasuredClass(), 1e-6);
        assertFalse(entry.hasPrediction());
        assertEquals(0, entry.NProperties());
        assertEquals(-1, entry.getTargetProperty());
        
        // Predicted class as single number
        entry.setPredictedClass(1);
        assertTrue(entry.hasMeasurement());
        assertEquals(-1, entry.getMeasuredClass(), 1e-6);
        assertTrue(entry.hasPrediction());
        assertEquals(1, entry.getPredictedClass(), 1e-6);
        assertEquals(-1, entry.getTargetProperty());
        
        // Predicted class as probabilities
        entry.setClassProbabilities(new double[]{0.2, 0.3, 0.4});
        assertTrue(entry.hasMeasurement());
        assertEquals(-1, entry.getMeasuredClass(), 1e-6);
        assertTrue(entry.hasPrediction());
        assertEquals(2, entry.getPredictedClass(), 1e-6);
        assertEquals(-1, entry.getTargetProperty());
        
        // Predicted class as single number
        entry.setPredictedClass(1.0);
        assertTrue(entry.hasMeasurement());
        assertEquals(-1, entry.getMeasuredClass(), 1e-6);
        assertTrue(entry.hasPrediction());
        assertEquals(1.0, entry.getPredictedClass(), 1e-6);
        assertEquals(-1, entry.getTargetProperty());
        
        // Add a property
        entry.addProperty();
        assertEquals(1, entry.NProperties());
        assertEquals(-1, entry.getTargetProperty());
        
        // Set target property
        entry.setTargetProperty(0);
        assertEquals(0, entry.getTargetProperty());
        
        // Test setting measured class
        entry.setMeasuredClass(-2);
        assertTrue(entry.hasMeasurement());
        assertEquals(-2, entry.getMeasuredClass(), 1e-6);
        assertFalse(entry.hasPrediction());
        assertEquals(1, entry.NProperties());
        assertEquals(0, entry.getTargetProperty());
        
        // Predicted class as single number
        entry.setPredictedClass(2);
        assertTrue(entry.hasMeasurement());
        assertEquals(-2, entry.getMeasuredClass(), 1e-6);
        assertTrue(entry.hasPrediction());
        assertEquals(2, entry.getPredictedClass(), 1e-6);
        assertEquals(0, entry.getTargetProperty());
        
        // Predicted class as probabilities
        entry.setClassProbabilities(new double[]{0.2, 0.4, 0.3});
        assertTrue(entry.hasMeasurement());
        assertEquals(-2, entry.getMeasuredClass(), 1e-6);
        assertTrue(entry.hasPrediction());
        assertEquals(1, entry.getPredictedClass(), 1e-6);
        assertEquals(0, entry.getTargetProperty());
        
        // Predicted class as single number
        entry.setPredictedClass(3.0);
        assertTrue(entry.hasMeasurement());
        assertEquals(-2, entry.getMeasuredClass(), 1e-6);
        assertTrue(entry.hasPrediction());
        assertEquals(3, entry.getPredictedClass(), 1e-6);
        assertEquals(0, entry.getTargetProperty());
        
        // Set back to default class, check results
        entry.setTargetProperty(-1);
        assertTrue(entry.hasMeasurement());
        assertEquals(-1, entry.getMeasuredClass(), 1e-6);
        assertTrue(entry.hasPrediction());
        assertEquals(1.0, entry.getPredictedClass(), 1e-6);
        assertEquals(-1, entry.getTargetProperty());
    }
    
    @Test
    public void testDeleteMeasurement() {
        MultiPropertyEntry entry = new MultiPropertyEntry();
        entry.addProperty();
        
        // Test the entry
        assertEquals(1, entry.NProperties());
        assertEquals(-1, entry.getTargetProperty());
        assertFalse(entry.hasMeasurement());
        
        // Set the measurement
        entry.setMeasuredClass(1.0);
        assertTrue(entry.hasMeasurement());
        
        // Make sure you can delete it
        entry.deleteMeasuredClass();
        assertFalse(entry.hasMeasurement());
        
        // Make the property the target property amd repeat
        entry.setTargetProperty(0);
        
        // Test the entry
        assertEquals(0, entry.getTargetProperty());
        assertFalse(entry.hasMeasurement());
        
        // Set the measurement
        entry.setMeasuredClass(1.0);
        assertTrue(entry.hasMeasurement());
        
        // Make sure you can delete it
        entry.deleteMeasuredClass();
        assertFalse(entry.hasMeasurement());
    }
    
}
