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
 * @author Logan
 */
public class DelimitedClassOutputTest {
    
    @Test
    public void testContinuous() throws Exception {
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
        
        DelimitedClassOutput output = new DelimitedClassOutput();
        output.setOptions(options);
        
        // Make output file
        File file = new File("temp.csv");
        file.deleteOnExit();
        
        // Write file
        output.writeDataset(data, file);
        
        // Make sure it looks as expected
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertEquals("Entry,Measured,Predicted", fp.readLine());
        assertEquals("(0.000_1.000),1.0,None", fp.readLine());
        assertEquals("(1.000_0.000),None,None", fp.readLine());
        assertEquals(null, fp.readLine());
        fp.close();
        
        // Write a tab-delimited file
        options.set(0, "\\t");
        output.setOptions(options);
        
        output.writeDataset(data, file.getAbsolutePath());
        
        fp = new BufferedReader(new FileReader(file));
        assertEquals("Entry\tMeasured\tPredicted", fp.readLine());
        assertEquals("(0.000,1.000)\t1.0\tNone", fp.readLine());
        assertEquals("(1.000,0.000)\tNone\tNone", fp.readLine());
        assertEquals(null, fp.readLine());
        fp.close();
    }
    
    @Test
    public void testDiscrete() throws Exception {
         // Make a simple dataset
        Dataset data = new Dataset();
        data.setClassNames(new String[]{"A,1","B,1"});
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        
        data.addAttribute("x,1", new double[]{0,1});
        data.addAttribute("x,2", new double[]{1,0});
        
        data.getEntry(0).setMeasuredClass(1.0);
        data.getEntry(1).setClassProbabilities(new double[]{0.4,0.6});
        
        // Write a CSV file
        List<Object> options = new LinkedList<>();
        options.add(",");
        
        DelimitedClassOutput output = new DelimitedClassOutput();
        output.setOptions(options);
        
        // Make output file
        File file = new File("temp.csv");
        file.deleteOnExit();
        
        // Write file
        output.writeDataset(data, file);
        
        // Make sure it looks as expected
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertEquals("Entry,Measured,Predicted,P(A_1),P(B_1)", fp.readLine());
        assertEquals("(0.000_1.000),1.0,None,None,None", fp.readLine());
        assertEquals("(1.000_0.000),None,1.0,0.4,0.6", fp.readLine());
        assertEquals(null, fp.readLine());
        fp.close();
        
        // Write a tab-delimited file
        options.set(0, "\\t");
        output.setOptions(options);
        
        output.writeDataset(data, file.getAbsolutePath());
        
        fp = new BufferedReader(new FileReader(file));
        assertEquals("Entry\tMeasured\tPredicted\tP(A,1)\tP(B,1)", fp.readLine());
        assertEquals("(0.000,1.000)\t1.0\tNone\tNone\tNone", fp.readLine());
        assertEquals("(1.000,0.000)\tNone\t1.0\t0.4\t0.6", fp.readLine());
        assertEquals(null, fp.readLine());
        fp.close();
    }
    
}
