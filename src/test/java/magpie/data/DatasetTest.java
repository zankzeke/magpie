
package magpie.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.models.regression.WekaRegression;
import org.junit.Test;
import static org.junit.Assert.*;
import weka.core.Instances;

/**
 *
 * @author Logan Ward
 */
public class DatasetTest {
    
    @Test
    public void testClone() throws Exception {
        // Create a dataset with 1 attribute and 1 entry
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addAttribute("x", new double[]{1});
        
        // Make sure it has the correct number of attributes
        assertEquals(1, data.NEntries());
        assertEquals(1, data.NAttributes());
        
        // Make a clone
        Dataset clone = data.clone();
        
        // Make sure it has the same number of entries, etc
        assertEquals(1, data.NEntries());
        assertEquals(1, data.NAttributes());
        assertEquals(1, clone.NEntries());
        assertEquals(1, clone.NAttributes());
        
        // Add entry to original, make sure it doesn't affect clone
        data.addEntry(new BaseEntry());
        assertEquals(2, data.NEntries());
        assertEquals(1, data.NAttributes());
        assertEquals(1, clone.NEntries());
        assertEquals(1, clone.NAttributes());
        
        // Change attributes of the shared entry, make sure it doesn't affect clone
        data.getEntry(0).setAttribute(0, 10);
        assertEquals(1, clone.getEntry(0).getAttribute(0), 1e-6);
        
        // Make sure adding an attribute to clone doesn't affect base dataset
        clone.addAttribute("y", new double[]{2});
        assertEquals(2, data.NEntries());
        assertEquals(1, data.NAttributes());
        assertEquals(1, clone.NEntries());
        assertEquals(2, clone.NAttributes());
        
        // Make sure that a empty clone has no entries
        clone = data.emptyClone();
        assertEquals(2, data.NEntries());
        assertEquals(1, data.NAttributes());
        assertEquals(0, clone.NEntries());
        assertEquals(1, clone.NAttributes());
        
        // Add entry to clone, make sure original unaffected
        clone.addEntry(new BaseEntry());
        assertEquals(2, data.NEntries());
        assertEquals(1, data.NAttributes());
        assertEquals(1, clone.NEntries());
        assertEquals(1, clone.NAttributes());
        
        // Add attributes to original, make sure clone unaffected
        data.addAttributes(Arrays.asList(new String[]{"y","z"}));
        assertEquals(2, data.NEntries());
        assertEquals(3, data.NAttributes());
        assertEquals(1, clone.NEntries());
        assertEquals(1, clone.NAttributes());
    }
    
    @Test
    public void testGetTrainingSet() {
        // Make a fake dataset, 1 entry with measurement and 1 without
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.getEntry(0).setMeasuredClass(-1);
        
        // Get the trainset set
        Dataset trainData = data.getTrainingExamples();
        
        assertEquals(1, trainData.NEntries());
        assertTrue(trainData.getEntry(0).hasMeasurement());
        assertEquals(2, data.NEntries());
    }

