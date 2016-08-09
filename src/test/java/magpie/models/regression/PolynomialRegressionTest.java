package magpie.models.regression;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PolynomialRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        PolynomialRegression model = new PolynomialRegression();
        model.setOrder(2);
        return model;
    }
    
    @Test
    public void testResult() throws Exception {
        // Make a dataset with measured class = 2x - y
        Dataset data = new Dataset();
        data.addAttribute("x", new double[0]);
        data.addAttribute("y", new double[0]);
        
        for (int i=0; i<10; i++) {
            double x = Math.random();
            double y = Math.random();
            
            // Make an entry object
            BaseEntry entry = new BaseEntry();
            entry.setAttributes(new double[]{x, y});
            entry.setMeasuredClass(2 * x - y);
            
            data.addEntry(entry);
        }
        
        // Train a linear regression model
        PolynomialRegression model = new PolynomialRegression();
        
        List<Object> options = new ArrayList<>();
        options.add(1);
        
        model.setOptions(options);
        
        model.train(data);
        
        // Print the model
        String modelStr = model.printModel();
        String[] words = modelStr.split(" ");
        assertEquals(1 + 4 + 4, words[2].length()); // Length of the coeff for x
        assertArrayEquals(new double[]{0,2,-1}, model.coefficients, 1e-6);
        System.out.println("4 sig figs: " + modelStr);
        
        // Run the model, and make sure it is a near perfect fit
        model.run(data);
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(entry.getMeasuredClass(), entry.getPredictedClass(), 1e-6);
        }
        
        // Increase the number of significant figures
        options.add("-print_accuracy");
        options.add(2);
        
        model.setOptions(options);
        
        modelStr = model.printModel();
        words = modelStr.split(" ");
        assertEquals(1 + 2 + 4, words[2].length()); // Length of the coeff for x
        System.out.println("2 sig figs: " + modelStr);
    }
}
