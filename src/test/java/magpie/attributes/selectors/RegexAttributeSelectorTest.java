package magpie.attributes.selectors;

import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class RegexAttributeSelectorTest {
    
    @Test
    public void test() throws Exception {
        // Make a fake dataset
        Dataset data = new Dataset();
        data.addAttribute("x1", new double[0]);
        data.addAttribute("x2", new double[0]);
        data.addAttribute("y", new double[0]);

        // Make the Regex selector
        RegexAttributeSelector sel = new RegexAttributeSelector();
        
        List<Object> options = new LinkedList<>();
        options.add("x[0-9]");
        
        sel.setOptions(options);
        
        // Run the selector
        sel.train(data);
        
        assertEquals(2, sel.getSelections().size());
        assertTrue(sel.getSelectionNames().contains("x1"));
        assertTrue(sel.getSelectionNames().contains("x2"));
        
        System.out.println(sel.printDescription(true));
        
        // Now, run it with exclusion
        options.add(0, "-v");
        
        sel.setOptions(options);
        
        // Run the selector
        sel.train(data);
        
        assertEquals(1, sel.getSelections().size());
        assertTrue(sel.getSelectionNames().contains("y"));
        
        System.out.println(sel.printDescription(true));
    }
    
}
