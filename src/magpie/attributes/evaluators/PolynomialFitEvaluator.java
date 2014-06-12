/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.attributes.evaluators;

import java.util.List;
import magpie.data.Dataset;
import magpie.models.regression.PolynomialRegression;
import magpie.user.CommandHandler;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Evaluate an attribute based on the RMSE of polynomial model using that attribute.
 *  This is exactly the method used by <a href="http://pubs.acs.org/doi/abs/10.1021/cm403727z">
 *  Meredig and Wolverton (2014)</a> in their Clustering-Ranking-Modeling method.
 * 
 * <usage><p><b>Usage</b>: &lt;order>
 * <br><pr><i>order</i>: Order of polynomial fit to data when evaluating attributes</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PolynomialFitEvaluator extends BaseAttributeEvaluator {
    /** Order of polynomial used to evaluate attributes */
    protected int order = 2;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            order = Integer.parseInt(Options[0]);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <order>";
    }
    
    @Override
    protected boolean positiveIsBetter() {
        return false;
    }

    @Override
    protected double[] evaluateAttributes_internal(Dataset Data) {
        double[] output = new double[Data.NAttributes()];
        
        // For each attribute 
        double[] measuredClass = Data.getMeasuredClassArray();
        for (int a=0; a < Data.NAttributes(); a++) {
            double[] attributeValues = Data.getSingleAttributeArray(a);
            
            // Fit a polynomial
            double[][] holderArray = new double[Data.NEntries()][1];
            for (int i=0; i<Data.NEntries(); i++) holderArray[i][0] = attributeValues[i];
            double[] coeff = PolynomialRegression.fitPolynomialModel(holderArray,order,measuredClass);
            
            // Run the model
            double[] predictedClass = PolynomialRegression.runPolynomialModel(holderArray, order, coeff);
            
            // Get the RMSE
            double rmse = 0.0;
            for (int i=0; i<Data.NEntries(); i++)
                rmse += Math.pow(predictedClass[i] - measuredClass[i], 2.0);
            rmse /= (double) Data.NEntries();
            rmse = Math.sqrt(rmse);
            
            // Store result
            output[a] = rmse;
        }
        
        // Normalize
        double best = StatUtils.min(output);
        for (int i=0; i<output.length; i++) output[i] /= best;
        return output;
    }
    
}
