package magpie.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MultiPropertyDatasetTest {
    
    /**
     * Repeats tests from Dataset, but with a {@linkplain MultiPropertyDataset}
     * @throws Exception 
     */
    @Test
    public void testAddDatasetNoProperties() throws Exception {
        /// Create datasets with the same attributes, class
        MultiPropertyDataset data1 = new MultiPropertyDataset();
        MultiPropertyDataset data2 = new MultiPropertyDataset();
        data1.addEntry(new MultiPropertyEntry());
        data2.addEntry(new MultiPropertyEntry());
        
        data1.addAttribute("x", new double[]{1});
        data2.addAttribute("x", new double[]{2});
        
        // Test addition
        data1.addEntries(data2, false);
        
        assertEquals(2, data1.NEntries());
        assertEquals(1, data2.NEntries());
        assertEquals(1, data1.NAttributes());
        assertEquals(1, data1.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(2, data1.getEntry(1).getAttribute(0), 1e-6);
        assertNotSame(data1.getEntry(1), data2.getEntry(0)); // Entry was cloned
        
        // Add another attribute to dataset 2, make sure merge fails
        boolean except = false;
        
        data2.addAttribute("y", new double[]{0});
        data2.getEntry(0).setMeasuredClass(-1);
        
        try {
            data1.addEntries(data2, false);
        } catch (Exception e) {
            except = true;
        }
        
        assertTrue(except);
        assertEquals(2, data1.NEntries());
        
        // Try again with force turned on
        data1.addEntries(data2, true);
        
        assertEquals(3, data1.NEntries());
        assertEquals(0, data1.getEntry(2).NAttributes()); // Attribute were deleted
        assertEquals(-1, data1.getEntry(2).getMeasuredClass(), 1e-6); // Class unaffected
        
        // Make data2 have two classes
        data2.setClassNames(new String[]{"Yes", "No"});
        
        data1.addEntries(data2, true);
        
        assertEquals(4, data1.NEntries());
        assertEquals(1, data1.NClasses());
        assertEquals(0, data1.getEntry(3).NAttributes()); // Attribute were deleted
        assertFalse(data1.getEntry(3).hasMeasurement()); // Class was deleted
    }
    
    @Test
    public void testAddDatasetProperties() throws Exception {
        /// Create datasets with the same attributes, class
        MultiPropertyDataset data1 = new MultiPropertyDataset();
        MultiPropertyDataset data2 = new MultiPropertyDataset();
        data1.addEntry(new MultiPropertyEntry());
        data2.addEntry(new MultiPropertyEntry());
        
        // Test #1: Same properties
        data1.addProperty("prop1");
        data2.addProperty("prop1");
        
        data1.getEntry(0).addProperty();
        data1.getEntry(0).setMeasuredProperty(0, -1);
        data2.getEntry(0).addProperty();
        data2.getEntry(0).setPredictedProperty(0, 2);
        
        data1.addEntries(data2, true);
        
        assertEquals(1, data1.NProperties());
        assertEquals(1, data2.NProperties());
        assertEquals(-1, data1.getEntry(0).getMeasuredProperty(0), 1e-6);
        assertFalse(data2.getEntry(0).hasMeasuredProperty(0));
        assertEquals(2, data2.getEntry(0).getPredictedProperty(0), 1e-6);
        
        // Test #2: Different properties
        data1.addProperty("prop2");
        data1.getEntry(0).addProperty();
        data1.getEntry(1).addProperty();
        data1.setTargetProperty(1, true);
        
        data2.addProperty("prop3"); // Continuous propert
        data2.getEntry(0).addProperty(-1, 2);
        
        data2.addProperty("prop4", new String[]{"yes", "no"});
        data2.getEntry(0).addProperty();
        data2.getEntry(0).setPredictedProperty(2, new double[]{0.1,0.9});
        data2.setTargetProperty(2, true);
        
        data1.addEntries(data2, true);
        
        // Test number of properties
        assertEquals(4, data1.NProperties());
        assertEquals(3, data2.NProperties());
        for (BaseEntry entryPtr : data1.getEntries()) {
            MultiPropertyEntry entry = (MultiPropertyEntry) entryPtr;
            assertEquals(data1.NProperties(), entry.NProperties());
        }
        assertArrayEquals(new String[]{"prop1", "prop2", "prop3", "prop4"},
                data1.getPropertyNames());
        assertArrayEquals(new String[]{"yes", "no"}, data1.getPropertyClasses(3));
        
        // Make sure target property did not change
        assertEquals(1, data1.getTargetPropertyIndex());
        assertEquals(2, data2.getTargetPropertyIndex());
        
        // Check entries
        assertEquals(3, data1.NEntries());
        assertTrue(data1.getEntry(2).hasMeasuredProperty(2));
        assertTrue(data1.getEntry(2).hasPredictedProperty(2));
        assertFalse(data1.getEntry(2).hasMeasuredProperty(3));
        assertTrue(data1.getEntry(2).hasPredictedProperty(3));
        assertArrayEquals(new double[]{0.1,0.9}, 
                data1.getEntry(2).getPropertyClassProbabilties(3), 1e-6);
    }

    @Test
    public void testSave() throws Exception {
        // Make a sample dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("prop");
        
        // Write a CSV file to disk
        File file = new File(data.saveCommand("test", "prop"));
        assertEquals("test.prop", file.getName());
        file.deleteOnExit();
    }
}
