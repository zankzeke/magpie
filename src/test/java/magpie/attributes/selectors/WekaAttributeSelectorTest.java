package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.util.PropertyLists;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class WekaAttributeSelectorTest {

    @Test
    public void test() throws Exception {
        // Generate the dataset and attributes
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        
        data.useCompositionAsAttributes(true);
        for (String prop : PropertyLists.getPropertySet("general")) {
            data.addElementalProperty(prop);
        }
        data.generateAttributes();
        data.setTargetProperty("delta_e", false);
        
        // Create the selector
        WekaAttributeSelector selector = new WekaAttributeSelector();
        
        List<Object> options = new ArrayList<>();
        options.add("-eval");
        options.add("weka.attributeSelection.CfsSubsetEval");
        options.add("-M");
        
        options.add("-search");
        options.add("BestFirst");
        options.add("-D");
        options.add("1");
        
        selector.setOptions(options);
        
        // Test it
        int originalCount = data.NAttributes();
        selector.train(data);
        assertEquals(originalCount, data.NAttributes());
        selector.run(data);
        assertNotEquals(originalCount, data.NAttributes());
        
        // Print description 
        System.out.println(selector.printDescription(true));
    }
}
