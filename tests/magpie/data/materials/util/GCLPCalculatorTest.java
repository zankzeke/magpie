package magpie.data.materials.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class GCLPCalculatorTest {

    @Test
    public void testInitialization() throws Exception {
        // Create it
        GCLPCalculator calc = new GCLPCalculator();
        int nElem = calc.numPhases();
        assertEquals(LookupData.ElementNames.length, nElem);
        
        // Add in NaCl
        CompositionEntry NaCl = new CompositionEntry("NaCl");
        calc.addPhase(NaCl, -1);
        assertEquals(1 + nElem, calc.numPhases());
        
        // Add in a duplicate
        calc.addPhase(NaCl, -1);
        assertEquals(1 + nElem, calc.numPhases());
        
        // See if energy is updated
        calc.addPhase(NaCl, 0);
        assertEquals(-1, calc.Phases.get(NaCl), 1e-6);
        calc.addPhase(NaCl, -2);
        assertEquals(-2, calc.Phases.get(NaCl), 1e-6);
        
        // Add many phases
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        calc.addPhases(data);
    }
    
    @Test
    public void testGCLP() throws Exception {
        // Create it
        GCLPCalculator calc = new GCLPCalculator();
        int nElem = calc.numPhases();
        assertEquals(LookupData.ElementNames.length, nElem);
        
        // Simple test: No phases
        CompositionEntry NaCl = new CompositionEntry("NaCl");
        calc.doGCLP(NaCl);
        assertEquals(0.0, calc.getGroundStateEnergy(), 1e-6);
        assertEquals(2, calc.getPhaseEquilibria().size());
        
        // Add in Na2Cl and NaCl2 to map
        calc.addPhase(new CompositionEntry("Na2Cl"), -1);
        calc.addPhase(new CompositionEntry("NaCl2"), -1);
        calc.doGCLP(NaCl);
        assertEquals(-1, calc.getGroundStateEnergy(), 1e-6);
        assertEquals(2, calc.getPhaseEquilibria().size());
        
        // Add NaCl to map
        calc.addPhase(NaCl, -2);
        calc.doGCLP(NaCl);
        assertEquals(-2, calc.getGroundStateEnergy(), 1e-6);
        assertEquals(1, calc.getPhaseEquilibria().size());
        
        // Test for complex case: AlNiFeZrTiSiBrFOSeKHHe
        if (Files.isReadable(Paths.get("big-datasets/oqmd-hull.energies"))) {
            calc = new GCLPCalculator();
            CompositionDataset hullData = new CompositionDataset();
            hullData.importText("big-datasets/oqmd-hull.energies", null);
            hullData.setTargetProperty("delta_e", false);
            calc.addPhases(hullData);
            calc.doGCLP(new CompositionEntry("AlNiFeZrTiSiBrFOSeKHHe"));
            assertEquals(10, calc.getPhaseEquilibria().size());
            assertEquals(-1.553, calc.getGroundStateEnergy(), 1e-2);
        }
    }
}
