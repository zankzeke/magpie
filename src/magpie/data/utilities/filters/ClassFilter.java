/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
public class ClassFilter extends BaseDatasetFilter {
    /** Whether to use measured class variable */
	protected boolean UseMeasured = false;
    /** Threshold value */
    protected double Threshold = 0.0;
    /** Should entries with Feature==Threshold be kept? */
    protected boolean Equal = true;
    /** Should entries with Feature &lt; Threshold be kept? */
    protected boolean GreaterThan = true;
    /** Should entries with Feature &gt; Threshold be kept? */
    protected boolean LessThan = true;
    
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
	 * Set the comparison threshold.
	 * @param Threshold Desired threshold
	 */
	public void setThreshold(double Threshold) {
		this.Threshold = Threshold;
	}
    
	/**
	 * Define comparison operator. Can be &gt;, le, eq, "!=", and such.
	 * @param operator Desired operator
	 */
	public void setComparisonOperator(String operator) throws Exception {
		 switch(operator.toLowerCase()) {
            case "eq": case "=": case "==":
                Equal = true; GreaterThan = false; LessThan = false; break;
            case "ne": case "!=": case "~=": case "<>":
                Equal = false; GreaterThan = true; LessThan = true; break;
            case "gt": case ">":
                Equal = false; GreaterThan = true; LessThan = false; break;
            case "ge": case ">=": case "=>":
                Equal = true; GreaterThan = true; LessThan = false; break;
            case "lt": case "<":
                Equal = false; GreaterThan = false; LessThan = true; break;
            case "le": case "<=": case "=<":
                Equal = true; GreaterThan = false; LessThan = true; break;
            default:
                throw new Exception("Criteria \"" + operator + "\" not recognized.");
        }
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
    
    /**
     * Evaluate the filtering criteria on each member of an array
     * @param value Values to be tested
     * @return Boolean indicating whether they pass the criteria
     */
    protected boolean[] testCriteria(double[] value) {
        boolean[] passes = new boolean[value.length]; // Starts out false
        for (int i=0; i < passes.length; i++) {
            if (Equal && value[i] == Threshold)
                passes[i] = true;
            else if (GreaterThan && value[i] > Threshold) 
                passes[i] = true;
            else if (LessThan && value[i] < Threshold) 
                passes[i] = true;
        }
        return passes;
    }

    @Override
    protected boolean[] label(Dataset D) {
        double[] data = UseMeasured ? D.getMeasuredClassArray() : D.getPredictedClassArray();
        return testCriteria(data);
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing needs to be done
    }
}
