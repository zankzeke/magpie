package magpie.data.materials;

import java.io.File;
import java.io.PrintWriter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ElementDatasetTest {
    
    @Test
    public void testClone() throws Exception {
        // Create a blank dataset
        ElementDataset data = new ElementDataset();
        data.addElementalPropertySet("general");
        data.addEntry("Fe");
        
        // Make an empty clone
        ElementDataset clone = data.emptyClone();
        assertEquals(0, clone.NEntries());
        clone.removeElementalProperty("AtomicNumber");
        assertNotEquals(data.getElementalProperties().size(), 
                clone.getElementalProperties().size());
        
        // Make a regular clone
        clone = (ElementDataset) data.clone();
        assertEquals(data.NEntries(), clone.NEntries());
    }
        
    @Test
    public void testImport() throws Exception {
        // Create a sample input file
        File dataFile = File.createTempFile("magpie", "data");
        PrintWriter fo = new PrintWriter(dataFile);
        fo.println("element sln_energy valency{2+,3+,4+}");
        fo.println("Al\t0.78 3+");
        fo.println("Ni None 2+");
        fo.println("Fe 0.39 None");
        fo.close();
        
        // Read it in
        ElementDataset data = new ElementDataset();
        data.importText(dataFile.getCanonicalPath(), null);
        
        // Test results
        assertEquals(2, data.NProperties());
        assertArrayEquals("sln_energy valency".split(" "), data.getPropertyNames());
        assertArrayEquals("2+,3+,4+".split(","), data.getPropertyClasses("valency"));
        
        assertEquals(3, data.NEntries());
        assertEquals("Al", data.getEntry(0).toString());
        assertEquals(0.78, data.getEntry(0).getMeasuredProperty(0), 1e-5);
        assertEquals(0, data.getEntry(1).getMeasuredProperty(1), 1e-5);
        assertFalse(data.getEntry(2).hasMeasuredProperty(1));        
    }
    
    @Test
    public void testAttributes() throws Exception {
        ElementDataset data = new ElementDataset();
        data.addElementalPropertySet("general");
        
        // Compute attributes
        data.generateAttributes();
        assertEquals(data.getElementalProperties().size(), data.NAttributes());
    }
}
