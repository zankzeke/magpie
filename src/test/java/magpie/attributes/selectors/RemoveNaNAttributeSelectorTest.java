package magpie.attributes.selectors;

import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class RemoveNaNAttributeSelectorTest {

    @Test
    public void test() throws Exception {
        // Make a dataset with an NaN
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("w", new double[]{0,Double.NEGATIVE_INFINITY});
        data.addAttribute("x", new double[]{0,1,2});
        data.addAttribute("y", new double[]{0,Double.POSITIVE_INFINITY});
        data.addAttribute("z", new double[]{0,Double.NaN});
        
        // Make the selector
        RemoveNaNAttributeSelector sel = new RemoveNaNAttributeSelector();
        
        List<Object> options = new LinkedList<>();
        
        sel.setOptions(options);
        System.out.println(sel.printUsage());
        
        // Run the selector
        sel.train(data);
        sel.run(data);
        assertEquals(1, data.NAttributes());
        assertEquals("x", data.getAttributeName(0));
    }
    
}
