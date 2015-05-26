package magpie.analytics;

import magpie.data.Dataset;
import org.apache.commons.math3.stat.correlation.*;
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
    /** Spearman's correlation coefficient */
	public double S;
	/** Kendall's rank correlation coefficient */
	public double Tau;
    
    @Override public void evaluate(Dataset Results) {
        Measured = Results.getMeasuredClassArray();
        Predicted = Results.getPredictedClassArray();
        getStatistics(Measured, Predicted);        
    }

    /**
     * Calculate regression statistics 
     * @param measured Measured class variable for each entry
     * @param predicted Predicted class variable for each entry (same order as measured)
     */
    protected void getStatistics(double[] measured, double[] predicted) {
        int NEntries = measured.length;
        NumberTested = NEntries;
        // Calculate correlation coefficients
        R = new PearsonsCorrelation().correlation(measured, predicted);
        S = new SpearmansCorrelation().correlation(measured, predicted);
        Tau = new KendallsCorrelation().correlation(measured, predicted);
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
                +"\nPearson's Correlation (R): "+String.format("%.4f", R)
                +"\nSpearman's Correlation (S): "+String.format("%.4f", S)
                +"\nKendall's Correlation (Tau): "+String.format("%.4f", Tau)
                +"\nMAE: "+String.format("%.4e",MAE)
                +"\nRMSE: "+String.format("%.4e",RMSE)
                +"\nMRE: "+String.format("%.4f",MRE)
                +"\nROC AUC: "+String.format("%.4f", ROC_AUC);
        return out;
    }
}
