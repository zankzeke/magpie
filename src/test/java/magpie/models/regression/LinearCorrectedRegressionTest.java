
package magpie.models.regression;

import magpie.analytics.RegressionStatistics;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.splitters.MixedMetalsSplitter;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class LinearCorrectedRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        BaseModel sub = new PolynomialRegression();
        LinearCorrectedRegression model = new LinearCorrectedRegression();
        try {
            model.setSubmodel(sub);
        } catch (Exception e) {
            throw new Error(e);
        }
        return model;
    }
    
    @Test
    public void testImprovement() throws Exception {
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        data.generateAttributes();
        data.setTargetProperty("delta_e", true);
        
        SplitRegression sub = new SplitRegression();
        sub.setPartitioner(new MixedMetalsSplitter());
        sub.setGenericModel(new WekaRegression("trees.REPTree", null));
        sub.train(data);
        
        LinearCorrectedRegression model = new LinearCorrectedRegression();
        model.setSubmodel(sub);
        model.train(data);
        
        // Get MAE
        RegressionStatistics s1 = (RegressionStatistics) sub.TrainingStats;
        RegressionStatistics s2 = (RegressionStatistics) model.TrainingStats;
        s1.savePerformanceData("data_uncorrected.csv");
        s2.savePerformanceData("data_corrected.csv");
        assertTrue(s1.MAE - s2.MAE > -1e-6);
    }
    
}
