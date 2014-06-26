/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.rankers;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.optimization.algorithms.OptimizationHelper;
import magpie.utility.interfaces.Options;

/**
 * This is a template for classes that rank entries based on some sort of objective 
 * function. When implementing this class, you only need to provide the objective 
 * function. The final user must decide whether it uses measured or predicted data and
 * whether the goal is to maximize or minimize the objective function.
 * 
 * <p>TODO: Make this an "objective" function class
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class EntryRanker implements java.lang.Cloneable, Options {
    /** Whether to maximize or minimize the objective function */
    private boolean MaximizeFunction = false; 
    /** Whether to use the measured or predicted class variable */
    private boolean UseMeasured = false;

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    protected EntryRanker clone() {
        EntryRanker x; 
        try {
            x = (EntryRanker) super.clone(); 
        } catch (CloneNotSupportedException e) {
            throw new Error(e); // Do nothing (should never end up here)
        }
        return x;
    }

	/**
	 * Set whether to used measured (or predicted) class values
	 * @param useMeasured Desired option
	 */
	public void setUseMeasured(boolean useMeasured) {
		this.UseMeasured = useMeasured;
	}
	
	/**
	 * @return Whether this ranker is using measured or predicted class variables
	 */
	public boolean isUsingMeasured() {
		return UseMeasured;
	}
    
	/**
	 * Set whether this entry ranker will maximize objective function
	 * @param toMaximize Whether to maximize function
	 */
    public void setMaximizeFunction(boolean toMaximize) {
		this.MaximizeFunction = toMaximize;
	}
	
	/**
	 * @return Whether the goal is to maximize the objective
	 */
	public boolean isMaximizing() {
		return MaximizeFunction;
	}
	
    /** 
     * Some kind of objective function that returns a double when given an entry.<br>
     * <p>NOTE: This function needs to support using either the measured or predicted
     * class depending on the settings</p>
     * 
     * @param Entry Entry to be analyzed
     * @return Evaluation of target function
     */
    abstract public double objectiveFunction(BaseEntry Entry);

    /** 
     * Return the rank of entries that were sorted based on the objective function defined
     * in this class (also depends on the settings for Maximize and Measured. Will 
     * return the values for each instance (sorted in the same order as the
     * output, which require preallocating Values.<br>
     * <br><code>double[] Values = new double[Data.NEntries()];</code>
     * @param Data Dataset containing entries to be ranked
     * @param Values Pre-allocated, containing Data.NEntries() number of entries
     * @return Entry number ranked by objective function
     */
    public int[] rankEntries(Dataset Data, double[] Values) {
        if (Values.length != Data.NEntries())
            throw new Error("Values was not correctly pre-allocated");
        // Run the objective function on each entry
        for (int i=0; i<Data.NEntries(); i++)
            Values[i] = objectiveFunction(Data.getEntry(i));
        int[] rank = OptimizationHelper.sortAndGetRanks(Values, MaximizeFunction);
        return rank;
    }
    
    /**
     * Evaluate every entry in a dataset using the objective function
     * @param Data Dataset to be evaluated
     * @return Value of objective function for each entry
     */
    public double[] runObjectiveFunction(Dataset Data){
        double[] values = new double[Data.NEntries()];
        for (int i=0; i<Data.NEntries(); i++)
            values[i] = objectiveFunction(Data.getEntry(i));
        return values;
    }

    /** 
     * Calculate the rank of entries based on this objective function
     * @param Data Dataset containing entries to be ranked
     * @return Entry numbers descending in rank (0 is the best).
     */
    public int[] rankEntries(Dataset Data) {
        double[] value = new double[Data.NEntries()];
        return rankEntries(Data, value);
    }
    
    /**
     * Calculate the rank of entries based on this objective function
     * @param Data Dataset containing entries to be ranked
     * @param useMeasured Whether to use measured class variable
     * @return Entry numbers descending in rank (0 is the best)
     */
    public int[] rankEntries(Dataset Data, boolean useMeasured) {
        boolean original = UseMeasured;
        this.setUseMeasured(useMeasured);
        int[] ranks = rankEntries(Data);
        this.setUseMeasured(original);
        return ranks;
    }
}
