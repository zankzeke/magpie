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
public class UserSpecifiedAttributeSelectorTest {

    @Test
    public void test() throws Exception {
        // Make a fake dataset
        Dataset data = new Dataset();
        data.addAttribute("x1", new double[0]);
        data.addAttribute("x2", new double[0]);
        data.addAttribute("y", new double[0]);

        // Make the Regex selector
        UserSpecifiedAttributeSelector sel = new UserSpecifiedAttributeSelector();
        
        List<Object> options = new LinkedList<>();
        options.add("x1");
        options.add("y");
        
        sel.setOptions(options);
        
        // Run the selector
        sel.train(data);
        
        assertEquals(2, sel.getSelections().size());
        assertTrue(sel.getSelectionNames().contains("x1"));
        assertTrue(sel.getSelectionNames().contains("y"));
        
        System.out.println(sel.printDescription(true));
    }
}
