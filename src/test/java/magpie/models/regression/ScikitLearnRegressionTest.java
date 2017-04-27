package magpie.models.regression;

import java.io.File;
import java.io.FileInputStream;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import magpie.statistics.performance.RegressionStatistics;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Logan Ward
 */
public class ScikitLearnRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        ScikitLearnRegression model = new ScikitLearnRegression();
        try {
            model.readModel(new FileInputStream("test-files/sklearn-linreg.pkl"));
            model.setCompressionLevel(1);
        } catch (Exception e) {
            throw new Error(e);
        }
        return model;
    }
    
    @Test
    public void testSerialization() throws Exception {
        ScikitLearnRegression model = (ScikitLearnRegression) generateModel();
        Dataset data = getData();
        
        // Train model
        model.train(data);
        
        // Save it using serialization
        File tempFile = File.createTempFile("model", ".obj");
        tempFile.deleteOnExit();
        model.saveState(tempFile.getCanonicalPath());
        
        // Load it back in
        ScikitLearnRegression model2 = (ScikitLearnRegression) BaseModel.loadState(tempFile.getCanonicalPath());
        
        // Make sure they give the same results
        model.run(data);
        double[] model1Vals = data.getPredictedClassArray();
        model2.run(data);
        double[] model2Vals = data.getPredictedClassArray();
        Assert.assertArrayEquals(model1Vals, model2Vals, 1e-6);
    }

    @Test
    public void testModelTypeMismatch() throws Exception {
        boolean failed = false;

        // Load in a classifier
        ScikitLearnRegression model = new ScikitLearnRegression();
        model.readModel(new FileInputStream("test-files/sklearn-gbc.pkl"));

        // Try to run the model
        try {
            model.train(getData());
        } catch (Exception e) {
            failed = true;
        }

        Assert.assertTrue(failed);
    }

    /**
     * Verify that Magpie gives the exact same results as training this manually
     * @throws Exception
     */
    @Test
    public void testModelOutputs() throws Exception {
        // Train a model on some sample datasets
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.csv", null);
        ScikitLearnRegression model = (ScikitLearnRegression) generateModel();
        model.train(data);

        // Verify that the MAE is equal to the results I got manually
        Assert.assertEquals(0.59349613930720235, ((RegressionStatistics) model.TrainingStats).MAE, 1e-10);
    }
}
