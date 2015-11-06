package magpie.data.utilities.modifiers;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author Logan Ward
 */
public class AddPropertyModifierTest {

    @Test
    public void test() throws Exception {
        // Create a fake dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addEntry(new MultiPropertyEntry());
        
        // Create filter, set options
        AddPropertyModifier mdfr = new AddPropertyModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("prop1");
        options.add("prop2");
        
        mdfr.setOptions(options);
        
        // Test usage output
        System.out.println(mdfr.printUsage());
        
        // Run modifier
        mdfr.transform(data);
        
        // Test results
        assertEquals(2, data.NProperties());
        assertTrue(ArrayUtils.contains(data.getPropertyNames(), "prop1"));
        assertTrue(ArrayUtils.contains(data.getPropertyNames(), "prop2"));
        for (BaseEntry ptr : data.getEntries()) {
            assertEquals(2, ((MultiPropertyEntry) ptr).NProperties());
        }
    }
    
}
