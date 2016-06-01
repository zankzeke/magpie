package magpie.data.utilities.filters;

import java.util.ArrayList;
import java.util.List;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class IQROutlierFilterTest {

    @Test
    public void test() throws Exception {
        // Read in the "simple" dataset
        Dataset data = new Dataset();
        
        data.importText("datasets/simple-data.txt", null);
        
        // Compute the IQR and Median of the measured class
        double iqr = StatUtils.percentile(data.getMeasuredClassArray(), 75) - 
                StatUtils.percentile(data.getMeasuredClassArray(), 25);
        double median = StatUtils.percentile(data.getMeasuredClassArray(), 50);
        
        // Create the filter with a small tolerance
        IQROutlierFilter filter = new IQROutlierFilter();
        
        List<Object> options = new ArrayList<>();
        options.add(0.5);
        
        filter.setOptions(options);
        
        filter.train(data);
        
        // Run the test
        boolean[] res = filter.label(data);
        
        for (int e=0; e<data.NEntries(); e++) {
            double x = data.getEntry(e).getMeasuredClass() - median;
            x = Math.abs(x / iqr);
            assertTrue(res[e] == (x > 0.5));
        }
    }
    
}
