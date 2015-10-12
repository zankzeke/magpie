package magpie.data.utilities.modifiers;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class StabilityCalculationModifierTest {

    @Test
    public void test() throws Exception {
        // Make fake potential data
        CompositionDataset known = new CompositionDataset();
        known.addEntry("NaCl");
        known.addProperty("energy_pa");
        known.getEntry(0).addProperty();
        known.getEntry(0).setMeasuredProperties(new double[]{-0.5});
        known.setTargetProperty("energy_pa", true);
        
        // Make fake dataset
        CompositionDataset test = new CompositionDataset();
        test.addEntry("NaCl3");
        test.addEntry("Na3Cl");
        test.addProperty("energy_pa");
        test.getEntry(0).addProperty(-0.2);
        test.getEntry(1).addProperty(0, -0.1);
        
        // Run the GCLP computation
        StabilityCalculationModifier mdfr = new StabilityCalculationModifier();
        mdfr.setEnergyName("energy_pa");
        mdfr.setStabilityName("stability");
        mdfr.setCompounds(known);
        mdfr.transform(test);
        
        // Test results
        assertEquals(2, test.NProperties());
        assertEquals(0.05, test.getEntry(0).getMeasuredProperty(1), 1e-6);
        assertEquals(0.15, test.getEntry(1).getPredictedProperty(1), 1e-6);
        assertFalse(test.getEntry(0).hasPredictedProperty(1));
        assertTrue(test.getEntry(1).hasMeasuredProperty(1));
        
        // Test running it a second time
        test.getEntry(0).setMeasuredProperty(0, 0);
        mdfr.transform(test);
        assertEquals(2, test.NProperties());
        assertEquals(0.25, test.getEntry(0).getMeasuredProperty(1), 1e-6);
        assertEquals(0.15, test.getEntry(1).getPredictedProperty(1), 1e-6);
        assertFalse(test.getEntry(0).hasPredictedProperty(1));
        assertTrue(test.getEntry(1).hasMeasuredProperty(1));
    }
    
}   
