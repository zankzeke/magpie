package magpie.data.materials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.*;
import vassal.io.VASP5IO;

/**
 *
 * @author Logan Ward
 */
public class CrystalStructureDatasetTest {

    @Test
    public void testAttributeGeneration() throws Exception {
        // Create dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        // Create entry
        Cell strc = new Cell();
        strc.setBasis(new double[]{3.52, 3.52, 3.52}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.setTypeName(0, "Cu");
        CrystalStructureEntry entry = new CrystalStructureEntry(strc, "Cu", null);
        
        // Add entry to dataset
        data.addEntry(entry);
        
        // Generate attributes
        data.addElementalProperty("Electronegativity");
        data.generateAttributes();
        
        // Make sure everything matches
        assertEquals(data.NAttributes(), data.getEntry(0).NAttributes());
    }
    
    @Test
    public void testAddEntry() throws Exception {
        // Create dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        // Parse entry
        String input = "Rh Mg Co Si\n"
                + " 1.0\n"
                + "0.000000 3.763641 3.763641\n"
                + "3.763641 0.000000 3.763641\n"
                + "3.763641 3.763641 0.000000\n"
                + "Co Mg Rh Si\n"
                + "1 1 1 1\n"
                + "direct\n"
                + " 0.7500000000 0.7500000000 0.7500000000\n"
                + " 0.5000000000 0.5000000000 0.5000000000\n"
                + " 0.2500000000 0.2500000000 0.2500000000\n"
                + " 0.0000000000 0.0000000000 0.0000000000";
        CrystalStructureEntry entry = data.addEntry(input);
        
        // Check out results
        assertEquals(4, entry.getStructure().nAtoms());
        assertEquals(4, entry.getStructure().nTypes());
        assertEquals("Rh Mg Co Si", entry.getName());
    }
    
    @Test
    public void testSimilarity() throws Exception {
        // Create dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        // Create primitive cell for B2-AlNi
        Cell strc = new Cell();
        strc.setBasis(new double[]{2.88, 2.88, 2.88}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Al");
        strc.setTypeName(1, "Ni");
        CrystalStructureEntry entry = new CrystalStructureEntry(strc, "Primtivie", null);
        data.addEntry(entry);
        
        // Create scaled cell
        strc = new Cell();
        strc.setBasis(new double[]{3.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Al");
        strc.setTypeName(1, "Ni");
        entry = new CrystalStructureEntry(strc, "Scaled", null);
        data.addEntry(entry);
        
        // Create a cell where A & B are swapped
        strc = new Cell();
        strc.setBasis(new double[]{3.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Ni");
        strc.setTypeName(1, "Al");
        entry = new CrystalStructureEntry(strc, "Primtivie", null);
        data.addEntry(entry);
        
        // Create a 2x1x1 supercell
        strc = new Cell();
        strc.setBasis(new double[]{6.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.25,0.5,0.5}, 1));
        strc.addAtom(new Atom(new double[]{0.75,0.5,0.5}, 1));
        strc.setTypeName(0, "Ni");
        strc.setTypeName(1, "Al");
        entry = new CrystalStructureEntry(strc, "Primtivie", null);
        data.addEntry(entry);
        
        // Generate attributes
        data.generateAttributes();
        
        // Make sure scaling doesn't effect it
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a) + " fails scaling",
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(1).getAttribute(a),
                    1e-6);
        }
        
        // Make sure its permutationally-invariant
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a) + " fails permutation",
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(2).getAttribute(a),
                    1e-6);
        }
        
        // Make sure it passes supercell
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a) + " fails supercell",
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(3).getAttribute(a),
                    1e-6);
        }
    }
    
    @Test
    public void testPOSCAROutput() throws Exception {
        // Create dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        // Create entry
        Cell strc = new Cell();
        strc.setBasis(new double[]{3.52, 3.52, 3.52}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.setTypeName(0, "Cu");
        CrystalStructureEntry entry = new CrystalStructureEntry(strc, "Cu", null);
        
        // Add entry to dataset
        data.addEntry(entry);

        // Save the properties
        File output = new File("test-csd-output");

        // Call the save command
        data.saveCommand("test-csd-output", "poscar");
        
        // Tests
        assertTrue(output.isDirectory());
        assertTrue(new File(output, "properties.txt").isFile());
        
        // Make sure properties.txt has 2 lines
        BufferedReader fp = new BufferedReader(new FileReader(new File(output, "properties.txt")));
        String temp = fp.readLine();
        int nLines = 0;
        while (temp != null) {
            temp = fp.readLine();
            nLines++;
        }
        assertEquals(2, nLines);
        fp.close();
        
        FileUtils.deleteDirectory(output);
    }

    @Test
    public void testImport() throws Exception {
        CrystalStructureDataset data = new CrystalStructureDataset();

        // Read in the dataset
        data.importText("datasets/icsd-sample", null);

        // Make sure it read all 32 crystal structures
        assertEquals(32, data.NEntries());
        for (int i=0; i<32; i++) {
            assertTrue(data.getEntry(i).getName().startsWith(String.format("%d-", i)));
        }

        // Save the poscars
        File output = new File("test-csd-input");
        try {
            data.saveCommand("test-csd-input", "poscar");

            // Write another file to that directory
            VASP5IO io = new VASP5IO();
            io.writeStructureToFile(data.getEntry(0).getStructure(), "test-csd-input/test.vasp");

            // Read in that directory, make sure it gets 33 files
            CrystalStructureDataset newData = (CrystalStructureDataset) data.emptyClone();
            newData.importText("test-csd-input", null);
            assertEquals(33, newData.NEntries());
            assertTrue(newData.getEntry(0).getName().contains("0-B1Ho5Si3"));
        } finally {
            FileUtils.deleteDirectory(output);
        }
    }
}
