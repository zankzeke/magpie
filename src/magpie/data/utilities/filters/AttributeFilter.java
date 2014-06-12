/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * Filter entries based on value of a single attribute.
 * 
 * <usage><p><b>Usage</b>: &lt;Target Attribute> &lt;Criteria> &lt;Threshold>
 * <br><pr><i>Target Attribute</i>: Attribute on which data is filtered
 * <br><pr><i>Criteria</i>: Comparison operator used to filter data. Can be: &lt;, &le;, >, &ge;, =, and &ne;
 * <br><pr><i>Threshold</i>: Value to which attribute is compared</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class AttributeFilter extends BaseDatasetFilter {
    /** Name of feature to base filter on */
    protected String TargetAttribute = null;
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
        return "Usage: <Target Attribute> <Criteria> <Threshold>";
    }
    
    /**
     * Configure the filter by defining which entries should be kept
     * @param TargetFeature Which feature to filter
     * @param Criteria Criteria used to test for inclusion
     * @param Threshold Threshold used to test for inclusion
     */
    public void setOptions(String TargetFeature, String Criteria, String Threshold) {
        this.TargetAttribute = TargetFeature;
        try { this.Threshold = Double.parseDouble(Threshold); }
        catch (NumberFormatException e) { throw new Error("Error parsing Threshold: " + e); }
        switch(Criteria.toLowerCase()) {
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
                throw new Error("Criteria \"" + Criteria + "\" not recognized.");
        }
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
        int FeatureID = D.AttributeName.indexOf(TargetAttribute);
        if (FeatureID == -1)
            throw new Error("Attribute "+TargetAttribute+" not found in Dataset");
        double[] feature = D.getSingleAttributeArray(FeatureID);
        return testCriteria(feature);
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing needs to be done
    }
}
