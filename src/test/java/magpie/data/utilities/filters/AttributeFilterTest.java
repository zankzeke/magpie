package magpie.data.utilities.filters;

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
public class AttributeFilterTest {

    @Test
    public void test() throws Exception {
        // Test dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addAttribute("x", new double[]{0,1,2});
        data.addAttribute("y", new double[]{2,1,0});
        
        // Make filter
        AttributeFilter filter = new AttributeFilter();
        
        // Test equality
        List<Object> options = new ArrayList<>(3);
        options.add("x");
        options.add("==");
        options.add(1);
        
        filter.setOptions(options);
        
        boolean[] label = filter.label(data);
        assertFalse(label[0]);
        assertTrue(label[1]);
        assertFalse(label[2]);
        
        // Test not equals
        options.set(1, "<>");
        
        filter.setOptions(options);
        
        label = filter.label(data);
        assertTrue(label[0]);
        assertFalse(label[1]);
        assertTrue(label[2]);
        
        // Test gt
        options.set(1, ">");
        
        filter.setOptions(options);
        
        label = filter.label(data);
        assertFalse(label[0]);
        assertFalse(label[1]);
        assertTrue(label[2]);
        
        // Test gt
        options.set(1, ">");
        
        filter.setOptions(options);
        
        label = filter.label(data);
        assertFalse(label[0]);
        assertFalse(label[1]);
        assertTrue(label[2]);
        
        // Test gt
        options.set(1, "ge");
        
        filter.setOptions(options);
        
        label = filter.label(data);
        assertFalse(label[0]);
        assertTrue(label[1]);
        assertTrue(label[2]);
        
        // Test less than
        options.set(0, "y");
        options.set(1, "lt");
        
        filter.setOptions(options);
        
        label = filter.label(data);
        assertFalse(label[0]);
        assertFalse(label[1]);
        assertTrue(label[2]);
        
        // Test less than or equals
        options.set(1, "le");
        
        filter.setOptions(options);
        
        label = filter.label(data);
        assertFalse(label[0]);
        assertTrue(label[1]);
        assertTrue(label[2]);
    }
    
}
