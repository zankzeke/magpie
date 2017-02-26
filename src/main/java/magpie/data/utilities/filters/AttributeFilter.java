package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * Filter entries based on value of a single attribute.
 * 
 * <usage><p><b>Usage</b>: &lt;Target Attribute> &lt;Operator> &lt;Threshold>
 * <br><pr><i>Target Attribute</i>: Name of attribute to use for filtering
 * <br><pr><i>Opterator</i>: Comparison operator used to filter data. Can be: &lt;, &le;, >, &ge;, =, and &ne;
 * <br><pr><i>Threshold</i>: Value to which attribute is compared</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class AttributeFilter extends ComparisonOperatorFilter {
    /** Name of feature to base filter on */
    protected String TargetAttribute = null;
    
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
        return "Usage: <Target Attribute> <Operator> <Threshold>";
    }
    
    /**
     * Configure the filter by defining which entries should be kept
     * @param TargetFeature Which feature to filter
     * @param Operator Operator used to test for inclusion
     * @param Threshold Threshold used to test for inclusion
     */
    public void setOptions(String TargetFeature, String Operator, String Threshold) {
        setTargetAttribute(TargetFeature);
        try { setThreshold(Double.parseDouble(Threshold)); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error parsing Threshold: " + e);
        }
        setComparisonOperator(Operator);
    }

    /**
     * Set the attribute being compared
     * @param targetAttribute Name of attribute used in filtering
     */
    public void setTargetAttribute(String targetAttribute) {
        this.TargetAttribute = targetAttribute;
    }


    @Override
    public boolean[] label(Dataset D) {
        int attributeID = D.getAttributeIndex(TargetAttribute);
        if (attributeID == -1)
            throw new Error("Attribute " + TargetAttribute + " not found in Dataset");
        double[] feature = D.getSingleAttributeArray(attributeID);
        return testCriteria(feature);
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing needs to be done
    }
}
