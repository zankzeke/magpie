package magpie.models;

import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.models.regression.GuessMeanRegression;
import magpie.statistics.performance.RegressionStatistics;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the commands of a model
 * @author Logan Ward
 */
public class ModelCommandsTest {
    
    /**
     * Get an example dataset
     * @return Dataset with two attributes and a single measured class
     * @throws java.lang.Exception
     */
    static public Dataset getDataset() throws Exception {
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.txt", null);
        return data;
    }
    
    /**
     * Get an example model
     * @return A {@linkplain GuessMeanRegression} model
     */
    static public BaseModel getModel() {
        return new GuessMeanRegression();
    }
    
    @Test
    public void testTrainAndRun() throws Exception {
        Dataset data = getDataset();
        BaseModel model = getModel();
        
        // Make sure trained returns false
        assertFalse(model.isTrained());
        assertFalse(model.isValidated());
        
        // Run the train command
        List<Object> cmd = new LinkedList<>();
        cmd.add("train");
        cmd.add(data);
        
        model.runCommand(cmd);
        
        // Make sure it is trained
        assertTrue(model.isTrained());
        assertFalse(model.isValidated());
        
        // Clear the predicted class variable in data
        double prevResult = data.getEntry(0).getPredictedClass();
        data.deletePredictedClasses();
        assertFalse(data.getEntry(0).hasPrediction());
        
        cmd.set(0, "run");
        
        model.runCommand(cmd);
        
        assertTrue(data.getEntry(0).hasPrediction());
        assertEquals(prevResult, data.getEntry(0).getPredictedClass(), 1e-6);
    }
    
    @Test
    public void testCV() throws Exception {
        Dataset data = getDataset();
        BaseModel model = getModel();
        
        // Make sure trained returns false
        assertFalse(model.isTrained());
        assertFalse(model.isValidated());
        
        // Run the cross validation command
        List<Object> cmd = new LinkedList<>();
        cmd.add("crossvalidate");
        cmd.add(data);
        
        model.runCommand(cmd);
        
        // Make sure it is validated and neither trained nor run
        assertFalse(model.isTrained());
        assertTrue(model.isValidated());
        assertFalse(data.getEntry(0).hasPrediction());
        assertTrue(model.getValidationMethod().contains("10-fold"));
        
        // Same thing, but with 5-fold CV
        cmd.add(5);
        
        model.runCommand(cmd);
        
        assertFalse(model.isTrained());
        assertTrue(model.isValidated());
        assertFalse(data.getEntry(0).hasPrediction());
        assertTrue(model.getValidationMethod().contains("5-fold"));
        
        // Same thing, but with LOOCV
        cmd.set(2, "loocv");
        
        model.runCommand(cmd);
        
        assertFalse(model.isTrained());
        assertTrue(model.isValidated());
        assertFalse(data.getEntry(0).hasPrediction());
        assertTrue(model.getValidationMethod().toLowerCase().contains("leave-one-out"));
        
        // Make sure the LOOCV result is the expected value
        double[] error = new double[data.NEntries()];
        double totalSum = StatUtils.sum(data.getMeasuredClassArray());
        for (int i=0; i<data.NEntries(); i++) {
            double myVal = data.getEntry(i).getMeasuredClass();
            error[i] = Math.abs(myVal - (totalSum - myVal) / (data.NEntries() - 1));
        }
        assertEquals(((RegressionStatistics) model.ValidationStats).MAE,
                StatUtils.mean(error), 1e-6);
    }
}
