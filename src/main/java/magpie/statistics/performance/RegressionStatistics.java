package magpie.statistics.performance;

import magpie.data.Dataset;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.util.Map;
import java.util.TreeMap;

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
	public double Rho;
	/** Kendall's rank correlation coefficient */
	public double Tau;

    @Override
    protected void evaluate_protected(Dataset results) {
        Measured = results.getMeasuredClassArray();
        Predicted = results.getPredictedClassArray();
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
        if (measured.length > 2) {
            R = new PearsonsCorrelation().correlation(measured, predicted);
            Rho = new SpearmansCorrelation().correlation(measured, predicted);
            Tau = new KendallsCorrelation().correlation(measured, predicted);
        } else {
            Tau = Double.NaN;
            Rho = Double.NaN;
            R = Double.NaN;
        }
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
                +"\nSpearman's Correlation (Rho): "+String.format("%.4f", Rho)
                +"\nKendall's Correlation (Tau): "+String.format("%.4f", Tau)
                +"\nMAE: "+String.format("%.4e",MAE)
                +"\nRMSE: "+String.format("%.4e",RMSE)
                +"\nMRE: "+String.format("%.4f",MRE)
                +"\nROC AUC: "+String.format("%.4f", ROC_AUC);
        return out;
    }

    @Override
    public Map<String, Double> getStatistics() {
        Map<String, Double> output = new TreeMap<>();

        output.put("NEvaluated", (double) NumberTested);
        output.put("R", R);
        output.put("Rho", Rho);
        output.put("Tau", Tau);
        output.put("MAE", MAE);
        output.put("RMSE", RMSE);
        output.put("MRE", MRE);
        output.put("ROCAUC", ROC_AUC);
        
        return output;
    }
}
