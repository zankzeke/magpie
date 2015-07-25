package magpie.models.regression;

import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
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
        CompositionDataset testData = new CompositionDataset();
        testData.addEntry("Al1.5BeC");
        
        // Compute the hueristic by hand
        double Eac = -1 + (0.75-4.0/7.0) / 0.75;
        double Eab = 0 - 2 * 3.0/7.0 / 0.5;
        double Ebc = 0;
        double Etern = 0.375 * Eac + 0.375 * Eab + 0.25 * Ebc;
        
        // Compute the formation energy
        m.run(testData);
        
        // Check it
        assertEquals(Etern, testData.getEntry(0).getPredictedClass(), 1e-2);
    }
}
