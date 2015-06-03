package magpie.models;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.regression.GuessMeanRegression;
import magpie.models.regression.PolynomialRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test regression model capabilities.
 * @author Logan Ward
 */
public class BaseModelTest {
    
    /**
     * Create a new instance of the model
     * @return New instance
     */
    public BaseModel generateModel() {
        return new GuessMeanRegression();
    }
    
    public Dataset getData() throws Exception {
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.csv", null);
        return data;
    }

	@Test
	public void testSimpleTraining() throws Exception {
		BaseModel model = generateModel();
        Dataset data = getData();
		model.train(data);
	}
	
	@Test
	public void testMixedTraining() throws Exception {
		BaseModel model = generateModel();
        Dataset data = getData();
		
		Dataset clone = data.clone();
		clone.addEntry("0.0, -1.5");
		
		model.train(clone);
	}
	
    @Test
	public void testCrossValidation() throws Exception {
		BaseModel model = generateModel();
        Dataset data = getData();
		
		model.crossValidate(10, data);
	}
    
    @Test
    public void testClone() throws Exception {
        BaseModel model1 = generateModel();
        BaseModel model2 = model1.clone();
        
        // Train a model on a random subset, 
        //   get its perforamnce on the whole dataset
        Dataset data = getData();
        model1.train(data.getRandomSubset(0.5));
        model1.externallyValidate(data);
        String Stats1 = model1.ValidationStats.toString();
        
        // Train its clone on a different dataset
        model2.train(data.getRandomSubset(0.5));
        
        // Validation stats should be the same
        model1.externallyValidate(data);
        assertEquals(Stats1, model1.ValidationStats.toString());
    }
    
    @Test
    public void testPrintDescription() throws Exception {
        BaseModel model = generateModel();
        Dataset data = getData();
		model.train(data);
        
        // Make sure it prints something: HTML
        String dcrpt = model.printModelDescription(true);
        assertTrue(dcrpt.length() > 0);
        System.out.println(dcrpt);
        
        // Make sure it prints something: HTML
        dcrpt = model.printModelDescription(false);
        assertTrue(dcrpt.length() > 0);
        System.out.println(dcrpt);
    }
}
