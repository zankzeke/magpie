package magpie.csp;

import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the DMSPEngine. Also includes some tests for the base class
 * @author Logan Ward
 */
public class DMSPEngineTest {
    
    @Test
    public void testImportCompounds() throws Exception {
        DMSPEngine csp = new DMSPEngine();
        csp.importKnownCompounds("datasets/prototypes.list");
    }
    
    @Test
    public void testGetTrainingSet() throws Exception {
        // Initilize the engine
        DMSPEngine csp = new DMSPEngine();
        csp.importKnownCompounds("datasets/prototypes.list");
        
        // Compute structure for Na5Pb2
        csp.getTrainingSet(new CompositionEntry("Na5Pb2"));
    }
    
    @Test
    public void testStructurePrediction() throws Exception {
        // Initialize the engine
        DMSPEngine csp = new DMSPEngine();
        csp.importKnownCompounds("datasets/prototypes.list");
        
        // Compute structure for Na5Pb2
        csp.predictStructure("Na5Pb2");
    }
    
    @Test
    public void testCrossValidation() throws Exception {
        // Initialize the engine
        DMSPEngine csp = new DMSPEngine();
        csp.importKnownCompounds("datasets/prototypes.list");
        
        // Cross-validate for binaries
        csp.crossvalidate(2, 10);
        
        // LOOCV for elements
        csp.crossvalidate(1, 0);
    }
    
}
