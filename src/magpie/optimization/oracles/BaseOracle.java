/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.oracles;

import magpie.data.Dataset;
import magpie.utility.interfaces.Options;

/**
 * Interface for all Oracle classes, which manage the evaluation
 * of new entries (useful for optimization algorithms)
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseOracle implements java.io.Serializable, 
        java.lang.Cloneable, Options {
    /** Dataset containing entries currently being evaluated */
    protected Dataset ToEvaluate = null;
    
    /**
     * Adds a new dataset to be evaluated. Throws error if this Oracle is 
     * already working on something.
     * @param NewData Dataset of entries to be evaluated
     */
    public void importDataset(Dataset NewData) {
        if (ToEvaluate != null)
            throw new Error("Oracle has not finished evaluating last batch");
        ToEvaluate = NewData;
    }
    
    /**
     * If the oracle is finished, returns the dataset with all evaluated entries. Sets
     * ToEvaluate back to null, allowing for the Oracle to receive a new set of data.
     * @return Reference to to evaluated dataset
     */
    public Dataset extractDataset() {
        if (!isComplete())
            throw new Error("Oracle has not finished evaluating entries");
        Dataset output = ToEvaluate;
        ToEvaluate = null;
        return output;
    }
    
    /**
     * Check whether every entry has been evaluated
     * @return Whether each entry in ToEvaluate has a measured class
     */
    public boolean isComplete() {
        if (ToEvaluate == null)
            throw new Error("No data has been imported");
        for (int i=0; i < ToEvaluate.NEntries(); i++)
            if (! ToEvaluate.getEntry(i).hasMeasurement()) 
                return false;
        return true;
    }
    
    /**
     * Run all necessary calculations to determine the measured class of each entry
     * Throws error if no entries have been imported
     */
    public void evaluateEntries() {
        if (ToEvaluate == null) throw new Error("No data has been imported");
        evaluate_protected();
    }
    
    /**
     * Operation which actually does the work
     */
    abstract protected void evaluate_protected();
    
    @Override@SuppressWarnings("CloneDeclaresCloneNotSupported")
    public BaseOracle clone() {
        BaseOracle x;
        try { x = (BaseOracle) super.clone(); }
        catch (CloneNotSupportedException c) { throw new Error(c); }
        x.ToEvaluate = ToEvaluate.clone();
        return x;
    }
}
