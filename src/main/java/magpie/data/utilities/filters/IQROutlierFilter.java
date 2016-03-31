package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Filters entries based on whether the class values are outliers using the interquartile range. 
 * Any entry with a measured class variable more than a certain number of interquartile ranges
 * from the median is classified as an outlier
 * 
 * <usage><p><b>Usage</b>: &lt;threshold&gt;
 * <br><pr><i>threshold</i>: Fraction of interquartile range away from median
 * that data is considered an outlier.
 * </usage>
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
        if (Double.isNaN(ICQ)) {
            throw new RuntimeException("Filter hasn't been trained");
        }
        
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
		Median = StatUtils.percentile(classValues, 50);
		ICQ = StatUtils.percentile(classValues, 75) - StatUtils.percentile(classValues, 25);
    }
}
