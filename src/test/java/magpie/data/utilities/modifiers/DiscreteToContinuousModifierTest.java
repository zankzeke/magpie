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
public class DiscreteToContinuousModifierTest {

    @Test
    public void testDataset() throws Exception {
        // Make a dataset
        Dataset data = new Dataset();
        
        data.setClassNames(new String[]{"Yes", "No"});
        
        data.addEntry(new BaseEntry()); // Measured 0, 100% Class 0
        data.addEntry(new BaseEntry()); // No measurement, 45% Class 0
        data.addEntry(new BaseEntry()); // Measured 1, 99% Class 1
        data.addEntry(new BaseEntry()); // Neither measurement nor prediction
        
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(0).setClassProbabilities(new double[]{1,0});
        
        data.getEntry(1).setClassProbabilities(new double[]{0.45,0.55});
        
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(2).setClassProbabilities(new double[]{0.01,0.99});
        
        // Make the modifier
        DiscreteToContinuousModifier mdfr = new DiscreteToContinuousModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("Yes");
        
        mdfr.setOptions(options);
        
        // Print usage
        System.out.println(mdfr.printUsage());
        
        // Test modifier
        mdfr.transform(data);
        
        assertEquals(1, data.NClasses());
        assertEquals(4, data.NEntries());
        
        assertEquals(1, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(0).getPredictedClass(), 1e-6);
        
        assertFalse(data.getEntry(1).hasMeasurement());
        assertEquals(0.45, data.getEntry(1).getPredictedClass(), 1e-6);
        
        assertEquals(0, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertEquals(0.01, data.getEntry(2).getPredictedClass(), 1e-6);
        
        assertFalse(data.getEntry(3).hasMeasurement());
        assertFalse(data.getEntry(3).hasPrediction());
    }
    
    @Test
    public void testMultiPropertyDatasetNewProperty() throws Exception {
        // Make a dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("prop");
        data.setTargetProperty(0, true);
        
        data.setClassNames(new String[]{"Yes", "No"});
        
        data.addEntry(new MultiPropertyEntry()); // Measured 0, 100% Class 0
        data.addEntry(new MultiPropertyEntry()); // No measurement, 45% Class 0
        data.addEntry(new MultiPropertyEntry()); // Measured 1, 99% Class 1
        data.addEntry(new MultiPropertyEntry()); // Neither measurement nor prediction
        
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(0).setClassProbabilities(new double[]{1,0});
        
        data.getEntry(1).setClassProbabilities(new double[]{0.45,0.55});
        
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(2).setClassProbabilities(new double[]{0.01,0.99});
        
        // Make the modifier
        DiscreteToContinuousModifier mdfr = new DiscreteToContinuousModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("Yes");
        
        mdfr.setOptions(options);
        
        // Test modifier
        mdfr.transform(data);
        
        assertEquals(1, data.NClasses());
        assertEquals(4, data.NEntries());
        
        // Check property count
        assertEquals(2, data.NProperties());
        for (BaseEntry e : data.getEntries()) {
            assertEquals(2, ((MultiPropertyEntry) e).NProperties());
        }
        
        // Check new classes
        assertEquals(1, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(0).getPredictedClass(), 1e-6);
        
        assertFalse(data.getEntry(1).hasMeasurement());
        assertEquals(0.45, data.getEntry(1).getPredictedClass(), 1e-6);
        
        assertEquals(0, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertEquals(0.01, data.getEntry(2).getPredictedClass(), 1e-6);
        
        assertFalse(data.getEntry(3).hasMeasurement());
        assertFalse(data.getEntry(3).hasPrediction());
        
        // Check old properties
        data.setTargetProperty(0, true);
        
        assertEquals(0, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertArrayEquals(new double[]{1,0}, 
                data.getEntry(0).getPropertyClassProbabilties(0), 1e-6);
        
        assertFalse(data.getEntry(1).hasMeasuredProperty(0));
        assertArrayEquals(new double[]{0.45,0.55},
                data.getEntry(1).getPropertyClassProbabilties(0), 1e-6);
        
        assertEquals(1, data.getEntry(2).getMeasuredProperty(0), 1e-6);
        assertArrayEquals(new double[]{0.01,0.99},
                data.getEntry(2).getPropertyClassProbabilties(0), 1e-6);
        
        assertFalse(data.getEntry(3).hasMeasuredProperty(0));
        assertFalse(data.getEntry(3).hasPredictedProperty(0));
    }
    
    @Test
    public void testMultiPropertyDataset() throws Exception {
        // Make a dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.setClassNames(new String[]{"Yes", "No"});
        
        data.addEntry(new MultiPropertyEntry()); 
        data.addEntry(new MultiPropertyEntry()); 
        data.addEntry(new MultiPropertyEntry()); 
        data.addEntry(new MultiPropertyEntry()); 
        
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(0).setClassProbabilities(new double[]{1,0});
        
        data.getEntry(1).setClassProbabilities(new double[]{0.45,0.55});
        
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(2).setClassProbabilities(new double[]{0.01,0.99});
        
        // Make the modifier
        DiscreteToContinuousModifier mdfr = new DiscreteToContinuousModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("Yes");
        
        mdfr.setOptions(options);
        
        // Print usage
        System.out.println(mdfr.printUsage());
        
        // Test modifier
        mdfr.transform(data);
        
        assertEquals(1, data.NClasses());
        assertEquals(4, data.NEntries());
        assertEquals(0, data.NProperties());
        
        for (BaseEntry e : data.getEntries()) {
            assertEquals(0, ((MultiPropertyEntry) e).NProperties());
        }
        
        assertEquals(1, data.getEntry(0).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(0).getPredictedClass(), 1e-6);
        
        assertFalse(data.getEntry(1).hasMeasurement());
        assertEquals(0.45, data.getEntry(1).getPredictedClass(), 1e-6);
        
        assertEquals(0, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertEquals(0.01, data.getEntry(2).getPredictedClass(), 1e-6);
        
        assertFalse(data.getEntry(3).hasMeasurement());
        assertFalse(data.getEntry(3).hasPrediction());
    }
    
}
