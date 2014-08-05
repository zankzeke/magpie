/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Filters entries based on whether they are outliers using the interquartile range. 
 * Any entry with a measured class variable more than a certain number of interquartile ranges
 * from the median is classified as an outlier
 * 
 * <usage><p><b>Usage</b>: &lt;threshold&gt;
 * <br><pr><i>Q</i>: Target false discovery rate
 * <br><pr><i>K</i>: Number of fitting parameters in model
 * <br><pr><i>fraction</i>: Fraction of entries to check for being an outlier (starts with those with largest errors)</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class IQROutlierFilter extends BaseDatasetFilter {
	/** Maximum number of IQRs away a non-outlier can be */
	private double Threshold = 2.0;
    /** Median from training set */
	private double Median = Double.NaN;
	/** IQR from training set */
	private double ICQ = Double.NaN;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        try {
			setThreshold(Double.parseDouble(OptionsObj.get(0).toString()));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

	/**
	 * Define number of IQRs away from the median that an entries is an outliers. 
	 * @param Threshold Desired threshold (default = 2)
	 */
	public void setThreshold(double Threshold) {
		this.Threshold = Threshold;
	}

    @Override
    public String printUsage() {
        return "Usage: <threshold>";
    }

	@Override
	protected boolean[] label(Dataset D) {
		boolean[] output = new boolean[D.NEntries()];
		for (int i=0; i<D.NEntries(); i++) {
			double classValue = D.getEntry(i).getMeasuredClass();
			output[i] = Math.abs(classValue - Median) / ICQ > Threshold;
		}
		return output;
	}

    @Override
    public void train(Dataset TrainingSet) {
        double[] classValues = TrainingSet.getMeasuredClassArray();
		Median = StatUtils.percentile(classValues, 0.5);
		ICQ = StatUtils.percentile(classValues, 0.75) - StatUtils.percentile(classValues, 0.75);
    }
}
