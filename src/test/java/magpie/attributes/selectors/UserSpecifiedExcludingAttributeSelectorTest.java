package magpie.attributes.selectors;

import java.util.*;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class UserSpecifiedExcludingAttributeSelectorTest {

    @Test
    public void test() throws Exception {
        // Load in dataset
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.txt", null);
        
        // Create attribue selector
        UserSpecifiedExcludingAttributeSelector selector = 
                new UserSpecifiedExcludingAttributeSelector();
        List<String> attrs = new LinkedList<>();
        attrs.add("x");
        selector.selectAttributes(attrs);
        
        
        // Run it
        selector.train(data);
        selector.run(data);
        
        // Test result
        assertEquals(1, data.NAttributes());
        assertEquals("y", data.getAttributeNames()[0]);
        
        System.out.println(selector.printDescription(true));
    }
    
}
