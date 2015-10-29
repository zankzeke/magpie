package magpie.data.utilities.filters;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.modifiers.NonZeroClassModifier;
import magpie.models.classification.WekaClassifier;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ClassProbabilityFilterTest {

    @Test
    public void test() throws Exception {
        // Load in composition dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        
        // Make it a metal/nonmetal problem
        data.setTargetProperty("bandgap", false);
        data.generateAttributes();
        NonZeroClassModifier mdfr = new NonZeroClassModifier();
        mdfr.transform(data);
        data.setClassNames(new String[]{"Metal", "NonMetal"});
        
        // Create a simple classification model
        WekaClassifier clfr = new WekaClassifier("trees.RandomTree", null);
        clfr.train(data);
        clfr.run(data);
        
        // Store a backup copy
        CompositionDataset backup = (CompositionDataset) data.clone();
        
        // Test out the filtering: Goal, leave only predicted metals
        ClassProbabilityFilter fltr = new ClassProbabilityFilter();
        fltr.setClassName("Metal");
        fltr.setThreshold(0.8);
        fltr.setExclude(false);
        fltr.filter(data);
        
        // Test results
        assertEquals(0, data.getEntry(0).getPredictedClass(), 1e-6);
        for (int e=0; e<data.NEntries(); e++) {
            assertTrue(data.getEntry(e).getClassProbilities()[0] > 0.8);
        }
        
        // Test out the filtering: Exclude high prob nonmetals
        List<Object> options = new ArrayList<>();
        options.add("NonMetal");
        options.add("0.8");
        fltr.setOptions(options);
        
        data = (CompositionDataset) backup.clone();
        
        fltr.setExclude(true);
        fltr.filter(data);
        
        // Test results
        assertEquals(0, data.getEntry(0).getPredictedClass(), 1e-6);
        for (int e=0; e<data.NEntries(); e++) {
            assertTrue(data.getEntry(e).getClassProbilities()[1] < 0.8);
        }

    }
    
}
