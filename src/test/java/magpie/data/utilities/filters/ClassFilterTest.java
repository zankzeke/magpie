package magpie.data.utilities.filters;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan
 */
public class ClassFilterTest {
    
    @Test
    public void test() throws Exception {
        // Make a fake dataset with 4 entries
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.setMeasuredClasses(new double[]{0,1,2,3});
        data.setPredictedClasses(new double[]{3,2,1,0});
        
        // Run a test using the Java interface
        ClassFilter fltr = new ClassFilter();
        fltr.setComparisonOperator(">");
        fltr.setUseMeasured(true);
        fltr.setThreshold(2);
        
        boolean[] res = fltr.label(data);
        assertFalse(res[0]);
        assertFalse(res[1]);
        assertFalse(res[2]);
        assertTrue(res[3]);
        
        // Run test using text interface
        List<Object> options = new ArrayList<>();
        options.add("predicted");
        options.add("==");
        options.add("3");
        fltr.setOptions(options);
        
        res = fltr.label(data);
        assertTrue(res[0]);
        assertFalse(res[1]);
        assertFalse(res[2]);
        assertFalse(res[3]);
    }
    
}
