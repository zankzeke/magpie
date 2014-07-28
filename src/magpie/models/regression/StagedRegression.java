/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.regression;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.interfaces.MultiModel;
import magpie.models.utility.MultiModelUtility;

/**
 * Composite regression where the error signal from a model is used to train the next.
 * 
 * <usage><p><b>Usage</b>: &lt;absolute|relative&gt;
 * <br><pr><i>absolute|relative</i>: Whether to train model on absolute or relative error
 * </usage>
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
 * <command><p><b>submodel set &lt;number> $&lt;model></b> - Set a specific submodel
 * <br><pr><i>number</i>: Index of the submodel to set (list starts with 0)
 * <br><pr><i>model</i>: An instance of {@linkplain BaseModel} to use for that model</command>
 *
 * <command><p><b>&lt;output> = submodel get generic</b> - Retrieve the template for any unassigned submodels</command>
 *
 * <command><p><b>&lt;output> = submodel get &lt;number></b> - Retrieve a specific submodel
 * <br><pr><i>number</i>: Index of submodel to retrieve (list starts with 0)
 * Returns a clone of the model - you cannot use this to edit the model.</command>
 * 
 * <p><b><u>Implemented Print Commands</u></b>
 * 
 * <print><p><b>submodel</b> - Print out number of submodels</print>
 * 
 * <print><p><b>submodel &lt;number> [&lt;command...>]</b> - Pass a print command to one of the submodels
 * <br><pr><i>number</i>: Index of model to operate on (starts at 0)
 * <br><pr><i>command</i>: Print command that gets passed to that submodel</print>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class StagedRegression extends BaseRegression implements MultiModel, AbstractRegressionModel {
    /** Collections of regression models */
    private List<BaseModel> Model;
    /** Generic composite regression model */
    private BaseRegression GenericModel;
    /** Whether absolute or relative error is passed */
    private boolean PropogateAbsolute = true;

    public StagedRegression() {
        Model = new LinkedList<>();
    }

    @Override
    public void setOptions(List Options) throws Exception {
        try {
            String word = Options.get(0).toString().toLowerCase();
            if (word.startsWith("abs")) {
                useAbsoluteError();
            } else if (word.startsWith("rel")) {
                useRelativeError();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }
    
    /**
     * Set to train subsequent models on absolute error from previous.
     */
    public void useAbsoluteError() {
        PropogateAbsolute = true;
    }
    
    /**
     * Set model to train subsequent models on relative error from previous.
     */
    public void useRelativeError() {
        PropogateAbsolute = false;
    }

    @Override
    public String printUsage() {
        return "Usage: <absolute|relative>";
    }

    @Override
    public BaseModel getModel(int index) {
        return Model.get(index);
    }

    @Override
    public int NModels() {
        return Model.size();
    }
    
    @Override
    public void setNumberOfModels(int n) {
        // If we have too few, add generic models (or null)
        while (Model.size() < n)
            if (GenericModel == null) Model.add(null);
            else Model.add(GenericModel.clone());
        // If we have too many, remove the last ones
        while (Model.size() > n) {
            Model.remove(Model.size()-1);
        }
    }
    
    @Override
    public void setGenericModel(BaseModel x) {
        if (! (x instanceof AbstractRegressionModel))
            throw new Error("Model must be a regression model.");
        GenericModel = (BaseRegression) x.clone();
    }
    
    @Override
    public BaseRegression getGenericModel() {
        return GenericModel;
    }

    @Override
    public void setModel(int index, BaseModel x) {
        if (! (x instanceof AbstractRegressionModel))
            throw new Error("Model must be a regression model.");
        if (NModels() <= index)
            setNumberOfModels(index + 1);
        Model.set(index, x.clone());
    }
    
    /**
     * Verify that this model is ready to be trained
     */
    protected void checkStatus() {
        if (NModels() == 0)
            throw new Error("No models present!");
        for (int i=0; i<NModels(); i++)
            if (Model.get(i) == null) 
                throw new Error("Model " + i + " not yet defined.");
    }

    @Override public StagedRegression clone() {
        StagedRegression x = (StagedRegression) super.clone();
        x.Model = new LinkedList<>();
        for (int i=0; i<NModels(); i++)
            x.Model.add(Model.get(i).clone());
        return x; 
    }    

    @Override protected void train_protected(Dataset TrainData) {
        checkStatus();
        Dataset Clone = TrainData.clone();
        double[] measuredClass = TrainData.getMeasuredClassArray();
        double[] errorSignal = TrainData.getMeasuredClassArray();
        double[] prediction;
        // Train subsequent models on the error signal from the previous
        for (int i=0; i<NModels(); i++) {
            Clone.setMeasuredClasses(errorSignal);
            Model.get(i).train(Clone);
            prediction = Clone.getPredictedClassArray();
            if (i == NModels() - 1) break; // Shortcut for last step
            for (int j=0; j < measuredClass.length; j++) {
                errorSignal[j] -= prediction[j];
                if (! PropogateAbsolute) {
                    errorSignal[j] /= measuredClass[j];
                }
            }
                
        }
        Clone.setMeasuredClasses(measuredClass);
    }

    @Override
    public void run_protected(Dataset TestData) {
        double[] prediction;
        if (PropogateAbsolute) {
            // Mode #1: Absolute error
            prediction = new double[TestData.NEntries()];
            for (int i=0; i<NModels(); i++) {
                getModel(i).run(TestData);
                double[] submodelPrediction = TestData.getPredictedClassArray();
                for (int j=0; j<TestData.NEntries(); j++) {
                    prediction[j] *= submodelPrediction[j] + 1;
                }
            }
        } else {
            // Mode #2: Relative error
            // Get last predicted relative error and work backwards:
            //  N-1 model was off by factors given by model N
            prediction = new double[TestData.NEntries()];
            Arrays.fill(prediction, 1.0);
            for (int j=NModels()-1; j>=0; j--) {
                getModel(j).run(TestData);
                double[] submodelPrediction = TestData.getPredictedClassArray();
                for (int e=0; e<TestData.NEntries(); e++) {
                    prediction[e] *= submodelPrediction[e] + 1;
                }
            }
        }
        TestData.setPredictedClasses(prediction);
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
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return super.printCommand(Command);
        // Handle extra commands for split models
        switch (Command.get(0).toLowerCase()) {
            case "submodel":
                return MultiModelUtility.handleSubmodelPrintCommand(this, Command);
            default:
                return super.printCommand(Command); 
        }
    }    

    @Override
    protected String printModel_protected() {
        String output = "";
        for (int i=0; i < NModels(); i++) {
            output += "Stage #" + i + ":\n";
            output += getModel(i).printModel();
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
