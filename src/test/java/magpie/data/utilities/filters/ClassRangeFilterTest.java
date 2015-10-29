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
public class ClassRangeFilterTest {

    @Test
    public void test() throws Exception {
        // Make a fake dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.setMeasuredClasses(new double[]{0,1,2});
        data.setPredictedClasses(new double[]{1,2,0});
        
        // Make the filter
        ClassRangeFilter filter = new ClassRangeFilter();
        
        // Test #1: Measured between 0.5 and 1.5
        List<Object> options = new ArrayList<>();
        options.add("measured");
        options.add("inside");
        options.add(0.5);
        options.add(1.5);
        
        filter.setOptions(options);
        
        boolean[] res = filter.label(data);
        
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertFalse(res[2]);
        
        // Test #2: Predicted outside 0.5 and 1.5
        options = new ArrayList<>();
        options.add("predicted");
        options.add("outside");
        options.add(0.5);
        options.add(1.5);
        
        filter.setOptions(options);
        
        res = filter.label(data);
        
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertTrue(res[2]);
    }
    
}
