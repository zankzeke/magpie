/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.attributes.selectors;

import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests of attribute selectors.
 * @author Logan Wards
 */
public class BaseAttributeSelectorTest {

    @Test
    public void testRegexAttributeSelector() {
        // Get data
        Dataset data = new Dataset();
        try {
            data.importText("datasets/simple-data.csv", null);
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Get only X
        Dataset clone = data.clone();
        RegexAttributeSelector regex = new RegexAttributeSelector();
        regex.setRegex("^x");
        regex.setIncludeMatching(true);
        regex.train(clone);
        regex.run(clone);
		assertEquals("Should only find one attribute", 1, clone.NAttributes());
		assertEquals("Should only have \"x\" as an attribute", "x", clone.getAttributeName(0));
		
		// Get only X
        clone = data.clone();
        regex.setIncludeMatching(false);
        regex.train(clone);
        regex.run(clone);
		assertEquals("Should only find one attribute", 1, clone.NAttributes());
		assertEquals("Should only have \"y\" as an attribute", "y", clone.getAttributeName(0));	
    }
    
}
