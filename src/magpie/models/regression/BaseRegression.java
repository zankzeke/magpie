/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.regression;


import java.util.List;
import magpie.analytics.BaseStatistics;
import magpie.models.BaseModel;
import magpie.analytics.RegressionStatistics;
import magpie.data.Dataset;
import magpie.data.utilities.filters.IsOutlierFilter;

/**
 * Base class for all regression models.
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>robust &lt;Q></b> - Define False Positive Rate used for robust regression. See <a href="http://www.ncbi.nlm.nih.gov/pmc/articles/PMC1472692/">Motulsky and Brown</a>
 * <br><pr><i>Q</i>: Desired FPR during outlier detection.</command>
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseRegression extends BaseModel implements AbstractRegressionModel {
    /** Robust regression parameter. Default: Don't do it */
    private double Q = 0; 
    
    public BaseRegression() {
        TrainingStats = (BaseStatistics) new RegressionStatistics();
        ValidationStats = (BaseStatistics)  new RegressionStatistics();
    }
    
    @Override public BaseRegression clone() {
        BaseRegression x = (BaseRegression) super.clone();
        return x;
    }

    @Override
    public void robustTraining(Dataset TrainData) {
        if (doRobustRegression()) {
            // Step 1: Train the model on the supplied data
            train_protected(TrainData); 
            
            /* 
             * Step 2: Clone data. Call run_protected (this data has already been through
             * an attribute selector, so we can't use run) to get results. Remove outliers from
             * this dataset.
             */
            Dataset WithoutOutliers = TrainData.clone();
            run_protected(WithoutOutliers);
            IsOutlierFilter Filter = new IsOutlierFilter();
            Filter.setExclude(true);
            Filter.setQ(getRobustRegressionQ());
            Filter.setK(getNFittingParameters());
            Filter.filter(WithoutOutliers);
            
            /**
             * Step 3: Train model on data without outliers
             */
            train_protected(WithoutOutliers);
        } else
            train_protected(TrainData);
    }

    @Override
    public void setRobustRegressionQ(double Q) {
        this.Q = Q;
    }

    @Override
    public double getRobustRegressionQ() {
        return Q;
    }

    @Override
    public boolean doRobustRegression() {
        return Q > 0;
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.size() > 1) {
            String Action = Command.get(0).toString().toLowerCase();
            switch (Action) {
                case "robust": {
                    double Q;
                    try { Q = Double.parseDouble(Command.get(1).toString()); }
                    catch (Exception e) {
                        throw new Exception("Usage: robust <Q>");
                    }
                    setRobustRegressionQ(Q);
                    return null;
                } 
            }
        }
        return super.runCommand(Command);
    }
    
    
}
