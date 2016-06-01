package magpie.data.utilities.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
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
public class PropertiesOutputTest {
    
    @Test
    public void test() throws Exception {
        // Make a simple dataset
        MultiPropertyDataset data = new MultiPropertyDataset();
        data.addProperty("prop,mine");
        data.addEntry(new MultiPropertyEntry());
        data.addEntry(new MultiPropertyEntry());
        
        data.addAttribute("x,1", new double[]{0,1});
        data.addAttribute("x,2", new double[]{1,0});
        
        data.getEntry(0).setMeasuredClass(1.0);
        data.getEntry(0).setPredictedProperty(0, -1);
        
        data.getEntry(1).setPredictedClass(1.0);
        data.getEntry(1).setMeasuredProperty(0, -1);
        
        // Write a CSV file
        PropertiesOutput output = new PropertiesOutput();
        output.setOptions(new LinkedList<>());
        
        // Make output file
        File file = new File("temp.csv");
        file.deleteOnExit();
        
        // Write file
        output.writeDataset(data, file);
        
        // Make sure it looks as expected
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertEquals("x-1,x-2,class_measured,class_predicted,prop-mine_measured,prop-mine_predicted", fp.readLine());
        assertEquals("0.0,1.0,1.0,None,None,-1.0", fp.readLine());
        assertEquals("1.0,0.0,None,1.0,-1.0,None", fp.readLine());
        fp.close();
    }
    
}
