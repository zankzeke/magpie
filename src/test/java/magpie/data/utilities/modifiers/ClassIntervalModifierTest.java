package magpie.data.utilities.modifiers;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ClassIntervalModifierTest {

    @Test
    public void testDataset() throws Exception {
        // Make a dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry()); // Bin0
        data.addEntry(new BaseEntry()); // Bin1
        data.addEntry(new BaseEntry()); // Bin2
        data.addEntry(new BaseEntry()); // No bin
        
        data.getEntry(0).setMeasuredClass(0.5);
        data.getEntry(1).setMeasuredClass(1.5);
        data.getEntry(2).setMeasuredClass(2.5);
        
        // Make the modifier
        ClassIntervalModifier mdfr = new ClassIntervalModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("1");
        options.add("2");
        
        mdfr.setOptions(options);
        
        // Print usage
        System.out.println(mdfr.printUsage());
        
        // Test modifier
        mdfr.transform(data);
        
        assertEquals(3, data.NClasses());
        assertEquals(4, data.NEntries());
        assertEquals(0, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(1).getMeasuredClass(), 1e-6);
        assertEquals(2, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertFalse(data.getEntry(3).hasMeasurement());
    }
    
    @Test
    public void testMultiPropertyDatasetNewProperty() throws Exception {
        // Make a dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("prop");
        data.setTargetProperty(0, true);
        
        data.addEntry(new MultiPropertyEntry()); // Bin0
        data.addEntry(new MultiPropertyEntry()); // Bin1
        data.addEntry(new MultiPropertyEntry()); // Bin2
        data.addEntry(new MultiPropertyEntry()); // No bin
        
        data.getEntry(0).setMeasuredClass(0.5);
        data.getEntry(1).setMeasuredClass(1.5);
        data.getEntry(2).setMeasuredClass(2.5);
        
        // Make the modifier
        ClassIntervalModifier mdfr = new ClassIntervalModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("1");
        options.add("2");
        
        mdfr.setOptions(options);
        
        // Test modifier
        mdfr.transform(data);
        
        assertEquals(3, data.NClasses());
        assertEquals(4, data.NEntries());
        
        // Check property count
        assertEquals(2, data.NProperties());
        for (BaseEntry e : data.getEntries()) {
            assertEquals(2, ((MultiPropertyEntry) e).NProperties());
        }
        
        // Check new classes
        assertEquals(0, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(1).getMeasuredClass(), 1e-6);
        assertEquals(2, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertFalse(data.getEntry(3).hasMeasurement());
        
        // Check old properties
        data.setTargetProperty(0, true);
        assertEquals(0.5, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertEquals(1.5, data.getEntry(1).getMeasuredClass(), 1e-6);
        assertEquals(2.5, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertFalse(data.getEntry(3).hasMeasurement());
    }
    
    @Test
    public void testMultiPropertyDataset() throws Exception {
        // Make a dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        
        data.addEntry(new MultiPropertyEntry()); // Bin0
        data.addEntry(new MultiPropertyEntry()); // Bin1
        data.addEntry(new MultiPropertyEntry()); // Bin2
        data.addEntry(new MultiPropertyEntry()); // No bin
        
        data.getEntry(0).setMeasuredClass(0.5);
        data.getEntry(1).setMeasuredClass(1.5);
        data.getEntry(2).setMeasuredClass(2.5);
        
        // Make the modifier
        ClassIntervalModifier mdfr = new ClassIntervalModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("1");
        options.add("2");
        
        mdfr.setOptions(options);
        
        // Test modifier
        mdfr.transform(data);
        
        assertEquals(3, data.NClasses());
        assertEquals(4, data.NEntries());
        
        // Check property count
        assertEquals(0, data.NProperties());
        for (BaseEntry e : data.getEntries()) {
            assertEquals(0, ((MultiPropertyEntry) e).NProperties());
        }
        
        // Check new classes
        assertEquals(0, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(1).getMeasuredClass(), 1e-6);
        assertEquals(2, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertFalse(data.getEntry(3).hasMeasurement());
    }
}
