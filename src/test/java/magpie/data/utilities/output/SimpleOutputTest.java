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
public class SimpleOutputTest {

    @Test
    public void test() throws Exception {
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
        List<Object> options = new LinkedList<>();
        options.add("TargetEntryRanker");
        options.add(1);
        options.add("minimize");
        options.add("measured");
        options.add(1);
        
        SimpleOutput output = new SimpleOutput();
        output.setOptions(options);
        
        File file = new File("temp.file");
        file.deleteOnExit();
        System.out.println("\tRanked based on minimizing measured distance from 1");
        output.writeDataset(data, System.out);
        output.writeDataset(data, file);
        
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertTrue(fp.readLine().toLowerCase().startsWith("rank"));
        assertTrue(fp.readLine().toLowerCase().contains("(0.000,1.000)"));
        assertTrue(fp.readLine() == null);
        fp.close();
        
        // Print with ranking on measured class, maximizing
        options.set(2, "maximize");
        
        output.setOptions(options);
        
        System.out.println("\tRanked based on maximizing measured distance from 1");
        output.writeDataset(data, System.out);
        output.writeDataset(data, file);
        
        fp = new BufferedReader(new FileReader(file));
        assertTrue(fp.readLine().toLowerCase().startsWith("rank"));
        assertTrue(fp.readLine().toLowerCase().contains("(1.000,0.000)"));
        assertTrue(fp.readLine() == null);
        fp.close();
        
        // Print with ranking on predicted class, maximizing
        options.set(3, "predicted");
        
        output.setOptions(options);
        
        System.out.println("\tRanked based on maximizing predicted distance from 1");
        output.writeDataset(data, System.out);
        output.writeDataset(data, file);
        
        fp = new BufferedReader(new FileReader(file));
        assertTrue(fp.readLine().toLowerCase().startsWith("rank"));
        assertTrue(fp.readLine().toLowerCase().contains("(0.000,1.000)"));
        assertTrue(fp.readLine() == null);
        fp.close();
        
        // Print with ranking on measured class, maximizing. Print all
        options.set(1, "all");
        
        output.setOptions(options);
        
        System.out.println("\tRanked based on maximizing predicted distance from 1");
        output.writeDataset(data, System.out);
        output.writeDataset(data, file);
        
        fp = new BufferedReader(new FileReader(file));
        assertTrue(fp.readLine().toLowerCase().startsWith("rank"));
        assertTrue(fp.readLine().toLowerCase().contains("(0.000,1.000)"));
        assertTrue(fp.readLine().toLowerCase().contains("(2.000,2.000)"));
        assertTrue(fp.readLine().toLowerCase().contains("(1.000,0.000)"));
        assertTrue(fp.readLine() == null);
        fp.close();
        
        // Now, print all without ranking
        options.clear();
        
        output.setOptions(options);
        
        System.out.println("\tPrinting all");
        output.writeDataset(data, System.out);
        output.writeDataset(data, file);
        
        fp = new BufferedReader(new FileReader(file));
        assertFalse(fp.readLine().toLowerCase().startsWith("rank"));
        assertTrue(fp.readLine().toLowerCase().contains("(0.000,1.000)"));
        assertTrue(fp.readLine().toLowerCase().contains("(1.000,0.000)"));
        assertTrue(fp.readLine().toLowerCase().contains("(2.000,2.000)"));
        assertTrue(fp.readLine() == null);
        fp.close();
    }
    
}
