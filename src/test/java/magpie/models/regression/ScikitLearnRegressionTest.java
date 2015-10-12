package magpie.models.regression;

import java.io.File;
import java.io.FileInputStream;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
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
}
