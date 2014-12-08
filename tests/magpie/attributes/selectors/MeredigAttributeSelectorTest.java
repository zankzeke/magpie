/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.attributes.selectors;

import magpie.data.materials.CompositionDataset;
import magpie.data.materials.util.PropertyLists;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MeredigAttributeSelectorTest {
    
    @Test
    public void test() throws Exception {
        // Generate the dataset and attributes
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.useCompositionAsAttributes(true);
        for (String prop : PropertyLists.getPropertySet("general")) {
            data.addElementalProperty(prop);
        }
        data.generateAttributes();
        
        // Run the filter
        MeredigAttributeSelector selector = new MeredigAttributeSelector();
        selector.train(data);
        selector.run(data);
        
        // Test results
        assertEquals(129, data.NAttributes());
    }
}
