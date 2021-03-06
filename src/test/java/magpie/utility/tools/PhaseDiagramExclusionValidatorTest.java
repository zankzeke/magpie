package magpie.utility.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.models.regression.GuessMeanRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author Logan Ward
 */
public class PhaseDiagramExclusionValidatorTest {

    @Test
    public void testEvaluate() throws Exception {
        // Make a dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Al");
        data.addEntry("Al2Fe");
        data.addEntry("AlNi");
        data.addEntry("AlFe");
        data.addEntry("AlZr");
        data.addEntry("AlNiZr");
        data.addEntry("AlFeZr");
        data.setMeasuredClasses(new double[]{0,1,2,3,4,5,6});
        data.generateAttributes();
        
        // Make a model
        GuessMeanRegression model = new GuessMeanRegression();
        
        // Test on binaries
        PhaseDiagramExclusionValidator val = new PhaseDiagramExclusionValidator();
        val.setNElements(2);
        Dataset output = val.evaluateModel(model, data);
        assertEquals(5, val.LastResults.size());
        
        // Make sure the results have no attributes, but the dataset does
        for (BaseEntry entry : data.getEntries()) {
            assertTrue(entry.NAttributes() > 0);
        }
        for (BaseEntry entry : output.getEntries()) {
            assertTrue(entry.NAttributes() == 0);
        }
        
        // Test on ternaries using command interface
        List<Object> options = new ArrayList<>();
        options.add("3");
        val.setOptions(options);
        assertEquals(3, val.NElements);
        
        options.clear();
        options.add("evaluate");
        options.add(model);
        options.add(data);
        val.runCommand(options);
        assertEquals(3, val.LastResults.size());
        
        // Print out results
        System.out.println(val.printLastResults());
        System.out.println(val.printCommand(Arrays.asList(new String[]{"last", "stats"})));
    }
    
}