    @Test
    public void testDataImport() {
        Dataset data = new Dataset();
        try {
            data.importText("datasets/simple-data.txt", null);
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
    public void testDatasetAddition() throws Exception {
        // Create datasets with the same attributes, class
        Dataset data1 = new Dataset();
        Dataset data2 = new Dataset();
        data1.addEntry(new BaseEntry());
        data2.addEntry(new BaseEntry());
        
        data1.addAttribute("x", new double[]{1});
        data2.addAttribute("x", new double[]{2});
        
        // Test addition
        data1.addEntries(data2, false);

        //   Via command-line interface
        List<Object> command = new ArrayList<>();
        command.add("add");
        command.add(data2);
        data1.runCommand(command);
        
        assertEquals(3, data1.NEntries());
        assertEquals(1, data2.NEntries());
        assertEquals(1, data1.NAttributes());
        assertEquals(1, data1.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(2, data1.getEntry(1).getAttribute(0), 1e-6);
        assertNotSame(data1.getEntry(1), data2.getEntry(0)); // Entry was cloned
        
        // Add another attribute to dataset 2, make sure merge fails
        boolean except = false;
        
        data2.addAttribute("y", new double[]{0});
        data2.getEntry(0).setMeasuredClass(-1);
        
        try {
            data1.addEntries(data2, false);
        } catch (Exception e) {
            except = true;
        }
        
        assertTrue(except);
        assertEquals(3, data1.NEntries());
        
        //    Via command-line interface
        except = false;
        try {
            data1.runCommand(command);
        } catch (Exception e) {
            except = true;
        }
        
        assertTrue(except);
        assertEquals(3, data1.NEntries());
        
        // Try again with force turned on
        data1.addEntries(data2, true);
        
        assertEquals(4, data1.NEntries());
        assertEquals(0, data1.getEntry(3).NAttributes()); // Attribute were deleted
        assertEquals(-1, data1.getEntry(3).getMeasuredClass(), 1e-6); // Class unaffected
        
        //    Via command-line interface
        command.add("-force");
        data1.runCommand(command);
        
        assertEquals(5, data1.NEntries());
        assertEquals(0, data1.getEntry(4).NAttributes()); // Attribute were deleted
        assertEquals(-1, data1.getEntry(4).getMeasuredClass(), 1e-6); // Class unaffected
        
        // Make data2 have two classes
        data2.setClassNames(new String[]{"Yes", "No"});
        
        data1.addEntries(data2, true);
        
        assertEquals(6, data1.NEntries());
        assertEquals(1, data1.NClasses());
        assertEquals(0, data1.getEntry(5).NAttributes()); // Attribute were deleted
        assertFalse(data1.getEntry(5).hasMeasurement()); // Class was deleted
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
        data.importText("datasets/simple-data.txt", null);
        
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
        data.importText("datasets/simple-data.txt", null);
        
        // Make a copy
        Dataset original = data.clone();
        
        // Transfer to weka format
        Instances weka = data.transferToWeka(true, false);
        assertEquals(0, data.getEntry(0).NAttributes());
        assertNotEquals(0, original.getEntry(0).NAttributes());
        data.restoreAttributes(weka);
        assertEquals(original.NAttributes(), data.getEntry(0).NAttributes());
        for (int i=0; i<data.NEntries(); i++) {
            assertArrayEquals(original.getEntry(i).getAttributes(),
                    data.getEntry(i).getAttributes(), 1e-6);
        }
        
        // Test with an actual Weka model
        WekaRegression model = new WekaRegression("trees.REPTree", null);
        model.crossValidate(10, data);
        for (int i=0; i<data.NEntries(); i++) {
            assertArrayEquals(original.getEntry(i).getAttributes(),
                    data.getEntry(i).getAttributes(), 1e-6);
        }
    }
    
    @Test
    public void testClearAttributes() throws Exception {
        // Import dataset
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.txt", null);
        
        // Test
        data.clearAttributes();
        assertEquals(0, data.NAttributes()); 
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(0, entry.NAttributes());
        }
    }
    
    @Test
    public void testMatch() throws Exception {
        // Create fake dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x", new double[]{0,1,2});
        
        // Create entry to be matched
        BaseEntry entry = new BaseEntry("0.9");
        
        // Test the matching
        List<BaseEntry> match = data.matchEntries(entry, 2);
        
        // Test results
        assertEquals(2, match.size());
        assertEquals(1, match.get(0).getAttribute(0), 1e-6);
        assertEquals(0, match.get(1).getAttribute(0), 1e-6);
        
        // Test through the command-line interface
        Dataset toMatch = data.clone();
        toMatch.addEntry(entry);
        
        //    Assemble command
        List<Object> cmd = new ArrayList<>();
        cmd.add("match");
        cmd.add(toMatch);
        cmd.add(2);
        
        //    Run command
        data.runCommand(cmd);
    }
    
    @Test
    public void testDeleteMeasurements() {
        // Create fake dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.getEntry(0).setMeasuredClass(-1);
        data.getEntry(2).setMeasuredClass(-3);
        data.getEntry(1).setPredictedClass(-1);
        
        // Test clear
        data.deleteMeasuredClasses();
        assertFalse(data.getEntry(0).hasMeasurement());
        assertFalse(data.getEntry(1).hasMeasurement());
        assertFalse(data.getEntry(2).hasMeasurement());
        assertFalse(data.getEntry(0).hasPrediction());
        assertTrue(data.getEntry(1).hasPrediction());
        assertFalse(data.getEntry(2).hasPrediction());
    }
}
