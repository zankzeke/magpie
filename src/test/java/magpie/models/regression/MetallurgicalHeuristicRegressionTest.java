package magpie.models.regression;

import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MetallurgicalHeuristicRegressionTest extends BaseModelTest {

    public MetallurgicalHeuristicRegressionTest() {
        DataType = new CompositionDataset();
    }

    @Override
    public BaseModel generateModel() {
        // Create fake hull data
        CompositionDataset hullData = new CompositionDataset();
        hullData.addProperty("delta_e");
        try {
            hullData.addEntry("Al3C");
            hullData.addEntry("AlBe");
            hullData.addEntry("AlBe2");
        } catch (Exception e) {
            throw new Error(e);
        }
        hullData.setTargetProperty("delta_e", true);
        hullData.setMeasuredClasses(new double[]{-1,-2,-1.75});
        
        // Create model
        MetallurgicalHeuristicRegression m = new MetallurgicalHeuristicRegression();
        m.setBinaryConvexHulls(hullData);
        return m;
    }
    
    @Test
    public void testCalculation() throws Exception {
        // Get the model 
        MetallurgicalHeuristicRegression m = (MetallurgicalHeuristicRegression) generateModel();
        CompositionDataset trainData = (CompositionDataset) getData();
        m.train(trainData);
        
        // Make the test point (Al1.5BeC)
        CompositionDataset testData = trainData.emptyClone();
        testData.addEntry("Al1.5BeC");

        // Compute the heuristic by hand
        double Eac = -1 + (0.75-4.0/7.0) / 0.75;
        double Eab = 0 - 2 * 3.0/7.0 / 0.5;
        double Ebc = 0;
        double Etern = 0.375 * Eac + 0.375 * Eab + 0.25 * Ebc;
               
        // Compute the formation energy
        testData.generateAttributes();
        m.run(testData);
        
        // Check it
        assertEquals(Etern, testData.getEntry(0).getPredictedClass(), 1e-2);
        
        // Check fitting
        m.setUseCorrection(true);
        m.train(trainData);
        System.out.println(m.printModel());
    }
    
    @Test
    public void testBinaryHulls() throws Exception {
        // Create fake hull data
        CompositionDataset hullData = new CompositionDataset();
        hullData.addProperty("delta_e");
        hullData.addEntry("Al3C");
        hullData.addEntry("AlBe");
        hullData.addEntry("AlBe2");
        hullData.setTargetProperty("delta_e", true);
        hullData.setMeasuredClasses(new double[]{-1,-2,-1.75});
        
        // Create holder
        BinaryConvexHullHolder holder = new BinaryConvexHullHolder(hullData);
        
        // Test binary hulls
        assertEquals(0.0, holder.evaluatePoint(12, 5, 0), 1e-6);
        assertEquals(-1.0, holder.evaluatePoint(12, 5, 0.75), 1e-6);
        assertEquals(-0.5, holder.evaluatePoint(12, 5, 0.75/2), 1e-6);
        assertEquals(-0.5, holder.evaluatePoint(12, 5, 0.875), 1e-6);
        assertEquals(-1.0, holder.evaluateCompound(new CompositionEntry("Al3C")), 1e-6);
        assertEquals(-0.5, holder.evaluateCompound(new CompositionEntry("Al7C")), 1e-6);
        assertEquals(-2, holder.evaluateCompound(new CompositionEntry("AlBe")), 1e-6);
        assertEquals(-1.875, holder.evaluateCompound(new CompositionEntry("Al5Be7")), 1e-6);
        assertEquals(-1.75/2, holder.evaluateCompound(new CompositionEntry("AlBe5")), 1e-6);
    }
}
