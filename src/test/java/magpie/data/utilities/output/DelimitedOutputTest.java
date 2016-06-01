package magpie.data.utilities.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class DelimitedOutputTest {

    @Test
    public void test() throws Exception {
        // Make a simple dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x,1", new double[]{0,1});
        data.addAttribute("x,2", new double[]{1,0});
        
        data.getEntry(0).setMeasuredClass(1.0);
        
        // Write a CSV file
        List<Object> options = new LinkedList<>();
        options.add(",");
        
        DelimitedOutput output = new DelimitedOutput();
        output.setOptions(options);
        
        // Make output file
        File file = new File("temp.csv");
        file.deleteOnExit();
        
        // Write file
        output.writeDataset(data, file.getAbsolutePath());
        
        // Make sure it looks as expected
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertEquals("x_1,x_2,Class", fp.readLine());
        assertEquals("0.0,1.0,1.0", fp.readLine());
        assertEquals("1.0,0.0,None", fp.readLine());
        fp.close();
        
        // Write a tab-delimited file
        options.set(0, "\\t");
        output.setOptions(options);
        
        output.writeDataset(data, file.getAbsolutePath());
        
        fp = new BufferedReader(new FileReader(file));
        assertEquals("x,1\tx,2\tClass", fp.readLine());
        assertEquals("0.0\t1.0\t1.0", fp.readLine());
        assertEquals("1.0\t0.0\tNone", fp.readLine());
        fp.close();
    }
    
}
