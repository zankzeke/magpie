/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.utilities.normalizers;

import java.util.List;

/**
 * Normalization using the "softmax" method. First computes the mean and standard
 *  deviation of a variable, then computes a normalized value using the relationship
 * 
 * <center>y = x - mean(X) / r / stddev(x)</center>
 * <center>x&rsquo; = 1 / (1 + exp(-y))</center>
 * 
 * where x is the value of an attribute, X is the set of all values of that variable
 * in the training set, and x&rsquo; is the normalized attribute. For examples where
 * the range of X is measured to be 0, the range is assumed to be 1. "r" is a scaling
 * factor selected by the user.
 * 
 * <usage><p><b>Usage</b>: &lt;r&gt;
 * <br><pr><i>r</i>: Scaling constant (default = 1)</usage>
 * 
 * @author Logan Ward
 */
public class SoftmaxNormalizer extends ZScoreNormalizer {
    /** Scaling factor */
    private double R = 1;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            if (Options.isEmpty()) return;
            if (Options.size() == 1) {
                setR(Double.parseDouble(Options.get(0).toString()));
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    public SoftmaxNormalizer() {
    }

    @Override
    public String printUsage() {
        return "Usage: <r>";
    }
    
    /**
     * Set the scaling constant
     * @param R Scaling constant (should be positive, default is 1)
     */
    public void setR(double R) {
        this.R = R;
    }

    /**
     * Get the scaling constant
     * @return Scaling constant
     */
    public double getR() {
        return R;
    }

    @Override
    protected double normalizationFunction(double variable, double mean, double stdev) {
        double Zscore = super.normalizationFunction(variable, mean, stdev); 
        return 1.0 / (1.0 + Math.exp(-1 * Zscore / R));
    }

    @Override
    protected double restorationFunction(double variable, double mean, double stdev) {
        double y = -1 * Math.log(1.0 / variable - 1);
        return super.restorationFunction(y * R, mean, stdev);
    }
}
