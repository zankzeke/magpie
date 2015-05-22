package magpie.utility.tools;

import magpie.analytics.RegressionStatistics;
import magpie.data.materials.CompositionDataset;
import magpie.models.regression.WekaRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class BatchModelEvaluatorTest {

    @Test
    public void test() throws Exception {
        // Import some data
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        data.generateAttributes();
        data.setTargetProperty("delta_e", true);
        
        // Make a model
        WekaRegression weka = new WekaRegression("trees.REPTree", null);
        weka.train(data);
        
        // Clear out data and make a copy without attributes
        data.clearData();
        data.importText("datasets/small_set.txt", null);
        data.setTargetProperty("delta_e", true);
        
        // Run the model
        BatchModelEvaluator eval = new BatchModelEvaluator();
        eval.setModel(weka);
        eval.setBatchSize(100);
        eval.evaluate(data);
        
        // Make sure it gives the same results
        RegressionStatistics trainStats = (RegressionStatistics) weka.TrainingStats;
        RegressionStatistics runStats = new RegressionStatistics();
        runStats.evaluate(data);
        assertEquals(trainStats.MAE, runStats.MAE, 1e-6);
    }
    
}
