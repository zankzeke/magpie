
package magpie.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import magpie.Magpie;
import magpie.attributes.expanders.CrossExpander;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.modifiers.NonZeroClassModifier;
import magpie.data.utilities.output.SimpleOutput;
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
    public void testRemoveDuplicates() throws Exception {
        Dataset data = new Dataset();
        
        // Make a dataset with a duplicate
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addAttribute("x", new double[]{2,2,1});
        data.setMeasuredClasses(new double[]{0,1,2});
        
        // Get collapsed list of all entries
        Map<BaseEntry,List<BaseEntry>> unique = data.getUniqueEntries();
        assertEquals(2, unique.size());
        assertEquals(2, unique.get(data.getEntry(0)).size());
        assertEquals(1, unique.get(data.getEntry(2)).size());
        
        // Get collapsed list of duplicates
        Map<BaseEntry,List<BaseEntry>> dups = data.getDuplicates();
        assertEquals(1, dups.size());
        assertEquals(2, dups.get(data.getEntry(0)).size());
        assertFalse(dups.containsKey(data.getEntry(2)));
        
        // Remove duplicates
        Dataset originalData = data.clone();
        data.removeDuplicates();
        
        assertEquals(2, data.NEntries());
        assertEquals(2, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(1, data.getEntry(1).getAttribute(0), 1e-6);
        
        // Resolve duplicates via averagings
        List<Object> cmd = new ArrayList<>();
        cmd.add("duplicates");
        cmd.add("AveragingDuplicateResolver");
        
        data = originalData;
        data.runCommand(cmd);
        assertEquals(2, data.NEntries());
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
    public void testDeleteClass() {
        // Create fake dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.getEntry(0).setMeasuredClass(-1);
        data.getEntry(2).setMeasuredClass(-3);
        data.getEntry(1).setPredictedClass(-1);
        
        // Test clear measured
        data.deleteMeasuredClasses();
        assertFalse(data.getEntry(0).hasMeasurement());
        assertFalse(data.getEntry(1).hasMeasurement());
        assertFalse(data.getEntry(2).hasMeasurement());
        assertFalse(data.getEntry(0).hasPrediction());
        assertTrue(data.getEntry(1).hasPrediction());
        assertFalse(data.getEntry(2).hasPrediction());
        
        // Test clear predicted
        data.getEntry(0).setMeasuredClass(-1);
        data.deletePredictedClasses();
        assertTrue(data.getEntry(0).hasMeasurement());
        assertFalse(data.getEntry(1).hasMeasurement());
        assertFalse(data.getEntry(2).hasMeasurement());
        assertFalse(data.getEntry(0).hasPrediction());
        assertFalse(data.getEntry(1).hasPrediction());
        assertFalse(data.getEntry(2).hasPrediction());
    }
    
    @Test
    public void testOutput() throws Exception {
        // Make a simple dataset
        Dataset data = getEasyDataset();
        
        // Write a CSV file to disk
        File file = new File(data.saveCommand("test", "csv"));
        assertEquals("test.csv", file.getName());
        file.deleteOnExit();
        
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertEquals("X,Y,Class", fp.readLine());
        fp.close();
        
        // Write an ARFF file to disk
        file = new File(data.saveCommand("test", "arff"));
        assertEquals("test.arff", file.getName());
        file.deleteOnExit();
        
        fp = new BufferedReader(new FileReader(file));
        assertTrue(fp.readLine().startsWith("@RELATION"));
        fp.close();
        
        // Write class values to disk
        file = new File(data.saveCommand("test", "stats"));
        assertEquals("test.csv", file.getName());
        file.deleteOnExit();
        
        fp = new BufferedReader(new FileReader(file));
        assertTrue(fp.readLine().startsWith("Entry,Measured,Predicted"));
        fp.close();
    }
    
    @Test
    public void testPrintRank() throws Exception {
        // Make a simple dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x,1", new double[]{0,1,2});
        data.addAttribute("x,2", new double[]{1,0,2});
        
        data.setMeasuredClasses(new double[]{1,2,1.5});
        data.setPredictedClasses(new double[]{2,1,1.5});
        
        // Print with ranking on measured class, maximizing
        List<Object> command = new LinkedList<>();
        command.add("rank");
        command.add(1);
        command.add("minimize");
        command.add("measured");
        command.add("TargetEntryRanker");
        command.add(1);
        
        System.out.println("\tRanked based on minimizing measured distance from 1");
        data.runCommand(command);
        
        // Print with ranking on measured class, maximizing
        command.set(2, "maximize");
        
        System.out.println("\tRanked based on maximizing measured distance from 1");
        data.runCommand(command);
        
        // Print with ranking on predicted class, maximizing
        command.set(3, "predicted");
        
        System.out.println("\tRanked based on maximizing predicted distance from 1");
        data.runCommand(command);
        
        // Print with ranking on measured class, maximizing. Print all
        command.set(1, 2);
        
        System.out.println("\tRanked based on maximizing predicted distance from 1");
        data.runCommand(command);
    }
    
    @Test
    public void testPrintCommands() throws Exception {
        // Create a dataset with something interesting about it
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        data.setTargetProperty("bandgap", true);
        data.generateAttributes();
        
        NonZeroClassModifier mdfr = new NonZeroClassModifier();
        mdfr.transform(data);
        
        // Print with details about the class
        List<String> command = new LinkedList<>();
        System.out.println("\tPrint: details");
        command.add("details");
        String output = data.printCommand(command);
        System.out.println(output);
        
        // Print distribution of class variable
        command.set(0, "dist");
        System.out.println("\tPrint: dist");
        output = data.printCommand(command);
        System.out.println(output);
        
        // Print description
        command.set(0, "description");
        System.out.println("\tPrint: description");
        output = data.printCommand(command);
        System.out.println(output);
    }
}
