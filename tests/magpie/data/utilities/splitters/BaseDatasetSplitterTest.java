package magpie.data.utilities.splitters;

import java.io.File;
import java.util.*;
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
        // Load in dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        
        // Get an example splitter
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

}
