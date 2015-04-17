package magpie.analytics;

import magpie.data.Dataset;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.StatUtils;

/**
 * This class contains methods to calculate statistics about regression models.
 * @author Logan Ward
 * @version 0.1
 */
public class RegressionStatistics extends BaseStatistics {
    /** Mean absolute error */
    public double MAE;
    /** Mean relative error */
    public double MRE;
    /** Root mean squared error */
    public double RMSE;
    /** Pearson's correlation coefficient */
    public double R;  
    
    @Override public void evaluate(Dataset Results) {
        double[] measured = Results.getMeasuredClassArray();
        double[] predicted = Results.getPredictedClassArray();
        getStatistics(measured, predicted);        
    }

    /**
     * Calculate regression statistics 
     * @param measured Measured class variable for each entry
     * @param predicted Predicted class variable for each entry (same order as measured)
     */
    protected void getStatistics(double[] measured, double[] predicted) {
        int NEntries = measured.length;
        NumberTested = NEntries;
        // Calculate R
        R = measured.length > 1 ? new PearsonsCorrelation().correlation(measured, predicted) : Double.NaN;
        if (Double.isNaN(R)) R = 0;
        // Calculate statistics of absolute error
        double[] error = new double[NEntries];
        for (int i=0; i<NEntries; i++)
            error[i]=Math.abs(predicted[i]-measured[i]);
        MAE = StatUtils.mean(error);
        RMSE = Math.sqrt(StatUtils.sumSq(error)/(double)error.length);
        // Calculate statistics on the relative error
        for (int i=0; i<NEntries; i++)
            error[i]=Math.abs(1-(predicted[i]/measured[i]));
        MRE = StatUtils.mean(error);
        // Calculate the reciever-operating characteristic curve
        getROCCurve(measured, predicted, 25);
        ROC_AUC = integrateROCCurve(ROC);
    }
    
    @Override public Object clone() throws CloneNotSupportedException {
        RegressionStatistics x = (RegressionStatistics) super.clone();
        x.MAE = MAE;
        x.MRE = MRE;
        x.RMSE = RMSE;
        x.R = R;
        return x;
    }
    
    @Override public String toString() {
        String out = new String();
        out+="Number Tested: "+NumberTested
                +"\nCorrelation Coeff.: "+String.format("%.4f", R)
                +"\nMAE: "+String.format("%.4e",MAE)
                +"\nRMSE: "+String.format("%.4e",RMSE)
                +"\nMRE: "+String.format("%.4f",MRE)
                +"\nROC AUC: "+String.format("%.4f", ROC_AUC);
        return out;
    }
}
