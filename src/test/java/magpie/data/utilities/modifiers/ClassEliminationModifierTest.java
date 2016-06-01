package magpie.data.utilities.modifiers;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyEntry;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ClassEliminationModifierTest {

    @Test
    public void testDelete() throws Exception {
        // Create a fake dataset
        Dataset data = new Dataset();
        data.setClassNames(new String[]{"Yes", "Maybe", "No"});
        
        data.addEntry(new BaseEntry()); // Neither measured nor predicted
        data.addEntry(new BaseEntry()); // Measured "Yes"
        data.addEntry(new BaseEntry()); // Measured "Maybe"
        data.addEntry(new BaseEntry()); // Measured "No"
        
        data.getEntry(1).setMeasuredClass(0);
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(3).setMeasuredClass(2);
        
        // Create filter, set options
        ClassEliminationModifier mdfr = new ClassEliminationModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("Maybe");
        
        mdfr.setOptions(options);
        
        // Test usage output
        System.out.println(mdfr.printUsage());
        
        // Run modifier
        mdfr.transform(data);
        
        // Test results
        assertEquals(2, data.NClasses());
        assertArrayEquals(new String[]{"Yes", "No"}, data.getClassNames());
        assertEquals(3, data.NEntries());
        assertFalse(data.getEntry(0).hasMeasurement());
        assertEquals(0, data.getEntry(1).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(2).getMeasuredClass(), 1e-6);
    }
    
    @Test
    public void testChange() throws Exception {
        // Create a fake dataset
        Dataset data = new Dataset();
        data.setClassNames(new String[]{"Yes", "Maybe", "No"});
        
        data.addEntry(new BaseEntry()); // Neither measured nor predicted
        data.addEntry(new BaseEntry()); // Measured "Yes"
        data.addEntry(new BaseEntry()); // Measured "Maybe"
        data.addEntry(new BaseEntry()); // Measured "No"
        
        data.getEntry(1).setMeasuredClass(0);
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(3).setMeasuredClass(2);
        
        // Create filter, set options
        ClassEliminationModifier mdfr = new ClassEliminationModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("Maybe");
        options.add("No");
        
        mdfr.setOptions(options);
        
        // Run modifier
        mdfr.transform(data);
        
        // Test results
        assertEquals(2, data.NClasses());
        assertArrayEquals(new String[]{"Yes", "No"}, data.getClassNames());
        assertEquals(4, data.NEntries());
        assertFalse(data.getEntry(0).hasMeasurement());
        assertEquals(0, data.getEntry(1).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(2).getMeasuredClass(), 1e-6);
        assertEquals(1, data.getEntry(3).getMeasuredClass(), 1e-6);
    }
    
}
