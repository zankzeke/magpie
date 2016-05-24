package magpie.statistics.utility;

import java.util.Arrays;
import magpie.data.Dataset;
import magpie.optimization.algorithms.OptimizationHelper;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Calculates statistics useful to robust regression. Can perform the following operations
 * when provided {@link Dataset} that contains both measured and predicted class variables. 
 * 
 * <ul>
 * <li>Report the Robust Standard Deviation of Residuals</li>
 * <li>Determine which entries are outliers</li>
 * <li>Calculate the robust mean and standard deviation in absolute error.</li>
 * </ul>
 * <p>Uses methods outlined in a paper by <a href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC1472692/">
 * Motulsky and Brown</a>.</p>.
 * @author Logan Ward
 * @version 0.1
 */
public class RobustRegressionUtility {
    /** Threshold for considering an entry an outlier. Expressed as a desired
     * false discovery rate. See paper by <a href="http://www.jstor.org/stable/10.2307/2346101">
     * Benjamini and Hochberg</a>
     */
    protected double Q = 0.01;
    
    /** What fraction of the population to test */
    protected double TestFraction = 0.3;

    /**
     * Define the target False Discovery Rate for outlier selection 
     * @param q Desired Q parameter (see <a href="http://www.jstor.org/stable/10.2307/2346101">
     * Benjamini and Hochberg</a>)
     */
    public void setQ(double q) {
        this.Q = q;
    }

    /**
     * Define the fraction of entries that will be tested as potential outliers.
     * @param TestFraction Fraction of entries to test.
     */
    public void setTestFraction(double TestFraction) {
        this.TestFraction = TestFraction;
    }
     
    /**
     * Determines which entries in a dataset qualify as an outlier. for nonlinear regression purposes.
     * @param Data Dataset to screen
     * @return Whether each entry is an outlier
     */
    public boolean[] isOutlier(Dataset Data) {
        return isOutlier(Data,0);
    }
    
    /**
     * Determine which entries in a dataset qualify as an outlier, for nonlinear regression purposes.
     * @param Data Dataset to screen
     * @param K Number of fitting parameters in model
     * @return Whether each entry is an outlier
     */
    public boolean[] isOutlier(Dataset Data, int K) {
        if (Data.NEntries() == 0) return new boolean[0];
        if (! Data.getEntry(0).hasMeasurement())
            throw new Error("Data must have measured class variable");
        if (! Data.getEntry(0).hasPrediction())
            throw new Error("Data must have predicted class variable");
        boolean[] isOutlier = new boolean[Data.NEntries()];
        Arrays.fill(isOutlier, false); // Assume most are not outliers
        
        double[] predicted = Data.getPredictedClassArray();
        double[] measured = Data.getMeasuredClassArray();
        
        // Calculate the error and absolute error
        int N = Data.NEntries();
        double[] error = new double[N];
        for (int i=0; i<Data.NEntries(); i++)
            error[i] = predicted[i] - measured[i];
        
        // Calculate the population statistics
        double mean = StatUtils.mean(error);
        double RSDR = getRSDR(error, mean, K);
        
        // Rank entries by absolute, normalized residual
        double absoluteError[] = new double[Data.NEntries()];
        for (int i=0; i<absoluteError.length; i++)
            absoluteError[i] = Math.abs(error[i] - mean);
        int[] rank = OptimizationHelper.sortAndGetRanks(absoluteError, false);
        
        // Find at which point entries start becoming outliers. Method:
        //  1. Start at i = NEntries() * ( 1 - TestFraction )
        //  2. See whether entry rank[i] is an outlier by M&B's method
        //  3. If so, record i. Else, repeat while i < NEntries()
        int outliersFound = -1; double t, p, alpha;
        TDistribution TDist = new TDistribution(N - K);
        for (int i=(int) ((1-TestFraction) * N); i < Data.NEntries(); i++) {
            t = -1 * Math.abs(error[rank[i]] - mean) / RSDR;
            p = TDist.cumulativeProbability(t);
            alpha = Q * (N - i) / N;
            if (p < alpha) { outliersFound = i; break; }
        }
        // Mark them as outliers
        if (outliersFound != -1)
            for (int i=outliersFound; i<N; i++) isOutlier[rank[i]] = true;
                
        return isOutlier;
    }
    
    /**
     * Calculate the Robust Standard Deviation of Residuals. Follows the method described
     *  in <a href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC1472692/">. 
     * 
     * <p>NOTE: Their method seems to assume that the mean of residuals is 0, which
     * is likely for most functions. I remove this constraint by subtracting the mean
     * absolute error from the error of each point. This should not change results much.
     * 
     * Motulsky and Brown</a>
     * @param errors Errors for entire population (not magnitude of errors).
     * @param K Number of fitted parameters in the model
     * @return Robust Standard Deviation of Residuals
     */
    public double getRSDR(double[] errors, int K) {
        double mean = StatUtils.mean(errors);
        return getRSDR(errors, mean, K);
    }
    
    /**
     * Calculate RSDR if the mean is already known. See {@link #getRSDR(double[], int)} for
     * more details.
     * @param errors Errors for entire population (not magnitude of errors).
     * @param K Number of fitted parameters in the model
     * @param mean Mean error
     * @return Robust Standard Deviation of Residuals
     */
    public double getRSDR(double[] errors, double mean, int K) {
        double normalizedErrors[] = new double[errors.length];
        for (int i=0; i<errors.length; i++)
            normalizedErrors[i] = errors[i] - mean;
        double P68 = StatUtils.percentile(normalizedErrors, 68.27);
        return P68 * (double) errors.length / (double) (errors.length - K);
    }
    
}
