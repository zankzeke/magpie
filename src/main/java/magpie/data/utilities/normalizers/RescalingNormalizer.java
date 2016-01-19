
package magpie.data.utilities.normalizers;

import java.util.List;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Scale variables using the maximum and minimum values of the training set. Applies
 *  the transformation
 * <center>x&rsquo; = 2 * ( x - min(X) ) / (max(X) - min(X)) - 1</center>
 * where x is the value of an attribute, X is the set of all values of that variable
 * in the training set, and x&rsquo; is the normalized attribute. For examples where
 * the range of X is measured to be 0, the range is assumed to be 1.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * @author Logan Ward
 */
public class RescalingNormalizer extends IndependentVariableNormalizer {
    /** Minimum value of each attribute */
    private double[] AttributeMinimum;
    /** Range of each attribute */
    private double[] AttributeRange;
    /** Minimum value of class variable */
    private double ClassMinimum;
    /** Range of class variable */
    private double ClassRange;

    @Override
    public RescalingNormalizer clone() {
        RescalingNormalizer x = (RescalingNormalizer) super.clone();
        x.AttributeMinimum = AttributeMinimum.clone();
        x.AttributeMinimum = AttributeRange.clone();
        return x;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    protected void prepareAttributeArrays(int NAttributes) {
        AttributeMinimum = new double[NAttributes];
        AttributeRange = new double[NAttributes];
    }

    @Override
    protected void computeAttributeStatistics(int attributeNumber, double[] values) {
        AttributeMinimum[attributeNumber] = StatUtils.min(values);
        AttributeRange[attributeNumber] = StatUtils.max(values) - AttributeMinimum[attributeNumber];
        if (AttributeRange[attributeNumber] == 0) {
            AttributeRange[attributeNumber] = 1;
        }
    }

    @Override
    protected void computeClassStatistics(double[] values) {
        ClassMinimum = StatUtils.min(values);
        ClassRange = StatUtils.max(values) - ClassMinimum;
        if (ClassRange == 0) { 
            ClassRange = 1; 
        }
    }

    @Override
    protected void normalizeAttributes(double[] attributes) {
        for (int i=0; i<attributes.length; i++) {
            attributes[i] = normalizationFunction(attributes[i], AttributeMinimum[i], AttributeRange[i]);
        }
    }

    @Override
    protected double normalizeClassVariable(double value) {
        return normalizationFunction(value, ClassMinimum, ClassRange);
    }
    
    

    /**
     * Compute the normalized value of a variable
     * @param variable Value of variable to be normalized
     * @param minimum Minimum of this variable (from training set)
     * @param range Measured range of this variable
     * @return Normalized value
     */
    static private double normalizationFunction(double variable, double minimum, double range) {
        return 2 * (variable - minimum) / range - 1;
    }
    

    @Override
    protected void restoreAttributes(double[] attributes) {
        for (int i=0; i<attributes.length; i++) {
            attributes[i] = restorationFunction(attributes[i], AttributeMinimum[i], AttributeRange[i]);
        }
    }

    @Override
    protected double restoreClassVariable(double value) {
        return restorationFunction(value, ClassMinimum, ClassRange);
    }
    
    /**
     * Compute the restored value a variable 
     * @param variable Value of variable to be restored
     * @param minimum Minimum of that variable (from training set)
     * @param range Measured range of this variable
     * @return Restored value
     */
    static private double restorationFunction(double variable, double minimum, double range) {
        return (variable + 1) * range / 2 + minimum;
    }
}
