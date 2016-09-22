package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.List;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class StagedAttributeSelectorTest {
    
    @Test
    public void test() throws Exception {
        // Make a fake dataset 
        Dataset data = new Dataset();
        data.addAttribute("x1", new double[0]);
        data.addAttribute("x2", new double[0]);
        data.addAttribute("y", new double[0]);
        
        // Make the selectors to chain together
        UserSpecifiedAttributeSelector spec = new UserSpecifiedExcludingAttributeSelector();
        spec.addAttributeSpecification("x1");
        
        RegexAttributeSelector regex = new RegexAttributeSelector();
        regex.setRegex("x[0-9]");
        regex.setIncludeMatching(true);
        
        // Make the staged selector
        StagedAttributeSelector sel = new StagedAttributeSelector();
        
        List<Object> options = new ArrayList<>();
        options.add(spec);
        options.add(regex);
        
        sel.setOptions(options);
        
        // Run the selector
        sel.train(data);
        
        assertEquals(1, sel.getSelections().size());
        assertTrue(sel.getSelectionNames().contains("x1"));
        
        // Print the description
        System.out.println(sel.printDescription(true));
        System.out.println(sel.printDescription(false));
    }
 
    @Test
    public void testCitations() {
        StagedAttributeSelector selector = new StagedAttributeSelector();
        
        selector.addSelector(new LassoAttributeSelector());
        
        System.out.println(selector.getCitations());
    }
}
