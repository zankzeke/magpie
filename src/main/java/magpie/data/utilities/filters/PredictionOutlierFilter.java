package magpie.data.utilities.filters;

import java.util.List;
import magpie.analytics.utility.RobustRegressionUtility;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * Filters entries based on whether they are outliers. Uses {@linkplain RobustRegressionUtility} and
 *  methods described by <a href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC1472692/">
 * Motulsky and Brown</a> to detect outliers. Data must have a both a measured and predicted class variable.
 * 
 * <usage><p><b>Usage</b>: [-q &lt;Q>] [-k &lt;K> [-frac &lt;fraction>]
 * <br><pr><i>Q</i>: Target false discovery rate
 * <br><pr><i>K</i>: Number of fitting parameters in model
 * <br><pr><i>fraction</i>: Fraction of entries to check for being an outlier (starts with those with largest errors)</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PredictionOutlierFilter extends BaseDatasetFilter {
    /** Class used to calculate outliers */
    private final RobustRegressionUtility RobustUtility = new RobustRegressionUtility();
    /** Number of fitting parameters in a model */
    private int K;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            if (Options.length == 0) return;
            int i=-1;
            while (i + 1 < Options.length) {
                i++; 
                switch (Options[i].toLowerCase()) {
                    case "-q": 
                        i++; setQ(Double.parseDouble(Options[i])); break;
                    case "-k": i++; K = Integer.parseInt(Options[i]); break;
                    case "-frac": 
                        i++; RobustUtility.setTestFraction(Double.parseDouble(Options[i])); break;
                    default: 
                        throw new Exception();
                }
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: [ -q <Q> ] [ -k <K> ] [-frac <fraction>]";
    }
    
    
    
    /**
     * Set target False Discovery rate. See {@link RobustRegressionUtility}.
     * @param Q Target FDR
     */
    public void setQ(double Q) {
        RobustUtility.setQ(Q);
    }
    
    /** 
     * Define number of fitting parameters in model. See {@link RobustRegressionUtility}.
     * @param K Number of fitting parameters
     */
    public void setK(int K) {
        this.K = K;
    }

    @Override
    protected boolean[] label(Dataset D) {
        return RobustUtility.isOutlier(D, K);
    }

    @Override
    public void train(Dataset TrainingSet) {
        /* Nothing to do */
    }
}
