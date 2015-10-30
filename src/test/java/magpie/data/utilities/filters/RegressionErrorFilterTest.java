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
public class RegressionErrorFilterTest {

    @Test
    public void test() throws Exception {
        // Spoof a dataset after running a model
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.setMeasuredClasses(new double[]{0,0,0});
        data.setPredictedClasses(new double[]{0,0.5,0});
        
        // Make filter
        RegressionErrorFilter filter = new RegressionErrorFilter();
        
        List<Object> options = new LinkedList<>();
        options.add(0.25);
        
        filter.setOptions(options);
        
        // Test
        boolean[] res = filter.label(data);
        
        assertTrue(res[0]);
        assertFalse(res[1]);
        assertTrue(res[2]);
    }
    
}
