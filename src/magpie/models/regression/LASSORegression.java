package magpie.models.regression;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;
import magpie.utility.MathUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * This class enables regression using the LASSO algorithm, as described by 
 * <a href="http://www-stat.stanford.edu/~tibs/lasso/simple.html">this page</a>. It is 
 * basically linear regression with some additional constraints to make the solution vector
 * have a low L<sub>1</sub> norm.<p>
 * 
 * For now, this uses Forward Stepwise Regression.
 * 
 * <usage><p><b>Usage</b>: -maxterms &lt;terms>
 * <br><pr><i>terms</i>: Maximum number of terms allowed in expression</usage>
 * 
 * @author Logan Ward
 * @version 1.0
 */
public class LASSORegression extends BaseRegression {
    /** Maximum number of features allowed in model (-1 is unlimited) */
    protected int MaxNumberTerms = -1;
    /** Terms used in the model */
    protected List<Integer> Terms = new LinkedList<>();
    /** Names of terms used in the model */
    protected List<String> TermNames = new LinkedList<>();
    /** Coefficients for linear model */
    protected List<Double> Coefficients = new LinkedList<>();
    /** Intercept of the linear model */
    protected double Intercept;
    
    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public BaseRegression clone() {
        LASSORegression x = (LASSORegression) super.clone();
        x.Coefficients = new LinkedList<>(Coefficients);
        x.Terms = new LinkedList<>(Terms);
        x.TermNames = new LinkedList<>(TermNames);
        return x;
    }

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            int count = 0;
            while (count < Options.length) {
                switch (Options[count].toLowerCase()) {
                    case "-maxterms":
                        count++;
                        int maxterms = Integer.parseInt(Options[count]);
                        setMaxNumberTerms(maxterms);
                        break;
                    default:
                        throw new Exception();
                }
                count++;
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: -maxterms <maximum # terms>";
    }
    
    public void setMaxNumberTerms(int MaxNumberTerms) {
        this.MaxNumberTerms = MaxNumberTerms;
    }
    
    @Override
    protected void train_protected(Dataset TrainData) {
        /* Reset the coefficient vector */
        Coefficients.clear(); TermNames.clear(); Terms.clear();
        
        /* Gather and normalize data (NOT CURRENTLY USED)
        double [][] features = MathUtils.transpose(TrainData.getAttributeArray());
        FeatureMean = new double[features.length];
        FeatureDev = new double[features.length];
        for (int i=0; i<features.length; i++) {
            FeatureMean[i] = StatUtils.mean(features[i]);
            double temp = StatUtils.variance(features[i], FeatureMean[i]);
            FeatureDev[i] = temp == 0 || temp == Double.NaN ?
                    1.0 : Math.sqrt(temp);
            for (int j=0; j<features[i].length; j++)
                features[i][j] = (features[i][j] - FeatureMean[i]) / FeatureDev[i];
        }
        */
        double [][] observations = TrainData.getAttributeArray();
        double [][] features = MathUtils.transpose(observations);
        
        /* Retrieve the class */
        double[] targetClass = TrainData.getMeasuredClassArray();
        
        /* Do a LASSO fit. Algorithm:
         *   1) Select the parameter that correlates best with class variable
         *   2) Add that parameter to the model
         *   3) Determine the coefficient and intercept that minimizes error (L2 norm)
         *   4) Fit the parameter that correlates best with residual
         *   5) Determine parameter that has the best correlation with residual and
         *       is not already in the model
         *   6) Find the coefficient that minimizes error with residual
         *   7) Repeat from 4 until maximum number of terms is reached or error 
         *       passes below a threshold
         */
        // 1-3) Fit first term and intercept
        boolean[] includable = new boolean[features.length];
        Arrays.fill(includable, true);
        int toAdd = findMaxCorrelation(features, targetClass, includable);
        includable[toAdd] = false; // Remove from selection pool
        double[] results = linearFit(features[toAdd], targetClass, true);
        Intercept = results[0];
        Coefficients.add(results[1]);
        Terms.add(toAdd);
        TermNames.add(TrainData.getAttributeName(toAdd));
        
        // Find the residuals
        double[] residual = getResidual(observations, targetClass);
        double MAE = getMAE(residual);
        int termCount = 1;
        
        // 4-7) Add new terms to model
        while (termCount != MaxNumberTerms) {
            toAdd = findMaxCorrelation(features, residual, includable);
            results = linearFit(features[toAdd], residual, true);
            Coefficients.add(results[1]);
            Terms.add(toAdd);
            TermNames.add(TrainData.getAttributeName(toAdd));
            Intercept += results[0];
            includable[toAdd] = false;
            residual = getResidual(observations, targetClass);
            MAE = getMAE(residual);
            termCount++;
        }
    }   

