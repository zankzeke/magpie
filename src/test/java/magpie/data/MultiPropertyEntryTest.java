package magpie.data;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author logan
 */
public class MultiPropertyEntryTest {
    
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
