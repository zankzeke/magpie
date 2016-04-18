package magpie.data.utilities.modifiers.duplicates;

import java.util.LinkedList;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class AveragingDuplicateResolverTest {

    @Test
    public void testDatasetContinuous() {
        // Make a dataset
        Dataset data = new Dataset();
        for (int i=0; i<6; i++) {
            data.addEntry(new BaseEntry());
        }
        
        data.addAttribute("x", new double[]{0,1,1,2,2,3});
        
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(1).setMeasuredClass(1);
        data.getEntry(2).setMeasuredClass(2);
        data.getEntry(3).setMeasuredClass(3);
        Dataset originalData = data.clone();
        
        // Resolve it
        AveragingDuplicateResolver res = new AveragingDuplicateResolver();
        res.modifyDataset(data);
        
        assertEquals(4, data.NEntries());
        int entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(1));
        assertEquals(1.5, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(3));
        assertEquals(3, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(5));
        assertFalse(data.getEntry(entryID).hasMeasurement());
        
    }
    
    @Test
    public void testDatasetDiscrete() {
        // Make a dataset
        Dataset data = new Dataset();
        for (int i=0; i<7; i++) {
            data.addEntry(new BaseEntry());
        }
        
        data.addAttribute("x", new double[]{0,1,1,2,2,2,3});
        data.setClassNames(new String[]{"Yes", "No"});
        
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(1).setMeasuredClass(0);
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(3).setMeasuredClass(0);
        data.getEntry(4).setMeasuredClass(1);
        data.getEntry(5).setMeasuredClass(1);
        Dataset originalData = data.clone();
        
        // Resolve it
        AveragingDuplicateResolver res = new AveragingDuplicateResolver();
        res.modifyDataset(data);
        
        assertEquals(4, data.NEntries());
        int entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(1));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(3));
        assertEquals(1, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(6));
        assertFalse(data.getEntry(entryID).hasMeasurement());
        
    }
    
    @Test
    public void testMultiProperty() throws Exception {
        // Make a dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("Discrete", new String[]{"Yes", "No"});
        data.addProperty("Continuous");
        for (int i=0; i<7; i++) {
            data.addEntry(new MultiPropertyEntry());
        }
        
        data.addAttribute("x", new double[]{0,1,1,2,2,2,3});
        data.setClassNames(new String[]{"Yes", "No"});
        
        // Set the default class
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(1).setMeasuredClass(0);
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(3).setMeasuredClass(0);
        data.getEntry(4).setMeasuredClass(1);
        data.getEntry(5).setMeasuredClass(1);
        
        // Add a discrete property
        
        data.setTargetProperty(0, true);
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(1).setMeasuredClass(0);
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(3).setMeasuredClass(0);
        data.getEntry(4).setMeasuredClass(0);
        data.getEntry(5).setMeasuredClass(1);
        
        // Add a discrete property
        data.setTargetProperty(1, true);
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(1).setMeasuredClass(0);
        data.getEntry(2).setMeasuredClass(1);
        data.getEntry(3).setMeasuredClass(0);
        data.getEntry(4).setMeasuredClass(0);
        data.getEntry(5).setMeasuredClass(-3);
        
        
        // Create a backup
        data.setTargetProperty(0, true);
        MultiPropertyDataset originalData = (MultiPropertyDataset) data.clone();
        
        // Resolve it
        AveragingDuplicateResolver res = new AveragingDuplicateResolver();
        res.setOptions(new LinkedList<>());
        res.modifyDataset(data);
        
        // Test the basics
        assertEquals(4, data.NEntries());
        assertEquals(originalData.getTargetPropertyIndex(), data.getTargetPropertyIndex());
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(originalData.getTargetPropertyIndex(), 
                    ((MultiPropertyEntry) entry).getTargetProperty());
        }
        
        // Default class
        data.setTargetProperty(-1, true);
        int entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(1));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(3));
        assertEquals(1, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(6));
        assertFalse(data.getEntry(entryID).hasMeasurement());
        
        // Property 0
        data.setTargetProperty(0, true);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(1));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(3));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(6));
        assertFalse(data.getEntry(entryID).hasMeasurement());
        
        // Property 1
        data.setTargetProperty(1, true);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(1));
        assertEquals(0.5, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(3));
        assertEquals(-1, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(6));
        assertFalse(data.getEntry(entryID).hasMeasurement());
    }
}
