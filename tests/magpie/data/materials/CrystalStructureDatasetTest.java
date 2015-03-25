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
    }
    
}
