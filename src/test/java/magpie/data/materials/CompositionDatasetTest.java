package magpie.data.materials;

import java.util.LinkedList;
import java.util.List;
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
}
