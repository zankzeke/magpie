package magpie.data.utilities.filters;

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
public class EntryRankerFilterTest {

    @Test
    public void test() throws Exception {
        // Create test set
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.setMeasuredClasses(new double[]{0,1,2});
        data.setPredictedClasses(new double[]{2,1,0});
        
        // Create the filter
        EntryRankerFilter filter = new EntryRankerFilter();
        
        // Test #1
        List<Object> options = new LinkedList<>();
        options.add(2);
        options.add("maximum");
        options.add("measured");
        options.add("SimpleEntryRanker");
        
        filter.setOptions(options);
        
        boolean[] res = filter.label(data);
        
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertTrue(res[2]);
        
        // Test #2: 
        options = new LinkedList<>();
        options.add(2);
        options.add("minimum");
        options.add("predicted");
        options.add("SimpleEntryRanker");
        
        filter.setOptions(options);
        
        res = filter.label(data);
        
        assertFalse(res[0]);
        assertTrue(res[1]);
        assertTrue(res[2]);
    }
    
}
