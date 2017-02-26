package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * Filter entries based on value of class variable.
 * 
 * <usage><p><b>Usage</b>: &lt;measured|predicted&gt; &lt;Criteria&gt; &lt;Threshold&gt;
 * <br><pr><i>measured|predicted</i>: Whether to use measured or predicted class variable
 * <br><pr><i>Criteria</i>: Comparison operator used to filter data
 * <br><pr><i>Threshold</i>: Value to which class is compared</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class ClassFilter extends ComparisonOperatorFilter {
    /** Whether to use measured class variable */
	protected boolean UseMeasured = false;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            setOptions(Options[0], Options[1], Options[2]);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <measured|predicted> <Criteria> <Threshold>";
    }

    /**
	 * Set whether to use the measured class variable.
	 * @param value Desired setting
	 */
	public void setUseMeasured(boolean value) {
		this.UseMeasured = value;
	}
	
    /**
     * Configure the filter by defining which entries should be kept
     * @param Measured Should be either measured or predicted
     * @param Criteria Criteria used to test for inclusion
     * @param Threshold Threshold used to test for inclusion
	 * @throws Exception
     */
    public void setOptions(String Measured, String Criteria, String Threshold) throws Exception {
        if (Measured.toLowerCase().startsWith("me")) {
			UseMeasured = true;
		} else if (Measured.toLowerCase().startsWith("pr")) {
			UseMeasured = false;
		} else {
			throw new Exception();
		}
		
        try { setThreshold(Double.parseDouble(Threshold)); }
        catch (NumberFormatException e) { throw new Exception("Error parsing Threshold: " + e); }
		
		setComparisonOperator(Criteria);
    }

    @Override
    public boolean[] label(Dataset D) {
        double[] data = UseMeasured ? D.getMeasuredClassArray() : D.getPredictedClassArray();
        return testCriteria(data);
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing needs to be done
    }
}
