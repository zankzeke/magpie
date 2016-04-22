package magpie.data.utilities.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.util.LookupData;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionOutputTest {
    
    public CompositionDataset makeDataset() throws Exception {
        CompositionDataset data = new CompositionDataset();
        data.addProperty("energy");
        
        data.addEntry("AlNi");
        data.addEntry("AlFe");
        data.addEntry("Al3Fe");
        data.addEntry("Al");
        
        data.getEntry(0).setMeasuredClass(-1);
        data.getEntry(1).setPredictedClass(1);
        data.getEntry(2).setMeasuredProperty(0, -2);
        data.getEntry(3).setPredictedProperty(0, 2);
        
        return data;
    }

    @Test
    public void testPrintAll() throws Exception {
        CompositionDataset data = makeDataset();
        
        // Make the file
        File file = new File("test.comp");
        file.deleteOnExit();
        
        // Make the printer
        CompositionOutput output = new CompositionOutput();
        
        List<Object> options = new ArrayList<>();
        options.add("-all");
        
        output.setOptions(options);
        
        // Print the results
        output.writeDataset(data, file);
        
        // Check the results
        BufferedReader fp = new BufferedReader(new FileReader(file));
        for (int i=0; i<5; i++) {
            assertEquals(LookupData.ElementNames.length+4, fp.readLine().split(",").length);
        }
        assertNull(fp.readLine());
        fp.close();
    }
    
    @Test
    public void testPrintSpecified() throws Exception {
        CompositionDataset data = makeDataset();
        
        // Make the file
        File file = new File("test.comp");
        file.deleteOnExit();
        
        // Make the printer
        CompositionOutput output = new CompositionOutput();
        
        List<Object> options = new ArrayList<>();
        options.add("-list");
        options.add("Al");
        options.add("Ni");
        
        output.setOptions(options);
        
        // Print the results
        output.writeDataset(data, file);
        
        // Check the results
        BufferedReader fp = new BufferedReader(new FileReader(file));        
        assertEquals("X_Al,X_Ni,class_measured,class_predicted,energy_measured,energy_predicted",
                fp.readLine());
        assertEquals("0.5,0.5,-1.0,None,None,None", fp.readLine());
        assertEquals("0.5,0.0,None,1.0,None,None", fp.readLine());
        assertEquals("0.75,0.0,None,None,-2.0,None", fp.readLine());
        assertEquals("1.0,0.0,None,None,None,2.0", fp.readLine());
        assertNull(fp.readLine());
        fp.close();
    }
    
    @Test
    public void testPrintDataset() throws Exception {
        CompositionDataset data = makeDataset();
        CompositionDataset otherData = (CompositionDataset) data.clone();
        otherData.getEntriesWriteAccess().remove(0);
        
        // Make the file
        File file = new File("test.comp");
        file.deleteOnExit();
        
        // Make the printer
        CompositionOutput output = new CompositionOutput();
        
        List<Object> options = new ArrayList<>();
        options.add("-dataset");
        options.add(otherData);
        
        output.setOptions(options);
        
        // Print the results
        output.writeDataset(data, file);
        
        // Check the results
        BufferedReader fp = new BufferedReader(new FileReader(file));        
        assertEquals("X_Al,X_Fe,class_measured,class_predicted,energy_measured,energy_predicted",
                fp.readLine());
        assertEquals("0.5,0.0,-1.0,None,None,None", fp.readLine());
        assertEquals("0.5,0.5,None,1.0,None,None", fp.readLine());
        assertEquals("0.75,0.25,None,None,-2.0,None", fp.readLine());
        assertEquals("1.0,0.0,None,None,None,2.0", fp.readLine());
        assertNull(fp.readLine());
        fp.close();
    }
    
    @Test
    public void testPrintDynamic() throws Exception {
        CompositionDataset data = makeDataset();
        
        // Make the file
        File file = new File("test.comp");
        file.deleteOnExit();
        
        // Make the printer
        CompositionOutput output = new CompositionOutput();
        
        List<Object> options = new ArrayList<>();
        options.add("-dynamic");
        
        output.setOptions(options);
        
        output.addElements(Arrays.asList(1,2,3));
        assertEquals(3, output.ElementsToPrint.size());
        
        // Print the results
        output.writeDataset(data, file);
        
        // Check the results
        BufferedReader fp = new BufferedReader(new FileReader(file));        
        assertEquals("X_Al,X_Fe,X_Ni,class_measured,class_predicted,energy_measured,energy_predicted",
                fp.readLine());
        assertEquals("0.5,0.0,0.5,-1.0,None,None,None", fp.readLine());
        assertEquals("0.5,0.5,0.0,None,1.0,None,None", fp.readLine());
        assertEquals("0.75,0.25,0.0,None,None,-2.0,None", fp.readLine());
        assertEquals("1.0,0.0,0.0,None,None,None,2.0", fp.readLine());
        assertNull(fp.readLine());
        fp.close();
    }
}