    @Override
    public int getNFittingParameters() {
        return Coefficients.size() + 1;
    }

    @Override
    public void run_protected(Dataset TrainData) {
        double[] predictedClass = runModel(TrainData.getAttributeArray());
        TrainData.setPredictedClasses(predictedClass);
    }
    
    /**
     * Finds the feature with the maximum correlation out of all possible candidates
     * @param features Features for each measurement
     * @param objective Objective function for each measurement
     * @param isSearchable List of which features are searchable
     * @return Index of feature that is searchable and has the highest correlation coefficient
     */
    protected int findMaxCorrelation(double[][] features, double[] objective, 
            boolean[] isSearchable) {
        int best = -1;
        double corr_best = -15, corr;
        for (int i=0; i<features.length; i++){
            if (!isSearchable[i]) continue;
            corr = new PearsonsCorrelation().correlation(features[i], objective);
            if (Double.isNaN(corr)) corr = 0.0;
            else corr = Math.abs(corr);
            if (corr > corr_best) {
                corr_best = corr; best = i;
            }
        }
        return best;
    }
    
    /**
     * Find the best linear fit
     * @param x Independent variable
     * @param y Dependent variable
     * @param intercept Whether to fit an intercept
     * @return Optimal fit: [0] is the intercept, [1] is the slope
     */
    static public double[] linearFit(double[] x, double[] y, boolean intercept) {
        SimpleRegression Fit = new SimpleRegression(intercept);
        for (int i=0; i<x.length; i++)
            Fit.addData(x[i], y[i]);
        double[] output = new double[2];
        if (intercept) output[0] = Fit.getIntercept();
        output[1] = Fit.getSlope();
        return output;
    }
    
    /**
     * Determines the residuals from a model
     * @param x Observation matrix
     * @param y Data to compare against model
     * @return <code>y - runModel(x)</code>
     */
    public double[] getResidual(double[][] x, double[] y) {
        double[] yhat = runModel(x);
        double[] residuals = new double[y.length];
        for (int i=0; i<y.length; i++)
            residuals[i] = y[i] - yhat[i];
        return residuals;
    }
    
    /**
     * Runs the model contained within object
     * @param x Observation matrix
     * @return 
     */
    public double[] runModel(double[][] x){
        double[] y = new double[x.length];
        Arrays.fill(y, Intercept);
        for (int i=0; i<x.length; i++) {
            for (int j=0; j<Coefficients.size(); j++)
                y[i] += Coefficients.get(j) * x[i][Terms.get(j)];
        }
        return y;
    }
    
    /**
     * Get Mean Absolute Error (MAE) given residuals
     * @param residuals Error between model and reality for each entry
     * @return Mean absolute error
     */
    static public double getMAE(double[] residuals) {
        double[] copy = new double[residuals.length];
        for (int i=0; i<residuals.length; i++) copy[i] = Math.abs(residuals[i]);
        return StatUtils.mean(copy);
    }

    @Override
    protected String printModel_protected() {
        String output = "Class = " + String.format("%.5e", Intercept);
        for (int i=0; i < Terms.size(); i++)
            output += " + " + String.format("%.5e * %s", Coefficients.get(i), TermNames.get(i));
        return output + "\n";
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
