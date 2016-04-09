package magpie.data.utilities.output;

import java.io.File;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ARFFOutputTest {
    
    public ARFFOutputTest() {
    }

    @Test
    public void test() throws Exception {
        // Make a simple dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x,1", new double[]{0,1});
        data.addAttribute("x,2", new double[]{1,0});
        
        data.getEntry(0).setMeasuredClass(1.0);
        
        // Write an ARFF file
        ARFFOutput output = new ARFFOutput();
        File file = new File("temp.arff");
        file.deleteOnExit();
        
        output.writeDataset(data, file);
        output.writeDataset(data, System.out);
        
        // Import it and check results
        Dataset dataCopy = new Dataset();
        dataCopy.importText(file.getAbsolutePath(), null);
        
        assertEquals(2, dataCopy.NAttributes());
        assertEquals(2, dataCopy.NEntries());
        assertArrayEquals(new double[]{0,1}, dataCopy.getEntry(0).getAttributes(), 1e-6);
        assertEquals(1.0, dataCopy.getEntry(0).getMeasuredClass(), 1e-6);
        assertArrayEquals(new double[]{1,0}, dataCopy.getEntry(1).getAttributes(), 1e-6);
        assertFalse(dataCopy.getEntry(1).hasMeasurement());
        
    }
    
}
