/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.rankers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Rank entries based on the distance of their class variable from a target.
 * 
 * <usage><p><b>Usage</b>: &lt;target>
 * <br><pr><i>target</i>: Target value used when ranking entries</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class TargetEntryRanker extends BaseEntryRanker {
    /** Target value of class */
    protected double Target = 0.0;

    /**
     * Create a new instance while specifying the target
     * @param Target Target value for class variable
     */
    public TargetEntryRanker(double Target) {
        this.Target = Target;
    }

    /**
     * Create a new instance the target set to 0.0
     */
    public TargetEntryRanker() {}
    

    @Override
    public void setOptions(List Options) throws Exception {
        try {
            String value = Options.get(0).toString(); 
            Target = Double.parseDouble(value);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <target value>";
    }
    
    @Override 
    public double objectiveFunction(BaseEntry Entry) {
        double error;
        if (isUsingMeasured()) error = Entry.getMeasuredClass()-Target;
        else error = Entry.getPredictedClass()-Target;
        return Math.abs(error);
    }

	@Override
	public void train(Dataset data) {
		/** Nothing to do */
	}
	
	
}
