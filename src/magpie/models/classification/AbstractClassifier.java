/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.classification;

/**
 * Interface that defines what operations a Classifier must fulfill
 * @author Logan Ward
 */
public interface AbstractClassifier {

    /** @return Whether the class variable is treated as discrete */
    boolean classIsDiscrete();

    /**
     * @return Class cutoff used when calculating statistics
     */
    double getClassCutoff();

    /**
     * @return Number of classes model will distinguish between
     */
    int getNClasses();

    /** Allow the class variable to be treated as continuous */
    void setClassContinuous();
    
    /** Allow the class variable to be treated as discrete */
    void setClassDiscrete();

    /** 
     * Set the class cutoff used when calculating statistics.
     * @param x Class cutoff (0 <= x <= 1)
     */
    void setClassCutoff(double x);
    
}
