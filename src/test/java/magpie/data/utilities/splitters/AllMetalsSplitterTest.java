package magpie.data.utilities.splitters;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class AllMetalsSplitterTest extends BaseDatasetSplitterTest {

    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        return super.getSplitter(); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Test
    public void testSplit() throws Exception {
        // Make a fake dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Na");
        data.addEntry("Cl");
        data.addEntry("FeAl");
        
        // Run filter
        AllMetalsSplitter spltr = new AllMetalsSplitter();
        
        int[] label = spltr.label(data);
        
        assertArrayEquals(new int[]{1,0,1,0}, label);
    }

}
