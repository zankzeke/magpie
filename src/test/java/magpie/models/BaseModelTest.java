package magpie.models;

import magpie.Magpie;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.PrototypeEntry;
import magpie.data.materials.util.PrototypeSiteInformation;
import magpie.models.regression.AbstractRegressionModel;
import magpie.models.regression.GuessMeanRegression;
import magpie.statistics.performance.BaseStatistics;
import magpie.statistics.performance.ClassificationStatistics;
import magpie.statistics.performance.RegressionStatistics;
import org.apache.commons.math3.util.Combinations;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

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
        } else if (DataType.getClass().equals(PrototypeDataset.class)) {
            data = new PrototypeDataset();
            
            // Make the site info thingy for an A2B2 compound
            PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
            siteInfo.addSite(2, true, new LinkedList<Integer>());
            siteInfo.addSite(2, true, new LinkedList<Integer>());
            
            // Set it for the dataset
            PrototypeDataset dPtr = (PrototypeDataset) data;
            dPtr.setSiteInfo(siteInfo);
            
            // Make some random data
            for (int[] elems : new Combinations(10, 2)) {
                // Only do binary compounds
                if (elems[0] == elems[1]) continue;
                
                // Make an entry
                PrototypeEntry entry = new PrototypeEntry(siteInfo);
                for (int i=0; i<elems.length; i++) {
                    entry.setSiteComposition(i, new CompositionEntry(
                            new int[]{elems[i] + 25}, new double[]{1.0}));
                }
                entry.setMeasuredClass(Math.pow(-1, elems[0]));
                data.addEntry(entry);
            }
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
        } else if (DataType.getClass().equals(PrototypeDataset.class)) {
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
        model.done();
	}
	
	@Test
	public void testMixedTraining() throws Exception {
		BaseModel model = generateModel();
        Dataset data = getData();
		
		Dataset clone = data.clone();
		addEntry(clone);
		
		model.train(clone);
        
        model.done();
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
		
        // Run k-fold CV
		Dataset output = model.crossValidate(10, data);
        assertTrue(model.getValidationMethod().contains("10-fold"));
        assertEquals(data.NEntries(), output.NEntries());
        
        // Run random split CV
        Magpie.NThreads = 2;
        model.crossValidate(0.5, 10, data, 1);
        assertTrue(model.isValidated());
        assertTrue(model.getValidationMethod().contains("50.0%"));
        assertTrue(model.getValidationMethod().contains("10"));
        
        // Externally validate model
        model.externallyValidate(data);
        assertTrue(model.isValidated());
        assertTrue(model.getValidationMethod().contains(Integer.toString(data.NEntries())));
        
        // Make sure that validation stats are the same as training stats
        //   Should give same result, because model should not have been re-trained
        assertEquals(model.TrainingStats.toString(), model.ValidationStats.toString());
        
        // Reset model
        model.resetModel();
        assertTrue(model.getValidationMethod().contains("Unvalidated"));
        assertFalse(model.isValidated());
        
        model.done();
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
        BaseStatistics Stats1 = model1.ValidationStats;
        
        // Train its clone on a different dataset
        model2.train(data.getRandomSubset(0.5));
        
        // Validation stats should be the same
        model1.externallyValidate(data);
        if (model1 instanceof AbstractRegressionModel) {
            assertEquals(((RegressionStatistics) Stats1).MAE,
                    ((RegressionStatistics) model1.ValidationStats).MAE, 1e-6);
        } else {
            assertEquals(((ClassificationStatistics) Stats1).Accuracy,
                    ((ClassificationStatistics) model1.ValidationStats).Accuracy, 1e-6);
        }
        
        model1.done();
        model2.done();

        // Test the command line tool
        List<Object> command = new LinkedList<>();
        command.add("clone");

        BaseModel model3 = (BaseModel) model1.runCommand(command);

        assertNotSame(model1, model3);
        assertNotSame(model2, model3);
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
        
        model.done();
    }
    
    @Test
    public void testTrain() throws Exception {
        // Get model and training set
        BaseModel model = generateModel();
        Dataset data = getData();
        
        // Train model without computing training data, make sure it does not run model
        double[][] attrBefore = data.getEntryArray();
        model.train(data, false);
        assertFalse(data.getEntry(0).hasPrediction());
        
        // Make sure the attributes / measured class values are not changed
        double[][] attrAfter = data.getEntryArray();
        assertEquals(attrBefore.length, attrAfter.length);
        for (int e=0; e<attrAfter.length; e++) {
            assertArrayEquals(attrBefore[e], attrAfter[e], 1e-6);
        }
        
        // Make sure that training the model normally does
        model.train(data);
        assertTrue(data.getEntry(0).hasPrediction());
		assertTrue(model.TrainingStats.NumberTested > 0);

        // Check to make sure train + run doesn't change attributes / class
        attrAfter = data.getEntryArray();
        assertEquals(attrBefore.length, attrAfter.length);
        for (int e=0; e<attrAfter.length; e++) {
            assertArrayEquals(attrBefore[e], attrAfter[e], 1e-6);
        }
        
        model.done();
    }
}
