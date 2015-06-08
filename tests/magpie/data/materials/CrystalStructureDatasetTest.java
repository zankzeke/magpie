package magpie.data.materials;

import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.*;

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
        AtomicStructureEntry entry = new AtomicStructureEntry(strc, "Cu", null);
        
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
        AtomicStructureEntry entry = data.addEntry(input);
        
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
        AtomicStructureEntry entry = new AtomicStructureEntry(strc, "Primtivie", null);
        data.addEntry(entry);
        
        // Create scaled cell
        strc = new Cell();
        strc.setBasis(new double[]{3.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Al");
        strc.setTypeName(1, "Ni");
        entry = new AtomicStructureEntry(strc, "Scaled", null);
        data.addEntry(entry);
        
        // Create a cell where A & B are swapped
        strc = new Cell();
        strc.setBasis(new double[]{3.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Ni");
        strc.setTypeName(1, "Al");
        entry = new AtomicStructureEntry(strc, "Primtivie", null);
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
        entry = new AtomicStructureEntry(strc, "Primtivie", null);
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
}
