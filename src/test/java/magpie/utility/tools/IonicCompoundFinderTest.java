package magpie.utility.tools;

import java.util.Arrays;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ward6_000
 */
public class IonicCompoundFinderTest {
    
    @Test
    public void testNaCl() throws Exception {
        IonicCompoundFinder finder = new IonicCompoundFinder();
        finder.setNominalComposition(new CompositionEntry("NaCl"));
        finder.setMaximumDistance(0.2);
        finder.setMaxFormulaUnitSize(4);
        
        // Make sure it only finds one 
        List<CompositionEntry> accepted = finder.findAllCompounds();
        assertEquals(1, accepted.size());
    }
    
    @Test
    public void testFeO() throws Exception {
        IonicCompoundFinder finder = new IonicCompoundFinder();
        finder.setNominalComposition(new CompositionEntry("Fe2O"));
        finder.setMaximumDistance(0.34);
        finder.setMaxFormulaUnitSize(5);
        
        // Make sure it only finds one (FeO)
        List<CompositionEntry> accepted = finder.findAllCompounds();
        assertEquals(1, accepted.size());
        
        // Make sure it finds two (FeO, Fe2O3)
        finder.setMaximumDistance(0.54);
        accepted = finder.findAllCompounds();
        assertEquals(2, accepted.size());
        assertEquals("FeO", accepted.get(0).toString());
    }
    
    @Test
    public void testNaBrCl() throws Exception {
        IonicCompoundFinder finder = new IonicCompoundFinder();
        finder.setNominalComposition(new CompositionEntry("Na2.1ClBr"));
        finder.setMaximumDistance(0.1);
        finder.setMaxFormulaUnitSize(5);
        
        // Make sure it only finds one (Na2ClBr)
        List<CompositionEntry> accepted = finder.findAllCompounds();
        assertEquals(1, accepted.size());
    }
    
    @Test
    public void testBa2As2S() throws Exception {
        IonicCompoundFinder finder = new IonicCompoundFinder();
        finder.setNominalComposition(new CompositionEntry("Ba2As2S"));
        finder.setMaximumDistance(0.35);
        finder.setMaxFormulaUnitSize(7);
        
        // Make sure it finds Ba4As2S
        List<CompositionEntry> accepted = finder.findAllCompounds();
        assertTrue(accepted.contains(new CompositionEntry("Ba4As2S")));
        
        // Check on the run
        CompositionDataset data = (CompositionDataset) 
                finder.runCommand(Arrays.asList(new Object[]{"run", "Ba2As2S", "0.35", "7"}));
        assertEquals(accepted.size(), data.NEntries());
    }
    
    @Test
    public void testCommand() throws Exception {
        IonicCompoundFinder finder = new IonicCompoundFinder();
        
        // Run command
        CompositionDataset data = (CompositionDataset) 
                finder.runCommand(Arrays.asList(new Object[]{"run", "Fe2O", "0.54", "5"}));
        assertEquals(2, data.NEntries());
    }
}
