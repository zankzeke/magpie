package magpie.attributes.evaluators;

import java.util.LinkedList;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PearsonsCorrelationEvaluatorTest {

    @Test
    public void test() throws Exception {
        // Make a fake dataset
        Dataset data = new Dataset();
        
        BaseEntry entry = new BaseEntry();
        entry.setMeasuredClass(2);
        entry.setPredictedClass(4);
        data.addEntry(entry);
        
        entry = new BaseEntry();
        entry.setMeasuredClass(10);
        entry.setPredictedClass(20);
        data.addEntry(entry);
        
        entry = new BaseEntry();
        entry.setMeasuredClass(14);
        entry.setPredictedClass(28);
        data.addEntry(entry);
        
        // Add attributes
        data.addAttribute("x", new double[]{0.1,0.5,0.7});
        data.addAttribute("y", new double[]{0,-1,5});
        
        // Run evaluator
        PearsonsCorrelationEvaluator eval = new PearsonsCorrelationEvaluator();
        double[] res = eval.evaluateAttributes(data);
        
        // Check results
        assertEquals(1, res[0], 1e-6);
        assertEquals(0.644902022, res[1], 1e-6);
        
        // Check rankings
        int[] rank = eval.getAttributeRanks(data);
        assertArrayEquals(new int[]{0,1}, rank);
        
        // Verify options
        eval.setOptions(new LinkedList<>());
        assertEquals("Usage: *No options*", eval.printUsage());
    }
    
}
