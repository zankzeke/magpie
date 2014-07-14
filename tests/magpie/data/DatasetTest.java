/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data;

import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

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
    public void testEntryAddition() {
        Dataset data = getEasyDataset();
        assertEquals("Attribute count wrong.", 2, data.NAttributes());
        assertEquals("Entry count wrong", 1, data.NEntries());
    }
    
    @Test
    public void testClone() {
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
    protected Dataset getEasyDataset() {
        Dataset data = new Dataset();
        List<String> attributeNames = new LinkedList<>();
        attributeNames.add("X");
        attributeNames.add("Y");
        data.setAttributeNames(attributeNames);
        data.addEntry("1.0, 1.0");
        return data;
    }
    
    
}
