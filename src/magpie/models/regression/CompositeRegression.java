package magpie.models.regression;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.interfaces.MultiModel;
import magpie.models.utility.MultiModelUtility;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Ensemble of regression models. 
 * 
 * <p>Has the ability to return the variance in predictions between submodels, which 
 * can be used as a measure of their composite reliability.
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>submodel</b> - Print the number of submodels</command>
 *
 * <command><p><b>submodel set generic $&lt;model></b> - Define a model template to use for all submodels
 * <br><pr><i>model</i>: An instance of {@linkplain BaseModel}.
 * Note: Do not use this command for {@linkplain CompositeRegression} unless each
 * model automatically uses a different random number seed. Otherwise, each
 * submodel will be identical.</command>
 *
 * <command><p><b>submodel set &lt;number> $&lt;model</b> - Set a specific submodel
 * <br><pr><i>number</i>: Index of the submodel to set (list starts with 0)
 * <br><pr><i>model</i>: An instance of {@linkplain BaseModel} to use for that model</command>
 *
 * <command><p><b>&lt;output> = submodel get generic</b> - Retrieve the template for any unassigned submodels</command>
 *
 * <command><p><b>&lt;output = submodel get &lt;number></b> - Retrieve a specific submodel
 * <br><pr><i>number</i>: Index of submodel to retrieve (list starts with 0)
 * Returns a clone of the model - you cannot use this to edit the model.</command>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class CompositeRegression extends BaseRegression implements MultiModel {
    /** Collections of models used by this class */
    public List<BaseRegression> Model;
    /** Generic composite regression model (use wisely) */
    public BaseRegression GenericModel;

    public CompositeRegression() {
        Model = new LinkedList<>();
    }

    @Override
    public void setOptions(List Options) throws Exception {
        /* Nothing to set */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    @Override
    public BaseModel getModel(int index) {
        return Model.get(index);
    }

    /**
     * Get the number of models in ensemble
     * @return Number of models 
     */
    @Override
    public int NModels() {
        return Model.size();
    }
    
    @Override
    public void setNumberOfModels(int n) {
        // If we have too few, add generic models (or null)
        while (Model.size() < n)
            if (GenericModel == null) Model.add(null);
            else Model.add((BaseRegression) GenericModel.clone());
        // If we have too many, remove the last ones
        while (Model.size() > n) {
            Model.remove(Model.size()-1);
        }
    }
    
    @Override
    public void setGenericModel(BaseModel x) {
        if (! (x instanceof AbstractRegressionModel))
            throw new Error("Model must be a regression model");
        GenericModel = (BaseRegression) x.clone();
    }

    @Override
    public BaseRegression getGenericModel() {
        return GenericModel;
    }

    @Override
    public void setModel(int index, BaseModel x) {
        if (! (x instanceof AbstractRegressionModel))
            throw new Error("Model must be a regression model");
        if (NModels() <= index)
            setNumberOfModels(index+1);
        Model.set(index, (BaseRegression) x.clone());
    }
    
    protected void checkStatus() {
        if (NModels() == 0)
            throw new Error("No models present!");
    }

    @Override public CompositeRegression clone() {
        CompositeRegression x = (CompositeRegression) super.clone();
        x.Model = new LinkedList<>();
        for (int i=0; i<NModels(); i++)
            x.Model.add(Model.get(i).clone());
        return x; 
    }    

    @Override protected void train_protected(Dataset TrainData) {
        checkStatus();
        // Train each model individually
        for (int i=0; i<NModels(); i++)
            Model.get(i).train(TrainData);
    }

    @Override
    public int getNFittingParameters() {
        int count = 0;
        for (int i=0; i<NModels(); i++) {
            AbstractRegressionModel Ptr = (AbstractRegressionModel) getModel(i);
            count += Ptr.getNFittingParameters();
        }
        return count;
    }
    
    

    @Override
    public void run_protected(Dataset TestData) {
        checkStatus();
        double[][] full_pred = getEnsemblePredictions(TestData);
        double[] mean_pred = new double[TestData.NEntries()];
        double[] temp = new double[NModels()];
        for (int i=0; i<TestData.NEntries(); i++) {
            for (int j=0; j<NModels(); j++)
                temp[j] = full_pred[j][i];
            mean_pred[i] = StatUtils.mean(temp);
        }
        TestData.setPredictedClasses(mean_pred);
    }
    
    /**
     * Calculate the predicted class using each model
     * @param Data Dataset to be evaluated
     * @return Predicted class from each model for each entry
     */
    public double[][] getEnsemblePredictions(Dataset Data) {
        checkStatus();
        double[][] output = new double[NModels()][];
        for (int i=0; i<NModels(); i++) {
            // Pass the parallelization options downward
            Model.get(i).run(Data);
            output[i] = Data.getPredictedClassArray();
        }
        return output;
    }
    
    /**
     * Get mean prediction by the ensemble for each entry
     * @param EnsemblePredictions array produced using <code>getEnsemblePredictions</code>
     * @return Variance estimate for each entry
     */
    public double[] getEnsembleMean(double[][] EnsemblePredictions) {
        double[] mean_pred = new double[EnsemblePredictions[0].length];
        double[] temp = new double[NModels()];
        for (int i=0; i<mean_pred.length; i++) {
            for (int j=0; j<NModels(); j++)
                temp[j] = EnsemblePredictions[j][i];
            mean_pred[i] = StatUtils.mean(temp);
        }
        return mean_pred;
    }
    
    /**
     * Get mean prediction by the ensemble for each entry
     * @param Double Dataset to be evaluated
     * @return Variance estimate for each entry
     */
    public double[] getEnsembleMean(Dataset Data) {
        double[][] pred = getEnsemblePredictions(Data);
        return getEnsembleMean(pred);
    }
    
    /**
     * Return the variance in predictions for each entry. To deal with small data sizes,
     * this is estimated using the range
     * @param EnsemblePredictions Double array produced using <code>getEnsemblePredictions</code>
     * @return Variance estimate for each entry
     */
    public double[] getEnsembleVariation(double[][] EnsemblePredictions) {
        double[] var_pred = new double[EnsemblePredictions[0].length];
        double[] temp = new double[NModels()];
        for (int i=0; i<var_pred.length; i++) {
            for (int j=0; j<NModels(); j++)
                temp[j] = EnsemblePredictions[j][i];
            var_pred[i] = StatUtils.max(temp) - StatUtils.min(temp);
        }
        return var_pred;
    }
    
    /**
     * Return the variance in predictions for each entry. To deal with small data sizes,
     * this is estimated using the range.
     * @param Data Dataset to evaluate
     * @return Variance estimate for each entry
     */
    public double[] getEnsembleVariation(Dataset Data) {
        double[][] full_pred = getEnsemblePredictions(Data);
        return getEnsembleVariation(full_pred);
    }  

    @Override
    public String printModel_protected() {
        String output = "";
        for (int i=0; i < NModels(); i++) {
            output += "Submodel #" + i + ":\n";
            output += getModel(i).printModel();
        }
        return output;
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        // Generate details of each sub model
        for (int i=0; i<NModels(); i++) {
            String[] submodel = getModel(i).printDescription(htmlFormat).split("\n");
            submodel[0] = "Submodel #" + i + ": " + submodel[0];
            output.addAll(Arrays.asList(submodel));
        }
        
        return output;
    }
    
    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) return super.runCommand(Command);
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "submodel":
                return MultiModelUtility.handleSubmodelCommand(this, Command.subList(1, Command.size()));
            default:
                return super.runCommand(Command);
        }
    }   
}
