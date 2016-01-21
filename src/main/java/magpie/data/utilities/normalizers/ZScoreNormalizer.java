
package magpie.data.utilities.normalizers;

import java.util.List;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Scale variables by calculating their Z scores. Applies the transformation
 * <center>x&rsquo; = ( x - mean(X) ) / stdev(X) </center>
 * where x is the value of an attribute, X is the set of all values of that variable
 * in the training set, and x&rsquo; is the normalized attribute. For examples where
 * the range of X is measured to be 0, the range is assumed to be 1.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * @author Logan Ward
 */
public class ZScoreNormalizer extends IndependentVariableNormalizer {
    /** Minimum value of each attribute */
    private double[] AttributeMean;
    /** Range of each attribute */
    private double[] AttributeStdDev;
    /** Minimum value of class variable */
    private double ClassMean;
    /** Range of class variable */
    private double ClassStdDev;

    @Override
    public ZScoreNormalizer clone() {
        ZScoreNormalizer x = (ZScoreNormalizer) super.clone();
        x.AttributeMean = AttributeMean.clone();
        x.AttributeStdDev = AttributeStdDev.clone();
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
        AttributeMean = new double[NAttributes];
        AttributeStdDev = new double[NAttributes];
    }

    @Override
    protected void computeAttributeStatistics(int attributeNumber, double[] values) {
        AttributeMean[attributeNumber] = StatUtils.mean(values);
        AttributeStdDev[attributeNumber] = Math.sqrt(StatUtils.variance(values, AttributeMean[attributeNumber]));
        if (AttributeStdDev[attributeNumber] == 0) {
            AttributeStdDev[attributeNumber] = 1;
        }
    }

    @Override
    protected void computeClassStatistics(double[] values) {
        ClassMean = StatUtils.mean(values);
        ClassStdDev = StatUtils.variance(values, ClassMean);
        if (ClassStdDev == 0) { 
            ClassStdDev = 1; 
        }
    }

    @Override
    protected void normalizeAttributes(double[] attributes) {
        for (int i=0; i<attributes.length; i++) {
            attributes[i] = normalizationFunction(attributes[i], AttributeMean[i], AttributeStdDev[i]);
        }
    }

    @Override
    protected double normalizeClassVariable(double value) {
        return normalizationFunction(value, ClassMean, ClassStdDev);
    }
    
    /**
     * Compute the normalized value of a variable
     * @param variable Value of variable to be normalized
     * @param mean Mean of this variable (from training set)
     * @param stdev Standard deviation of this variable
     * @return Normalized value
     */
    protected double normalizationFunction(double variable, double mean, double stdev) {
        return (variable - mean) / stdev;
    }
    

    @Override
    protected void restoreAttributes(double[] attributes) {
        for (int i=0; i<attributes.length; i++) {
            attributes[i] = restorationFunction(attributes[i], AttributeMean[i], AttributeStdDev[i]);
        }
    }

    @Override
    protected double restoreClassVariable(double value) {
        return restorationFunction(value, ClassMean, ClassStdDev);
    }
    
    /**
     * Compute the restored value a variable 
     * @param variable Value of variable to be restored
     * @param mean Mean of variable (from training set)
     * @param stdev Measured standard deviation of variable
     * @return Restored value
     */
    protected double restorationFunction(double variable, double mean, double stdev) {
        return variable * stdev + mean;
    }
}
