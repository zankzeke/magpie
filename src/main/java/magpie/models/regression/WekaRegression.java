package magpie.models.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import magpie.statistics.performance.RegressionStatistics;
import magpie.data.Dataset;
import magpie.models.interfaces.WekaModel;
import magpie.user.CommandHandler;
import magpie.utility.WekaUtility;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import weka.classifiers.AbstractClassifier;
import weka.core.Instances;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;

/**
 * Implementation of {@link WekaModel} for regression purposes. To use this class, simply provide 
 * the name of the Weka algorithm and a list of options as input. 
 * 
 * <p>Example: model = new models.classification.WekaClassifier trees.REPTree -L 5
 * 
 * <usage><p><b>Usage</b>: &lt;Weka classifier> [&lt;classifier options...>]
 * <br><pr><i>Weka classifier</i>: Name of a Weka classifier model (i.e. trees.REPTree). "?" to list options
 * <br><pr><i>classifier options</i>: Any options for that model. (see 
 * <a href="http://weka.sourceforge.net/doc.dev/">Weka Javadoc</a> for these options)</usage> 
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
        String modelName;
        String[] modelOptions;
        try {
            if (Options.length == 0) return; // Nothing to set (stay with ZeroR)
            else {
                modelName = Options[0];
                modelOptions = null;
                if (Options.length > 1) {
                    modelOptions = new String[Options.length - 1];
                    System.arraycopy(Options, 1, modelOptions, 0, modelOptions.length);
                }
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }

        // Set the model
        setModel(modelName, modelOptions);
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

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        output.add("Model name:    " + Model_Type);
        
        String options = "";
        for (String option : Model.getOptions()) {
            options += " " + option;
        }
        
        output.add("Model options:" + options);
        
        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        // Initialize output
        List<Pair<String, Citation>> output = super.getCitations(); 
        
        // Get citations from Weka Model
        List<Pair<String, Citation>> wekaCitations = WekaUtility.getWekaObjectCitations(Model, getClass());
        
        // Add in citations from Weka model
        output.addAll(wekaCitations);
        
        return output;
    }    
}
