package magpie.data.utilities.modifiers;

import magpie.Magpie;
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

    @Test
    public void testParallel() throws Exception {
        // Read in the test dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        data.setTargetProperty("delta_e", false);

        // Compute stability with respect to this set
        StabilityCalculationModifier mdfr = new StabilityCalculationModifier();
        mdfr.setCompounds(data);
        mdfr.setEnergyName("delta_e");
        mdfr.setStabilityName("stability");
        mdfr.modifyDataset(data);

        // Run the code a few times to "burn" it in
        for (int i=0; i<100; i++) {
            mdfr.modifyDataset(data);
        }

        // Run in parallel

        //    First, clear the results (make sure that parallel actually does something)
        data.setTargetProperty("stability", true);
        data.setMeasuredClasses(new double[data.NEntries()]);

        //    Now, run it
        Magpie.NThreads = 2;
        long startTime = System.nanoTime();
        mdfr.modifyDataset(data);
        long finishTime = System.nanoTime();
        long parallelTime = finishTime - startTime;
        System.out.format("Parallel time elapsed: %d us\n", parallelTime / 1000 );
        Magpie.NThreads = 1; // Return to serial (in case this affects other tests)
        double[] parallelResults = data.getMeasuredPropertyArray("stability");

        // Run serially

        //    First, clear the results (make sure that parallel actually does something)
        data.setTargetProperty("stability", true);
        data.setMeasuredClasses(new double[data.NEntries()]);

        //  Test it serially
        startTime = System.nanoTime();
        mdfr.modifyDataset(data);
        finishTime = System.nanoTime();
        long serialTime = finishTime - startTime;
        System.out.format("Serial time elapsed: %d us\n", serialTime / 1000);
        double[] serialResults = data.getMeasuredPropertyArray("stability");

        // Make sure the two are equivalent
        assertArrayEquals(parallelResults, serialResults, 1e-6);
    }
    
}   
