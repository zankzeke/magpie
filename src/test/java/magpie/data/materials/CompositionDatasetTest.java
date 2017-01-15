package magpie.data.materials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.materials.util.LookupData;
import magpie.data.materials.util.PropertyLists;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionDatasetTest {
    
    @Test
    public void testDescription() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Fe2O3");
        data.addElementalProperty("Number");
        data.addElementalProperty("Electronegativity");
        
        // Generate attributes
        data.useCompositionAsAttributes(false);
        data.generateAttributes();
        
        // Print description in HTML and normal format
        System.out.println("Plain Format");
        System.out.println(data.printDescription(false));
        System.out.println("HTML Format");  
        System.out.println(data.printDescription(true));
        
        // Generate attributes w/ elemental fractions
        data.useCompositionAsAttributes(true);
        data.generateAttributes();
        System.out.println("Plain Format w/ Element fractions");
        System.out.println(data.printDescription(false));
        System.out.println("HTML Format w/ Element fractions");
        System.out.println(data.printDescription(true));
    }
    
    @Test
    public void testImport() throws Exception {
        // Make a dataset
        CompositionDataset data = new CompositionDataset();
        
        // Load in dataset
        data.importText("datasets/small_set.txt", null);
        
        // Make sure everything looks fine
        assertEquals(630, data.NEntries());
        assertEquals(7, data.NProperties());
        assertArrayEquals(("bandgap energy_pa volume_pa magmom_pa"
                + " fermi hull_distance delta_e").split(" "),
                data.getPropertyNames());
        
        // Get only the ground states
        
        //  Make sure our test case has duplicates
        CompositionEntry testEntry = new CompositionEntry("Na4Nb4O12");
        assertTrue(data.getEntriesWriteAccess().lastIndexOf(testEntry)
                != data.getEntriesWriteAccess().indexOf(testEntry));
        
        //  Run the duplicate screen
        List<Object> cmd = new LinkedList<>();
        cmd.add("duplicates");
        cmd.add("RankingDuplicateResolver");
        cmd.add("minimize");
        cmd.add("PropertyRanker");
        cmd.add("energy_pa");
        cmd.add("SimpleEntryRanker");
        data.runCommand(cmd);
        
        // Make sure the duplicate is gone, and we have the groundstate
        assertTrue(data.getEntriesWriteAccess().lastIndexOf(testEntry)
                == data.getEntriesWriteAccess().indexOf(testEntry));
        int entryID = data.getEntriesWriteAccess().indexOf(testEntry);
        assertEquals(2.328, data.getEntry(entryID).getMeasuredProperty(0), 1e-3);
    }
    
    @Test
    public void testOutput() throws Exception {
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        
        // Save it
        File file = new File(data.saveCommand("temp", "comp"));
        assertTrue(file.exists());
        BufferedReader fp = new BufferedReader(new FileReader(file));
        assertEquals("X_Na,X_Cl,class_measured,class_predicted", fp.readLine());
        fp.close();
        file.delete();
    }
    
    @Test
    public void testProperties() throws Exception {
        // Make a sample dataset
        CompositionDataset data = new CompositionDataset();
        
        // Add a set of properties
        data.addElementalPropertySet("general");
        
        // Check results
        assertEquals(PropertyLists.getPropertySet("general").length, 
                data.ElementalProperties.size());
        assertTrue(data.ElementalProperties.containsAll(
                Arrays.asList(PropertyLists.getPropertySet("general"))));
    }
    
    @Test
    public void testTemplate() throws Exception {
        // Make a dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        data.generateAttributes();
        
        // Save a template copy
        File file = new File(data.saveCommand("temp", "template"));
        assertTrue(file.isFile());
        
        // Make sure the connection to LookupData are broken
        CompositionDataset dataCopy = (CompositionDataset)
                CompositionDataset.loadState(file.getPath());
        assertNotSame(data.OxidationStates, dataCopy.OxidationStates);
        assertNotSame(data.ElementNames, dataCopy.ElementNames);
    }
    
    @Test
    public void testBinaryLookup() throws Exception {
        CompositionDataset data = new CompositionDataset();
        double[][] table = data.getPairPropertyLookupTable("B2Volume");
        assertEquals(11.113600, LookupData.readPairTable(table, "Nb", "O"), 1e-6);
        assertEquals(11.113600, LookupData.readPairTable(table, "O", "Nb"), 1e-6);
        assertEquals(30.408400, LookupData.readPairTable(table, "Ag", "Ac"), 1e-6);
    }
    
    @Test
    public void testManageProperties() throws Exception {
        CompositionDataset data = new CompositionDataset();
        
        // Test changing the directory
        assertEquals("./lookup-data", data.DataDirectory);
        
        List<Object> cmd = new LinkedList<>();
        cmd.add("attributes");
        cmd.add("properties");
        cmd.add("directory");
        cmd.add("lookup");
        cmd.add("data");
        
        data.runCommand(cmd);
        assertEquals("lookup data", data.DataDirectory);
        
        // Test adding a set of elemental propeties
        cmd.set(2, "add");
        cmd.set(3, "set");
        cmd.set(4, "radii");
        
        data.runCommand(cmd);
        assertEquals(PropertyLists.getPropertySet("radii").length,
                data.ElementalProperties.size());
        
        // Test adding a few more properties
        cmd.set(2, "add");
        cmd.set(3, "A");
        cmd.set(4, "B");
        
        data.runCommand(cmd);
        assertEquals(PropertyLists.getPropertySet("radii").length + 2,
                data.ElementalProperties.size());
        
        // Make sure it doesn't add things twice
        data.runCommand(cmd);
        assertEquals(PropertyLists.getPropertySet("radii").length + 2,
                data.ElementalProperties.size());
        
        // Test removal
        cmd.set(2, "remove");
        cmd.set(3, "B");
        cmd.remove(4);
        data.runCommand(cmd);
        assertEquals(PropertyLists.getPropertySet("radii").length + 1,
                data.ElementalProperties.size());
        assertFalse(data.ElementalProperties.contains("B"));
        
        // Test adding pair property
        cmd.set(2, "pair");
        cmd.set(3, "add");
        cmd.add("B");
        cmd.add("A");
        data.runCommand(cmd);
        assertEquals(2, data.ElementPairProperties.size());
        assertTrue(data.ElementPairProperties.contains("B"));
        
        // Test adding pair property
        cmd.set(3, "remove");
        cmd.remove(5);
        data.runCommand(cmd);
        assertEquals(1, data.ElementPairProperties.size());
        assertFalse(data.ElementPairProperties.contains("B"));
        
        // Make sure get operations do not allow write access
        List<String> temp = data.getElementalProperties();
        assertEquals(data.ElementalProperties, temp);
        temp.clear();
        assertFalse(data.ElementalProperties.isEmpty());
        
        temp = data.getElementPairProperties();
        assertEquals(data.ElementPairProperties, temp);
        temp.clear();
        assertFalse(data.ElementPairProperties.isEmpty());
    }
    
    @Test
    public void testGeneratePairProperty() throws Exception {
        CompositionDataset data = new CompositionDataset();
        
        // Make the ratio of atomic number
        List<Object> cmd = new LinkedList<>();
        cmd.add("attributes");
        cmd.add("properties");
        cmd.add("pair");
        cmd.add("add");
        cmd.add("difference");
        cmd.add("AtomicWeight");
        
        data.runCommand(cmd);
        
        assertEquals(1, data.PairPropertyData.size());
        assertTrue(data.PairPropertyData.containsKey("difference_AtomicWeight"));
        
        double[][] table = data.PairPropertyData.get("difference_AtomicWeight");
        double[] lookup = data.getPropertyLookupTable("AtomicWeight");
        assertEquals(Math.abs(lookup[14]-lookup[62]), 
                LookupData.readPairTable(table, 62, 14), 1e-6);
        
        // Same thing, but with ratios
        cmd.set(4, "ratio");
        data.runCommand(cmd);
        
        table = data.PairPropertyData.get("ratio_AtomicWeight");
        lookup = data.getPropertyLookupTable("AtomicWeight");
        assertEquals(Math.abs(lookup[14]/lookup[62]), 
                LookupData.readPairTable(table, 62, 14), 1e-6);
    }
}
