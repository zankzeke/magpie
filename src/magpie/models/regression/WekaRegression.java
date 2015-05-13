package magpie.models.regression;

import java.util.List;
import magpie.analytics.RegressionStatistics;
import magpie.data.Dataset;
import magpie.models.interfaces.WekaModel;
import magpie.user.CommandHandler;
import magpie.utility.WekaUtility;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;

/**
 * Implementation of {@link WekaModel} for regression purposes. 
 * 
 * <usage><p><b>Usage</b>: &lt;Weka classifier> [&lt;classifier ptions...>]
 * <br><pr><i>Weka classifier</i>: Name of a Weka classifier model (i.e. trees.REPTree). "?" to list options
 * <br><pr><i>classifier options</i>: Any options for that model</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class WekaRegression extends BaseRegression implements WekaModel {
   /** Link to Weka-based model */
    public AbstractClassifier Model;
    /** Whether model type has been defined */
    public boolean model_defined=false;
    /** Name of model type currently in use */
    protected String Model_Type;
    /** Options supplied when instantiating Model */
    protected String[] Model_Options;
    
    /** Create a Weka model with a specified model and options
     * 
     * @param model_type Model type (ie trees.J48)
     * @param options Options for the model
     * @throws java.lang.Exception
     */
    public WekaRegression(String model_type, String[] options) throws Exception {
        setModel(model_type, options);
        ValidationStats = new RegressionStatistics();
        TrainingStats = new RegressionStatistics();
    }
    /**
     * Create a WekaRegression model based on the "rules.ZeroR" algorithm
     * @throws java.lang.Exception
     */
    public WekaRegression() throws Exception {
        setModel("rules.ZeroR", null);
    };
    
    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            if (Options.length == 0) return; // Nothing to set (stay with ZeroR)
            else {
                String ModelName = Options[0];
                String[] ModelOptions = null;
                if (Options.length > 1) {
                    ModelOptions = new String[Options.length - 1];
                    System.arraycopy(Options, 1, ModelOptions, 0, ModelOptions.length);
                }
                setModel(ModelName, ModelOptions);
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <Weka Classifier Name> [<Model Options...>]";
    }
    
    @Override 
    public WekaRegression clone() {
        WekaRegression output = (WekaRegression) super.clone();
        try { output.Model = (AbstractClassifier) AbstractClassifier.makeCopy(Model); }
        catch (Exception e) { throw new Error("Cloning model failed due to: "+e); }
        output.model_defined = model_defined;
        return output;
    }
            
    
    /** Set the underlying Weka-based model
     * 
     * @param model_type Model type (ie trees.J48)
     * @param options Options for the model
     */
    @Override 
    public final void setModel(String model_type, String[] options) throws Exception {
        Model = WekaUtility.instantiateWekaModel(model_type, options);
        model_defined=true; 
        Model_Type = model_type; 
        if (options != null )
            Model_Options = Model.getOptions();
    }
    
    /** Return the model name */
    @Override public String getModelName() { return Model_Type; }
    /** Return the model options */
    @Override public String[] getModelOptions() { return Model_Options; }
    /** Return model name and options */
    @Override public String getModelFull() {
        String out = Model_Type;
        for (String Model_Option : Model_Options) {
            out += " " + Model_Option;
        }
        return out;
    }
    
    @Override public String toString() { return Model.toString(); }

    @Override protected void train_protected(Dataset TrainingData) {
        try { 
            Instances wekadata = TrainingData.transferToWeka(true, false);
            Model.buildClassifier(wekadata); 
            TrainingData.restoreAttributes(wekadata);
        }
        catch (Exception e) { 
            throw new Error("Model failed to train." + e); 
        }
    }
           
    @Override 
    public void run_protected(Dataset TestData) {
        try { 
            Instances wekadata = TestData.transferToWeka(true, false);
            double[] prediction = new double [TestData.NEntries()];
            for (int i=0; i<wekadata.numInstances(); i++) 
                prediction[i]=Model.classifyInstance(wekadata.instance(i));
            TestData.setPredictedClasses(prediction);
            TestData.restoreAttributes(wekadata);
        } catch (Exception e) { 
            throw new Error(e); 
        }
    }

    @Override
    public int getNFittingParameters() {
        return 0; // I don't know if Weka has the capability to check this
    }

    @Override
    protected String printModel_protected() {
        return this.Model.toString();
    }    
}
