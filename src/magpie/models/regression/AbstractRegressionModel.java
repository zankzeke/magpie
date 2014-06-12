/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.regression;

import magpie.data.Dataset;
import magpie.models.BaseModel;

/**
 * Interface for all regression models. 
 * @author Logan Ward
 * @version 0.1
 */
public interface AbstractRegressionModel {
    
    /**
     * Robustly train a model. Is called from {@link BaseModel#train}.
     * <p> I recommend that you use methods outlined in a paper by <a href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC1472692/">
     * Motulsky and Brown</a>.</p>.
     * @param TrainData Training data
     */
    public void robustTraining(Dataset TrainData);
    
    /**
     * Set the desired False Discovery Rate for outlier detection. A robust regression parameter of
     * 0.01 is recommended. Zero turns it off. Don't give it less than zero.
     * <p>See paper by <a href="http://www.jstor.org/stable/10.2307/2346101">
     * Benjamini and Hochberg</a>
     * @param Q Desired FDR
     */
    public void setRobustRegressionQ(double Q);
    
    /**
     * Get desired False Discovery Rate
     * @return Target FDR
     */
    public double getRobustRegressionQ();
    
    /**
     * Whether robust regression is desired. For now, this should be:<br>
     * <code>getRobustRegressionQ &gt; 0</code>
     */
    public boolean doRobustRegression();
    
    /**
     * Number of fitting parameters in a model. Only pertinent to nonlinear regression. 
     * Otherwise, this should just return 0.
     * @return Number of fitting parameters
     */
    public int getNFittingParameters();
}
