/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.attributes.evaluators;

import java.util.List;
import magpie.data.Dataset;
import magpie.utility.MathUtils;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

/**
 * Evaluate attributes based on Spearson's Correlation Coefficient, which is simply
 *  the Pearson's Correlation Coefficient between the measured and predicted ranks
 *  of entries. This class ranks attributes based on the squared value of the correlation 
 *  coefficient. So, positive and negative correlations are treated with an equal footing.
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 * @see PearsonsCorrelationEvaluator
 */
public class SpearmansCorrelationEvaluator extends BaseAttributeEvaluator {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        /* No options to set */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    

    @Override
    protected boolean positiveIsBetter() {
        return true;
    }

    @Override
    protected double[] evaluateAttributes_internal(Dataset Data) {
        double[][] feature_array = MathUtils.transpose(Data.getAttributeArray());
        double[] class_array = Data.getMeasuredClassArray();
        double [] output = new double[Data.NAttributes()];
        for (int i=0; i<Data.NAttributes(); i++) {
            output[i] = new SpearmansCorrelation()
                    .correlation(feature_array[i], class_array);
            if (Double.isNaN(output[i])) output[i] = 0.0;
            else output[i] *= output[i];
        }
        return output;
    }
}
