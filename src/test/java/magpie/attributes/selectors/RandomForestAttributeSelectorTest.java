package magpie.attributes.selectors;

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
public class RandomForestAttributeSelectorTest {
    
    @Test
    public void testRegression() throws Exception {
        Dataset data = makeTestDataset();
        
        // Set the class such it is "A^2 + 2B"
        for (BaseEntry entry : data.getEntries()) {
            entry.setMeasuredClass(entry.getAttribute(0) * entry.getAttribute(0)
                    + 2 * entry.getAttribute(1));
        }
        
        testSelector(data);
    }
    
    @Test
    public void testClassifier() throws Exception {
        Dataset data = makeTestDataset();
        
        // Set two classes
        data.setClassNames(new String[]{"Yes","No"});
        
        // Set the class such it is "A < 0.25, B < 0.5, or A + B < 0.55"
        for (BaseEntry entry : data.getEntries()) {
            entry.setMeasuredClass(entry.getAttribute(0) < 0.25
                    || entry.getAttribute(1) < 0.5
                    || entry.getAttribute(0) + entry.getAttribute(1) < 0.55 ?
                    0 : 1 );
        }
        
        testSelector(data);
    }

    /**
     * Given a dataset where the measured class is related to the first two 
     * variables, test the ability of the selector to find it.
     * @param data Dataset used for testing
     * @throws Exception 
     */
    protected void testSelector(Dataset data) throws Exception {
        // Create the selector
        RandomForestAttributeSelector sel = new RandomForestAttributeSelector();
        
        List<Object> options = new LinkedList<>();
        options.add("-num_attrs");
        options.add(2);
        options.add("-num_steps");
        options.add(3);
        options.add("-num_trees");
        options.add(50);
        options.add("-debug");
        
        sel.setOptions(options);
        
        // Test the selector
        sel.train(data);
        
        assertEquals(2, sel.getSelectionNames().size());
        assertTrue(sel.getSelections().contains(0));
        assertTrue(sel.getSelections().contains(1));
    }

    /**
     * Make a test dataset with 10 attributes and 100 entries
     * @return Example dataset with no measured class
     */
    protected Dataset makeTestDataset() {
        Dataset data = new Dataset();
        for (int a=0; a<10; a++) {
            data.addAttribute(new Character((char) (65+a)).toString(), new double[0]);
        }
        
        for (int i=0; i<100; i++) {
            BaseEntry entry = new BaseEntry();
            entry.setAttributes(new double[10]);
            
            for (int a=0; a<10; a++) {
                entry.setAttribute(a, Math.random());
            }
            
            data.addEntry(entry);
        }
        
        return data;
    }
    
}
