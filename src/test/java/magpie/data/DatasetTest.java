
package magpie.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.Magpie;
import magpie.attributes.expanders.CrossExpander;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.materials.CompositionDataset;
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
    public void testAttributeExpansion() throws Exception {
        // Make a sample dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x", new double[]{2});
        
        // Add attribute expander
        List<Object> command = new LinkedList<>();
        command.add("attributes");
        command.add("expanders");
        command.add("add");
        command.add("PowerExpander");
        command.add(2);
        
        data.runCommand(command);
        
        // Check that it was added
        assertEquals(1, data.getAttributeExpanders().size());
        
        // Run it
        command.set(2, "run");
        command.remove(4);
        command.remove(3);
        
        data.runCommand(command);
        
        assertEquals(2, data.NAttributes());
        
        // Clear list
        command.set(2, "clear");
        
        data.runCommand(command);
        
        assertTrue(data.getAttributeExpanders().isEmpty());
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
    public void testParallelGeneration() throws Exception {
        // Make a dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        
        // Compute attributes serially
        Magpie.NThreads = 1;
        data.generateAttributes();
        
        // Store result
        double[][] goldResult = data.getAttributeArray();
        
        // Compute them in parallel
        Magpie.NThreads = 2;
        data.clearAttributes();
        data.generateAttributes();
        
        // Store new result
        for (int e=0; e<data.NEntries(); e++) {
            assertArrayEquals(goldResult[e], data.getEntry(e).getAttributes(), 1e-6);
        }
    }
    
    @Test
    public void testAttributeGenerators() throws Exception {
        // Make a dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        
        // Clear attribute generators
        List<Object> command = new LinkedList<>();
        command.add("attributes");
        command.add("generators");
        command.add("clear");
        
        data.runCommand(command);
        
        assertTrue(data.getAttributeGenerators().isEmpty());
        
        // Add attribute generator via text interface
        command.set(2, "add");
        command.add("composition.StoichiometricAttributeGenerator");
        command.add("2");
        
        data.runCommand(command);
        
        // Make sure it worked
        List<BaseAttributeGenerator> gens = data.getAttributeGenerators();
        assertEquals(1, gens.size());
        
        // Add in an expander
        data.addAttribueExpander(new CrossExpander());
        
        // Run the command
        command.set(2, "run");
        command.remove(4);
        command.remove(3);
        
        // Run it
        data.runCommand(command);
        
        // Make sure it didn't run the expander
        assertEquals(2, data.NAttributes());
        
        // Run the generator
        command.remove(2);
        command.set(1, "generate");
        
        data.runCommand(command);
        
        // Make sure it called both the generator and expander
        assertEquals(3, data.NAttributes());
    }
    
    @Test
    public void testSetAttributes() {
        // Create dataset
        Dataset data = new Dataset();
        data.setAttributeNames(Arrays.asList(new String[]{"x","y"}));
        assertEquals(2, data.NAttributes());
        assertEquals("x", data.getAttributeName(0));
        assertArrayEquals(new String[]{"x","y"}, data.getAttributeNames());
    }
    
    @Test
    public void testSubtraction() {
        // Create two datasets with a terrible secret....
        //   they share an entry!
        Dataset data1 = new Dataset(), data2 = new Dataset();
        
        data1.addEntry(new BaseEntry());
        data1.addEntry(new BaseEntry());
        data1.addAttribute("x", new double[]{1,2});
        
        data2.addEntry(new BaseEntry());
        data2.addAttribute("x", new double[]{1});
        
        // Subtract 2 from 1
        data1.subtract(data2);
        
        // Test results
        assertEquals(1, data1.NEntries());
        assertEquals(2, data1.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(1, data2.NEntries());
    }
    
    @Test 
    public void testRemoveDuplicates() {
        Dataset data = new Dataset();
        
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addAttribute("x", new double[]{2,2});
        
        data.removeDuplicates();
        
        assertEquals(1, data.NEntries());
        assertEquals(2, data.getEntry(0).getAttribute(0), 1e-6);
    }
    
    @Test
    public void testCombine() {
        // Make an array of datasets
        Dataset[] datasets = new Dataset[2];

        datasets[0] = new Dataset();
        datasets[0].addEntry(new BaseEntry());
        datasets[0].addAttribute("x", new double[]{1});
        
        datasets[1] = new Dataset();
        datasets[1].addEntry(new BaseEntry());
        datasets[1].addAttribute("x", new double[]{2});
        
        // Combine them with another array
        Dataset result = new Dataset();
        result.addAttribute("x", new double[0]);
        
        //    With array interface
        result.combine(datasets);
        
        assertEquals(2, result.NEntries());
        assertTrue(result.containsEntry(datasets[0].getEntry(0)));
        assertTrue(result.containsEntry(datasets[1].getEntry(0)));
        
        //    With collection interface
        result.clearData();
        
        result.combine(Arrays.asList(datasets));
        
        assertEquals(2, result.NEntries());
        assertTrue(result.containsEntry(datasets[0].getEntry(0)));
        assertTrue(result.containsEntry(datasets[1].getEntry(0)));
    }
    
    @Test
    public void testPartition() throws Exception {
        // Make a dataset to be split
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x", new double[]{1,2});
        
        // Automatically determine number of splits
        Dataset[] result = data.partition(new int[]{1,2});
        
        assertEquals(3, result.length);
        assertEquals(0, result[0].NEntries());
        assertEquals(1, result[1].NEntries());
        assertEquals(1, result[2].NEntries());
        
        assertTrue(result[1].containsEntry(data.getEntry(0)));
        assertTrue(result[2].containsEntry(data.getEntry(1)));
        
        // User-specified number of splits
        result = data.partition(new int[]{0,2}, 4);
        
        assertEquals(4, result.length);
        assertEquals(1, result[0].NEntries());
        assertEquals(0, result[1].NEntries());
        assertEquals(1, result[2].NEntries());
        assertEquals(0, result[3].NEntries());
        
        assertTrue(result[0].containsEntry(data.getEntry(0)));
        assertTrue(result[2].containsEntry(data.getEntry(1)));
    }
    
    @Test
    public void testSubset() throws Exception {
        // Make dataset
        Dataset data = getEasyDataset();
        data.addEntry("0.0, 0.0");
        data.addEntry("1.0, 0.0");
        
        // Get a subset
        Dataset subset = data.getSubset(new int[]{0,2});
        
        // Test results
        assertEquals(3, data.NEntries());
        assertEquals(2, subset.NEntries());
        assertEquals(data.NAttributes(), subset.NAttributes());
        assertTrue(subset.containsEntry(data.getEntry(0)));
        assertTrue(subset.containsEntry(data.getEntry(2)));
        
        // Get a random subset with two entries, using command line interface
        List<Object> command = new LinkedList<>();
        command.add("subset");
        command.add(2);
        
        subset = (Dataset) data.runCommand(command);
        
        assertEquals(3, data.NEntries());
        assertEquals(2, subset.NEntries());
        assertEquals(data.NAttributes(), subset.NAttributes());
        
        // Get a random subset with 33% of the entries
        command.set(1, 1f/3);
        
        subset = (Dataset) data.runCommand(command);
        
        assertEquals(3, data.NEntries());
        assertEquals(1, subset.NEntries());
        assertEquals(data.NAttributes(), subset.NAttributes());
    }

    @Test
    public void testDataImport() throws Exception {
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.txt", null);
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
