package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class RemoveDuplicateAttributeSelectorTest {
    
    @Test
    public void testNoThreshold() throws Exception {
        Dataset data = makeDataset();
        
        // Make the attribute selector
        RemoveDuplicateAttributeSelector sel = new RemoveDuplicateAttributeSelector();
        
        sel.setOptions(new ArrayList<>());
        
        System.out.println(sel.printUsage());
        
        // Check the results
        sel.train(data);
        assertEquals(2, sel.getSelections().size());
        assertTrue(sel.getSelectionNames().contains("x"));
        assertTrue(sel.getSelectionNames().contains("z"));
    }
    
    @Test
    public void testThreshold() throws Exception {
        Dataset data = makeDataset();
        
        // Make the attribute selector
        RemoveDuplicateAttributeSelector sel = new RemoveDuplicateAttributeSelector();
        
        List<Object> options = new ArrayList<>();
        options.add("-tolerance");
        options.add("1e-6");
        
        sel.setOptions(options);
        
        // Check the results
        sel.train(data);
        assertEquals(2, sel.getSelections().size());
        assertTrue(sel.getSelectionNames().contains("x"));
        assertTrue(sel.getSelectionNames().contains("z"));
        
        // Now, purturb an attribute slightly
        data.getEntry(5).setAttribute(1, 
                data.getEntry(5).getAttribute(1) + 2e-6);
        
        // Check the results
        sel.train(data);
        assertEquals(3, sel.getSelections().size());
    }

    
    protected Dataset makeDataset() {
        Dataset data = new Dataset();
        data.addAttribute("x", new double[0]);
        data.addAttribute("y", new double[0]);
        data.addAttribute("z", new double[0]);
        for (int i=0; i<10; i++) {
            BaseEntry entry = new BaseEntry();
            entry.setAttributes(new double[]{i,i,2*i-1});
            data.addEntry(entry);
        }
        return data;
    }
    
}
