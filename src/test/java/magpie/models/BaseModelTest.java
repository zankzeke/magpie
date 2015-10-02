package magpie.models;

import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.models.BaseModel;
import magpie.models.regression.GuessMeanRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test regression model capabilities.
 * @author Logan Ward
 */
public class BaseModelTest {
    public Dataset DataType = new Dataset();
    
    /**
     * Create a new instance of the model
     * @return New instance
     */
    public BaseModel generateModel() {
        return new GuessMeanRegression();
    }
    
    public Dataset getData() throws Exception {
        Dataset data;
        
        // Generate the appropriate kind of data
        if (DataType.getClass().equals(Dataset.class)) {
            data = new Dataset();
            data.importText("datasets/simple-data.txt", null);
            
        } else if (DataType.getClass().equals(CompositionDataset.class)) {
            data = new CompositionDataset();
            data.importText("datasets/small_set.txt", null);
            data.generateAttributes();
            
            CompositionDataset ptr = (CompositionDataset) data;
            ptr.setTargetProperty("delta_e", false);
        } else {
            throw new Exception("Dataset type not supported: " + DataType.getClass().getName());
        }
        return data;
    }
    
    public void addEntry(Dataset data) throws Exception {
        if (DataType.getClass().equals(Dataset.class)) {
            data.addEntry("0.0, -1.5");
        } else if (DataType.getClass().equals(CompositionDataset.class)) {
            data.addEntry("NaCl");
        } else {
            throw new Exception("Dataset type not supported: " + DataType.getClass().getName());
        }
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
		addEntry(clone);
		
		model.train(clone);
	}
	
    @Test
	public void testValidation() throws Exception {
		BaseModel model = generateModel();
        Dataset data = getData();
        
        // Train model
        assertFalse(model.isTrained());
        model.train(data);
        assertTrue(model.isTrained());
        
        // Test model status
        assertTrue(model.getValidationMethod().contains("Unvalidated"));
        assertFalse(model.isValidated());
		
        // Run CV
		Dataset output = model.crossValidate(10, data);
        assertTrue(model.getValidationMethod().contains("10-fold"));
        assertEquals(data.NEntries(), output.NEntries());
        
        // Reset model
        model.resetModel();
        assertTrue(model.getValidationMethod().contains("Unvalidated"));
        assertFalse(model.isValidated());
        
        // Externally validate model
        model.train(data);
        model.externallyValidate(data);
        assertTrue(model.isValidated());
        assertTrue(model.getValidationMethod().contains(Integer.toString(data.NEntries())));
        assertEquals(model.TrainingStats.toString(), model.ValidationStats.toString());
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
        String dcrpt = model.printDescription(true);
        assertTrue(dcrpt.length() > 0);
        System.out.println(dcrpt);
        
        // Make sure it prints something: HTML
        dcrpt = model.printDescription(false);
        assertTrue(dcrpt.length() > 0);
        System.out.println(dcrpt);
    }
    
    @Test
    public void testTrain() throws Exception {
        // Get model and training set
        BaseModel model = generateModel();
        Dataset data = getData();
        
        // Train model without computing training data, make sure it does not run model
        model.train(data, false);
        assertFalse(data.getEntry(0).hasPrediction());
        
        // Make sure that training the model normally does
        model.train(data);
        assertTrue(data.getEntry(0).hasPrediction());
        assertTrue(model.TrainingStats.NumberTested > 0);
    }
}
