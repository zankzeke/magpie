package magpie.models;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.attributes.selectors.UserSpecifiedAttributeSelector;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.regression.PolynomialRegression;
import org.apache.commons.math3.stat.regression.MillerUpdatingRegression;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test effects of adding normalizers, attribute selectors, etc
 * 
 * @author Logan Ward
 */
public class ModelAdditionsTest {
    
    @Test
    public void testNormalizer() throws Exception {
        // Create a test dataset
        Dataset data = new Dataset();
        data.addAttributes(Arrays.asList(new String[]{"x","y"}));
        
        for (double x=0; x<=2; x+=0.5) {
            for (double y=0; y<=2; y+=0.5) {
                BaseEntry entry = new BaseEntry();
                entry.setAttributes(new double[]{x,y});
                data.addEntry(entry);
                entry.setMeasuredClass(2*x + y);
            }
        }
        
        // Store initial attributes
        double[][] goldAttributes = data.getAttributeArray();
        
        // Train a model without any normalization
        PolynomialRegression model = new PolynomialRegression();
        model.setOrder(1);
        
        model.train(data);
        
        // Check that model fits data
        assertArrayEquals(new double[]{0,2,1}, model.getCoefficients(), 1e-6);
        
        // Get initial predictions from model
        model.run(data);
        double[] goldResults = data.getPredictedClassArray();
        
        // Add a normalizer
        List<Object> command = new LinkedList<>();
        command.add("normalize");
        command.add("attributes");
        command.add("RescalingNormalizer");
        
        model.runCommand(command);
        
        // Re-train model, check that coefficients are correct
        model.train(data);
        
        assertArrayEquals(new double[]{3,2,1}, model.getCoefficients(), 1e-6);
        
        // Make sure attributes unchanged
        checkAttributes(data, goldAttributes);
        
        // Re-run model, make sure results are the same (linear regression should
        //   not be affected by normalization)
        model.run(data);
        
        assertArrayEquals(goldResults, data.getPredictedClassArray(), 1e-6);
               
        // Make sure attributes unchanged
        checkAttributes(data, goldAttributes);
        
        // Now, normalize the class variable as well
        command = new LinkedList<>();
        command.add("normalize");
        command.add("attributes");
        command.add("class");
        command.add("RescalingNormalizer");
        
        model.runCommand(command);
        
        // Store original class variables
        double[] goldClass = data.getMeasuredClassArray();
        
        // Re-train model, check that coefficients are correct
        model.train(data);
        
        assertArrayEquals(new double[]{0,2f/3,1f/3}, model.getCoefficients(), 1e-6);
        
        // Make sure attributes/class unchanged
        checkAttributes(data, goldAttributes);
        assertArrayEquals(goldClass, data.getMeasuredClassArray(), 1e-6);
        
        // Re-run model, make sure results are the same (linear regression should
        //   not be affected by normalization)
        model.run(data);
        
        assertArrayEquals(goldResults, data.getPredictedClassArray(), 1e-6);
        assertArrayEquals(goldClass, data.getMeasuredClassArray(), 1e-6);
        
        // Print out model details
        String description = model.printDescription(false);
        assertTrue(description.contains("RescalingNormalizer"));
        System.out.println(description);
        
        // Save original model predictions
        double[] originalResults = data.getPredictedClassArray();
        
        // Make sure clone works
        PolynomialRegression modelClone = model.clone();
        modelClone.run(data);
        assertArrayEquals(originalResults, data.getPredictedClassArray(), 1e-6);
        
        Dataset dataClone = data.clone();
        
        //   Increase measured class variable by one
        for (BaseEntry e : dataClone.getEntries()) {
            e.setMeasuredClass(e.getMeasuredClass() + 1);
        }
        
        // Train clone model
        modelClone.train(dataClone);
        
        // Make sure original predictions are unaffected
        model.run(data);
        assertArrayEquals(originalResults, data.getPredictedClassArray(), 1e-6);
    }
    
