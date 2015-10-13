package magpie.data.utilities.modifiers;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class NaNToMeanModifierTest {

    @Test
    public void test() {
        // Create dataset
        Dataset data = new Dataset();
        data.setAttributeNames(Arrays.asList(new String[]{"x", "y"}));
        BaseEntry entry1, entry2, entry3;
        entry1 = new BaseEntry();
        entry1.setAttributes(new double[]{1.0, 2.0});
        data.addEntry(entry1);
        entry2 = new BaseEntry();
        entry2.setAttributes(new double[]{Double.NaN, 3.0});
        data.addEntry(entry2);
        entry3 = new BaseEntry();
        entry3.setAttributes(new double[]{2.0, Double.POSITIVE_INFINITY});
        data.addEntry(entry3);
        
        // Run the modifier
        NaNToMeanModifier mdfr = new NaNToMeanModifier();
        mdfr.modifyDataset(data);
        
        // Check results
        assertEquals(1.5, data.getEntry(1).getAttribute(0), 1e-6);
        assertEquals(2.5, data.getEntry(2).getAttribute(1), 1e-6);
    }
    
}
