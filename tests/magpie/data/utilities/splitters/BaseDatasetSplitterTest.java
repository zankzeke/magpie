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
        CompositionDataset data = getDataset();
        
        AllMetalsSplitter sptr = new AllMetalsSplitter();
        
        // Generate get command
        List<Object> cmd = new LinkedList<>();
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
        for (int i : new int[]{0,1}) {
            new File("splittest" + i + ".csv").delete();
        }
    }

    protected BaseDatasetSplitter getSplitter() throws Exception {
        // Get an example splitter
        AllMetalsSplitter sptr = new AllMetalsSplitter();
        return sptr;
    }

    protected CompositionDataset getDataset() throws Exception {
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
        CompositionDataset data = getDataset();
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