    @Test
    public void testAttributeSelection() throws Exception {
         // Create a test dataset
        Dataset data = new Dataset();
        data.addAttributes(Arrays.asList(new String[]{"x","y"}));
        
        for (double x=0; x<=2; x+=0.5) {
            for (double y=0; y<=2; y+=0.5) {
                BaseEntry entry = new BaseEntry();
                entry.setAttributes(new double[]{x,y});
                data.addEntry(entry);
                entry.setMeasuredClass(2*x + y);
            }
        }
        
        // Store initial attributes
        double[][] goldAttributes = data.getAttributeArray();
        
        // Train a model without any selection
        PolynomialRegression model = new PolynomialRegression();
        model.setOrder(1);
        
        model.train(data);
        
        // Check that model fits data
        assertArrayEquals(new double[]{0,2,1}, model.getCoefficients(), 1e-6);
        
        // Get initial predictions from model
        model.run(data);
        
        // Create attribute selector
        UserSpecifiedAttributeSelector sltr = new UserSpecifiedAttributeSelector();
        sltr.selectAttributes(Arrays.asList(new String[]{"x"}));
        
        // Add to model
        List<Object> command = new LinkedList<>();
        command.add("set");
        command.add("selector");
        command.add(sltr);
        
        model.runCommand(command);
        
        // Re-train model
        model.train(data);
        
        // Make sure attributes were unchanged
        checkAttributes(data, goldAttributes);
        
        // Check results of model
        MillerUpdatingRegression reg = new MillerUpdatingRegression(1, true);
        for (BaseEntry e : data.getEntries()) {
            reg.addObservation(new double[]{e.getAttribute(0)}, e.getMeasuredClass());
        }
        RegressionResults results = reg.regress();
        
        assertArrayEquals(results.getParameterEstimates(), 
            model.getCoefficients(), 1e-6);
        
        // Print out model details
        String description = model.printDescription(false);
        assertTrue(description.contains("UserSpecifiedAttributeSelector"));
        System.out.println(description);
        
        // Run model
        model.run(data);
        checkAttributes(data, goldAttributes);
        
        double[] originalResults = data.getPredictedClassArray();
        
        // Make sure clone works
        PolynomialRegression modelClone = model.clone();
        modelClone.run(data);
        assertArrayEquals(originalResults, data.getPredictedClassArray(), 1e-6);
        
        Dataset dataClone = data.clone();
        
        //   Increase measured class variable by one
        for (BaseEntry e : dataClone.getEntries()) {
            e.setMeasuredClass(e.getMeasuredClass() + 1);
        }
        
        // Train clone model
        modelClone.train(dataClone);
        
        // Make sure original predictions are unaffected
        model.run(data);
        assertArrayEquals(originalResults, data.getPredictedClassArray(), 1e-6);
        
        // Run model
        model.run(data);
        checkAttributes(data, goldAttributes);
    }
    
    @Test
    public void testFilter() throws Exception {
        // Create a test dataset
        Dataset data = new Dataset();
        for (int i=0; i<11; i++) {
            data.addEntry(new BaseEntry());
        }
        data.addAttribute("x", new double[]{0,0.1,0.2,-0.1,0.05,-0.05,0.3,0.4,1,0,0.15});
        data.addAttribute("y", new double[]{1,0.1,0.3,-0.2,0.05,-0.05,0.3,0.4,0,0,0.15});
        data.addAttribute("z", new double[]{1,1,1,1,1,1,1,1,1,1,1});
        
        data.setMeasuredClasses(new double[]{-0.5,2,0.2,-0.1,0.05,-0.05,0.3,0.4,0,0,0.15});
        
        // Store initial attributes
        double[][] goldAttributes = data.getAttributeArray();
        
        // Train a model without any normalization
        PolynomialRegression model = new PolynomialRegression();
        model.setOrder(1);
        
        model.train(data);
        
        // Check that model fits data
        assertArrayEquals(new double[]{0.281,0.068,-0.384,0}, model.getCoefficients(), 1e-2);
        
        // Get initial predictions from model
        model.run(data);
        
        // Add a normalizer
        List<Object> command = new LinkedList<>();
        command.add("filter");
        command.add("include");
        command.add("ZScoreOutlierFilter");
        command.add(1.0);
        command.add("-class");
        command.add("-attributes");
        
        model.runCommand(command);
        
        // Check whether filter is inclusive
        assertFalse(model.getFilter().toExclude());
        
        // Now, set it to exclude
        command.set(1, "exclude");
        
        model.runCommand(command);
        
        assertTrue(model.getFilter().toExclude());
        
        // Re-train model, check that coefficients are correct
        model.train(data);
        
        assertArrayEquals(new double[]{0,1,0,0}, model.getCoefficients(), 1e-6);
        
        // Make sure attributes unchanged
        checkAttributes(data, goldAttributes);
        
        // Print out model details
        String description = model.printDescription(false);
        assertTrue(description.contains("ZScoreOutlierFilter"));
        System.out.println(description);
        
        // Save original model predictions
        model.run(data);
        double[] originalResults = data.getPredictedClassArray();
        
        // Make sure clone works. For the filter, this shouldn't be a problem.
        //  Filter are only used at train time, so they shouldn't interfere with
        //  each other (unless models are being trained concurrently)
        PolynomialRegression modelClone = model.clone();
        assertArrayEquals(modelClone.getCoefficients(), model.getCoefficients(), 1e-6);
        modelClone.run(data);
        assertArrayEquals(originalResults, data.getPredictedClassArray(), 1e-6);
        
        Dataset dataClone = data.clone();
        
        //   Remove outlier from attribute array #1
        double[] newX = new double[]{0,0.1,0.2,-0.1,0.05,-0.05,0.3,0.4,0.2,0,0.15};
        for (int e=0; e<dataClone.NEntries(); e++) {
            dataClone.getEntry(e).setAttribute(0, newX[e]);
        }
        
        // Train clone model
        modelClone.train(dataClone);
        
        // Make sure original predictions are unaffected
        model.run(data);
        assertArrayEquals(originalResults, data.getPredictedClassArray(), 1e-6);
    }

    /**
     * Make sure attributes have not changed
     * @param data Dataset
     * @param goldAttributes Original values of datasets
     */
    protected void checkAttributes(Dataset data, double[][] goldAttributes) {
        for (int i=0; i<data.NEntries(); i++) {
            assertArrayEquals(goldAttributes[i], data.getEntry(i).getAttributes(), 1e-6);
        }
    }
}
