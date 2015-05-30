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
}
