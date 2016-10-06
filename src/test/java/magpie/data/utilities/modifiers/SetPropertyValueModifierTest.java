package magpie.data.utilities.modifiers;

import java.util.ArrayList;
import java.util.List;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class SetPropertyValueModifierTest {

    @Test
    public void testDiscrete() throws Exception {
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("prop");
        data.setPropertyClasses(0, new String[]{"Yes","No"});
        
        data.addEntry(new MultiPropertyEntry());
        
        // Create the modifier
        SetPropertyValueModifier mdfr = new SetPropertyValueModifier();
        
        List<Object> options = new ArrayList<>();
        options.add("prop");
        options.add("No");
        
        mdfr.setOptions(options);
        
        System.out.println(mdfr.printUsage());
        
        // Run the modifier
        mdfr.transform(data);
        
        assertEquals(1, data.getEntry(0).getPredictedProperty(0), 1e-6);
        assertEquals(1, data.getEntry(0).getMeasuredProperty(0), 1e-6);
        assertArrayEquals(new double[]{0,1}, 
                data.getEntry(0).getPropertyClassProbabilties(0), 1e-6);
    }
    
    @Test
    public void testContinuous() throws Exception {
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("prop");
        
        data.addEntry(new MultiPropertyEntry());
        
        // Create the modifier
        SetPropertyValueModifier mdfr = new SetPropertyValueModifier();
        
        mdfr.setPropertyValue("prop", 1.5);
        
        // Run the modifier
        mdfr.transform(data);
        
        assertEquals(1.5, data.getEntry(0).getPredictedProperty(0), 1e-6);
        assertEquals(1.5, data.getEntry(0).getMeasuredProperty(0), 1e-6);
    }
}
