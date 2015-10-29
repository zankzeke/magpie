package magpie.attributes.evaluators;

import java.util.ArrayList;
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
public class PolynomialFitEvaluatorTest {

    @Test
    public void test() throws Exception {
        Dataset data = new Dataset();
        
        BaseEntry entry = new BaseEntry();
        entry.setMeasuredClass(1);
        data.addEntry(entry);
        
        entry = new BaseEntry();
        entry.setMeasuredClass(4);
        data.addEntry(entry);
        
        entry = new BaseEntry();
        entry.setMeasuredClass(3);
        data.addEntry(entry);
        
        entry = new BaseEntry();
        entry.setMeasuredClass(-1);
        data.addEntry(entry);
        
        // Add attributes
        data.addAttribute("x", new double[]{0.1,0.3,0.8,1.1});
        data.addAttribute("y", new double[]{0.1,0.3,1.4,1.1});
        
        // Run evaluator
        PolynomialFitEvaluator eval = new PolynomialFitEvaluator();
        List<Object> options = new ArrayList<>();
        options.add(2);
        eval.setOptions(options);
        double[] res = eval.evaluateAttributes(data);
        
        // Check results
        assertEquals(0.284920647, res[0], 1e-6);
        assertEquals(1.796941874, res[1], 1e-6);
        
        // Check rankings
        int[] rank = eval.getAttributeRanks(data);
        assertArrayEquals(new int[]{0,1}, rank);
        
        // Verify options
        assertEquals("Usage: <order>", eval.printUsage());
    }
    
}
