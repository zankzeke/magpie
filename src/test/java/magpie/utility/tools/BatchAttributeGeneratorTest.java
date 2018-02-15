package magpie.utility.tools;

import magpie.Magpie;
import magpie.attributes.generators.element.ElementalPropertyAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.ElementDataset;
import magpie.data.utilities.generators.CombinatorialSubstitutionGenerator;
import org.junit.Test;
import vassal.data.Atom;
import vassal.data.Cell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * @author Logan
 */
public class BatchAttributeGeneratorTest {
    
    @Test
    public void testDataset() throws Exception {
        // Make a simple dataset
        ElementDataset data = new ElementDataset();
        data.addEntry("H");
        data.addEntry("He");
        data.addEntry("Li");
        data.addEntry("Be");
        
        data.getEntry(0).setMeasuredClass(1);
        
        // Define attribute generator
        data.clearAttributeGenerators();
        data.addAttributeGenerator(new ElementalPropertyAttributeGenerator());
        
        data.addElementalProperty("Number");
        
        // Write out data in CSV format
        BatchAttributeGenerator gen = new BatchAttributeGenerator();
        
        List<Object> options = new LinkedList<>();
        options.add(1);
        options.add("DelimitedOutput");
        options.add(",");
        
        gen.setOptions(options);
        System.out.println(gen.printUsage());
        
        // Write dataset
        Magpie.NThreads = 2;
        
        options.clear();
        options.add("write");
        options.add("temp.csv");
        options.add(data);
        
        gen.runCommand(options);
        
        // Make sure the dataset was unaffected
        assertEquals(4, data.NEntries());
        assertEquals(0, data.NAttributes());
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(0, entry.NAttributes());
        }
        
        // Check the thread count 
        assertEquals(2, Magpie.NThreads);
        
        // Test output
        File file = new File("temp.csv");
        file.deleteOnExit();
        assertTrue(file.exists());
        
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertEquals("Number,Class", fp.readLine());
        for (int i=0; i<4; i++) {
            String line = fp.readLine();
            assertNotNull(line);
            String[] words = line.split(",");
            if (words[0].equals("1.0")) {
                assertEquals("1.0", words[1]);
            } else {
                assertEquals("None", words[1]);
            }
        }
        assertNull(fp.readLine());
        fp.close();
    }
    
    @Test
    public void testGenerator() throws Exception {
        // Make crystal structure generator
        CombinatorialSubstitutionGenerator entryGen = new CombinatorialSubstitutionGenerator();
        
        CrystalStructureDataset prot = new CrystalStructureDataset();
        Cell strc = new Cell();
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.setTypeName(0, "H");
        prot.addEntry(new CrystalStructureEntry(strc, "SC-H", null));
        
        List<Object> options = new ArrayList<>();
        options.add("-voro");
        options.add("-style");
        options.add("all");
        options.add(prot);
        options.add("H");
        options.add("He");
        options.add("Li");
        options.add("Be");
        
        entryGen.setOptions(options);
        
        // Make a dataset to compute attributes
        CrystalStructureDataset template = new CrystalStructureDataset();
        
        // Write out data in CSV format
        BatchAttributeGenerator gen = new BatchAttributeGenerator();
        
        options.clear();
        options.add(1);
        options.add("DelimitedOutput");
        options.add("\\t");
        
        gen.setOptions(options);
        
        // Write dataset
        Magpie.NThreads = 2;
        
        options.clear();
        options.add("write");
        options.add("temp.csv");
        options.add(template);
        options.add(entryGen);
        
        gen.runCommand(options);
        
        // Make sure the dataset was unaffected
        assertEquals(0, template.NEntries());
        assertEquals(0, template.NAttributes());
        
        // Check the thread count 
        assertEquals(2, Magpie.NThreads);
        
        // Test output
        File file = new File("temp.csv");
        file.deleteOnExit();
        assertTrue(file.exists());
        
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertNotNull(fp.readLine());
        for (int i=0; i<4; i++) {
            String line = fp.readLine();
            assertNotNull("No data on line: " + i, line);
        }
        assertNull(fp.readLine());
        fp.close();
    }
}
