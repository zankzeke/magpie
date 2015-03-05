package magpie.models;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.regression.PolynomialRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test regression model capabilities.
 * @author Logan Ward
 */
abstract public class BaseModelTest {
	private Dataset data = new Dataset();
	
	public BaseModelTest() {
		try {
			data.clearData();
			data.importText("datasets/simple-data.csv", null);
		} catch (Exception e) {
			fail(e.getLocalizedMessage());
		}
	}
    
    /**
     * Create a new instance of the model
     * @return New instance
     */
    public abstract BaseModel generateModel();

	@Test
	public void testSimpleTraining() {
		BaseModel model = generateModel();
		model.train(data);
	}
	
	@Test
	public void testMixedTraining() throws Exception {
		BaseModel model = generateModel();
		
		Dataset clone = data.clone();
		clone.addEntry("0.0, -1.5");
		
		model.train(clone);
	}
	
    @Test
	public void testCrossValidation() {
		PolynomialRegression model = new PolynomialRegression();
		
		model.crossValidate(10, data);
	}
    
    @Test
    public void testClone() {
        BaseModel model1 = generateModel();
        BaseModel model2 = model1.clone();
        
        // Train a model on a random subset, 
        //   get its perforamnce on the whole dataset
        model1.train(data.getRandomSubset(0.5));
        model1.externallyValidate(data);
        String Stats1 = model1.ValidationStats.toString();
        
        // Train its clone on a different dataset
        model2.train(data.getRandomSubset(0.5));
        
        // Validation stats should be the same
        model1.externallyValidate(data);
        assertEquals(Stats1, model1.ValidationStats.toString());
    }
}
