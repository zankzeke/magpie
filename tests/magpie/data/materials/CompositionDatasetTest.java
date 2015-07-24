package magpie.data.materials;

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
        data.generateAttributes();
        
        // Print description in HTML and normal format
        System.out.println("Plain Format");
        System.out.println(data.printDescription(false));
        System.out.println("HTML Format");
        System.out.println(data.printDescription(true));
    }
    
    @Test
    public void testImport() throws Exception {
        // Make a dataset
        CompositionDataset data = new CompositionDataset();
        
        // Load in dataset
        data.importText("datasets/small_set.txt", null);
        
        // Make sure everything looks fine
        assertEquals(612, data.NEntries());
        assertEquals(7, data.NProperties());
        assertArrayEquals(("bandgap energy_pa volume_pa magmom_pa"
                + " fermi hull_distance delta_e").split(" "),
                data.getPropertyNames());
    }
}
