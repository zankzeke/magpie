package magpie.data.utilities.splitters;

import java.io.File;
import java.util.*;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for a base splitter
 * @author Logan Ward
 */
public class BaseDatasetSplitterTest {
    
    @Test
    public void testCommands() throws Exception {
        CompositionDataset data = (CompositionDataset) getDataset();
        
        BaseDatasetSplitter sptr = getSplitter();
        
        // Run train command
        List<Object> cmd = new LinkedList<>();
        cmd.add("train");
        cmd.add(data);
        sptr.runCommand(cmd);
        
        // Generate get command
        cmd.clear();
        cmd.add("get");
        cmd.add("1");
        cmd.add(data);
        sptr.runCommand(cmd);
        
        // Generate save command
        cmd.clear();
        cmd.add("save");
        cmd.add(data);
        cmd.add("splittest");
        cmd.add("stats");
        sptr.runCommand(cmd);
        assertTrue(new File("splittest0.csv").isFile());
        for (int i=0; i<sptr.getSplitNames().size(); i++) {
            new File("splittest" + i + ".csv").delete();
        }
    }
    
    @Test
    public void testSplit() throws Exception {
        // Get splitter and dataset
        Dataset data = getDataset();
        BaseDatasetSplitter spltr = getSplitter();
        
        // Compute labels
        spltr.train(data);
        int[] labels = spltr.label(data);
        
        // Store a clone of the dataset
        Dataset clone = data.clone();
        
        // Split the original set, and make sure it is split according to the labels
        List<Dataset> splits = spltr.split(data);
        assertEquals(0, data.NEntries());
        for (int e=0; e<clone.NEntries(); e++) {
            assertTrue(splits.get(labels[e]).containsEntry(clone.getEntry(e)));
        }
    }

    protected BaseDatasetSplitter getSplitter() throws Exception {
        // Get an example splitter
        AllMetalsSplitter sptr = new AllMetalsSplitter();
        return sptr;
    }

    protected Dataset getDataset() throws Exception {
        // Load in dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        
        // Compute attribute
        data.addElementalProperty("Number");
        data.generateAttributes();
        
        return data;
    }

    @Test
    public void testDetails() throws Exception {
        CompositionDataset data = (CompositionDataset) getDataset();
        BaseDatasetSplitter spltr = getSplitter();
        
        // Make sure splitter is trained
        spltr.train(data);
        
        // Print out results
        System.out.println("Splitter Details:");
        System.out.println(spltr.printDescription(false));
        System.out.println("Splitter Details - HTML:");
        System.out.println(spltr.printDescription(true));
        
        // Print out split names
        List<String> splitNames = spltr.getSplitNames();
        List<Dataset> splts = spltr.split(data);
        assertEquals("Number of splits and names don't match up", splitNames.size(), 
                splts.size());
        List<String> command = new LinkedList<>();
        command.add("names");
        System.out.println(spltr.printCommand(command));
    }
}
