/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;

/**
 * Filter entries based on absolute error. Entries with an absolute
 * difference between measured and predicted class variable below a threshold pass 
 * this filter. Dataset must have both a measured and predicted class variable for 
 * this filter to be applicable. 
 * 
 * <usage><p><b>Usage</b>: &lt;threshold&gt;
 * <br><pr><i>threshold</i>: Entries with an absolute error below this level pass filter</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class RegressionErrorFilter extends BaseDatasetFilter {
    /** Error threshold */
    private double Threshold;
    
    @Override 
    public void setOptions(List Options) throws Exception {
        try {
            Threshold = Double.parseDouble(Options.get(0).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <threshold>";
    }
    
    @Override protected boolean[] label(Dataset D) {
        if (! (D.getEntry(0).hasMeasurement() && D.getEntry(0).hasPrediction()))
            throw new Error("Dataset must have both measured and predicted classes");
        boolean[] output = new boolean[D.NEntries()];
        for (int i=0; i<D.NEntries(); i++)
            output[i] = Math.abs(D.getEntry(i).getPredictedClass() - 
                    D.getEntry(i).getMeasuredClass()) < Threshold;
        return output;
    }

    @Override public void train(Dataset TrainingSet) {
        /* Nothing to train */
    }
}
