package magpie.data.materials.util;

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
    }    
}
