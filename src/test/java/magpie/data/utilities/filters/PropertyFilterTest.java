package magpie.data.utilities.filters;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import magpie.models.regression.WekaRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PropertyFilterTest {

    @Test
    public void test() throws Exception {
        // Load in the materials example dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        
        data.setTargetProperty("delta_e", false);
        
        // Train and run model
        WekaRegression model = new WekaRegression();
        
        model.train(data);
        
        // Make the filter
        PropertyFilter filter = new PropertyFilter();
        
        // Test #1: Predicted
        List<Object> options = new ArrayList<>();
        options.add("predicted");
        options.add("delta_e");
        options.add("<");
        options.add(0);
        
        filter.setOptions(options);
        
        boolean[] res = filter.label(data);
        for (int e=0; e<data.NEntries(); e++) {
            assertTrue(res[e] == (data.getEntry(e).getPredictedClass() < 0));
        }
        
        // Test #2: Measured
        options = new ArrayList<>();
        options.add("measured");
        options.add("delta_e");
        options.add(">");
        options.add(0);
        
        filter.setOptions(options);
        
        res = filter.label(data);
        for (int e=0; e<data.NEntries(); e++) {
            assertTrue(res[e] == (data.getEntry(e).getMeasuredClass() > 0));
        }
    }
    
}
