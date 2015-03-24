
package magpie.data;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import weka.core.Instances;

/**
 *
 * @author Logan Ward
 */
public class DatasetTest {
    
    public DatasetTest() {
    }

    @Test
    public void testDataImport() {
        Dataset data = new Dataset();
        try {
            data.importText("datasets/simple-data.csv", null);
        } catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
        assertTrue("No entries were imported.", data.NEntries() > 1);
    }
    
    @Test
    public void testEntryAddition() throws Exception {
        Dataset data = getEasyDataset();
        assertEquals("Attribute count wrong.", 2, data.NAttributes());
        assertEquals("Entry count wrong", 1, data.NEntries());
    }
    
    @Test
    public void testClone() throws Exception {
        Dataset data = getEasyDataset();
        Dataset clone = data.clone();
        
        assertTrue("Attribute count different", data.NAttributes() == clone.NAttributes());
        assertTrue("Entry count different", data.NEntries() == clone.NEntries());
        
        clone.clearData();
        assertFalse("Entry list not cloned properly", data.NEntries() == 0);
        
        List<String> newNames = new LinkedList<>();
        newNames.add("F");
        clone.setAttributeNames(newNames);
        assertFalse("Attribute name list not cloned properly", data.NAttributes() == 1);
    }

    /**
     * Generate a dataset with only one entry and two attributes.
     * @return Dataset with one entry
     */
    protected Dataset getEasyDataset() throws Exception {
        Dataset data = new Dataset();
        List<String> attributeNames = new LinkedList<>();
        attributeNames.add("X");
        attributeNames.add("Y");
        data.setAttributeNames(attributeNames);
        data.addEntry("1.0, 1.0");
        return data;
    }
    
    @Test
    public void testARFF() throws Exception {
        // Read it in
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.csv", null);
        
        // Write it out in ARFF format
        data.saveCommand("temp", "arff");
        
        // Read it back in
        Dataset dataARFF = new Dataset();
        dataARFF.importText("temp.arff", null);
        
        // Check that they are equal
        // assertArrayEquals(data.getClassNames(), dataARFF.getClassNames());
        assertArrayEquals(data.getAttributeNames(),
                dataARFF.getAttributeNames());
        assertArrayEquals(data.getEntry(0).getAttributes(), 
                dataARFF.getEntry(0).getAttributes(), 1e-6);
        
        // Delete files
        new File("temp.arff").deleteOnExit();
    }
    
    @Test
    public void testWekaTransfer() throws Exception {
        // Read data
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.csv", null);
        
        // Make a copy
        Dataset original = data.clone();
        
        // Transfer to weka format
        Instances weka = data.transferToWeka(true, false);
        assertEquals(0, data.getEntry(0).NAttributes());
        data.restoreAttributes(weka);
        assertEquals(original.NAttributes(), data.getEntry(0).NAttributes());
    }
    
}
