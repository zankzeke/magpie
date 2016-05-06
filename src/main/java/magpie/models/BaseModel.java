package magpie.models;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import magpie.Magpie;
import magpie.analytics.BaseStatistics;
import magpie.attributes.selectors.BaseAttributeSelector;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.utilities.filters.BaseDatasetFilter;
import magpie.data.utilities.normalizers.BaseDatasetNormalizer;
import magpie.models.regression.AbstractRegressionModel;
import magpie.user.CommandHandler;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.*;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Base class for any model, regression or classification.
 * 
 * <p>All models support the ability to filter training data,
 * normalize attributes, and select attributes before model training (in that order).
 * 
 * <p><b>Implementation Guide</b>
 * 
 * <p>Operations that must be implemented:
 * <ul>
 * <li>{@linkplain #clone()} - Fulfills java.lang.Cloneable, ValidationStats and TrainingStats must be new instances.
 * And, you probably want to create a new instance of any submodels</li>
 * <li>{@linkplain  #train_protected(magpie.data.Dataset) } -
 * Trains the model on a training set, does not set TrainingStats</li>
 * <li>{@linkplain #run_protected(magpie.data.Dataset) } - Run a model on a Dataset</li>
 * <li>{@linkplain #printModel_protected() } and {@linkplain #printModelDescriptionDetails(boolean) }
 * - Print detailed and simple descriptions of the model
 * </ul>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>train $&lt;dataset&gt;</b> - Train model using measured class values 
 * <br><pr><i>dataset</i>: Dataset used to train this model</command>
 * 
 * <command><p><b>output = crossvalidate $&lt;dataset> [&lt;folds>]</b> - 
 * Use k-fold cross-validation to assess model performance.
 * <br><pr><i>dataset</i>: Dataset to use for cross validation
 * <br><pr><i>folds</i>: Number of crossvalidation folds (default = 10)
 * <br><pr><i>output</i>: Dataset, result of used to compute performance staistics
 * <br>Splits <i>dataset</i> into <i>folds</i> parts. Trains model on <i>folds</i> - 1 parts, validates against remaining part.
 * Repeats using each part as the validation set.</command>
 * 
 * <command><p><b>run $&lt;dataset></b> - Use model to predict class values for each entry
 * <br><pr><i>dataset</i>: Dataset to evaluate</command>
 * 
 * <command><p><b>validate $&lt;dataset></b> - Validate model against external dataset
 * <br><pr><i>dataset</i> - Dataset to use for validate</command>
 * 
 * <command><p><b>set selector $&lt;selector&gt;</b> - Define the {@link BaseAttributeSelector} used to screen attributes before training
 * <br><pr><i>selector</i>: Attribute selector to use</command>
 * 
 * <command><p><b>set selector $&lt;filter&gt;</b> - Define the {@link BaseDatasetFilter} used to filter data
 * before attribute normalization, attribute selection, and model training.
 * <br><pr><i>filter</i>: Filter to use</command>
 * 
 * <command><p><b>normalize [attributes] [class] &lt;method&gt; [&lt;options...&gt;]</b> 
 *  - Define how to normalize data (data is not normalized by default)
 * <br><pr><i>attributes</i>: Whether to normalize attributes
 * <br><pr><i>class</i>: Whether to normalize class variable
 * <br><pr><i>method</i>: Method used to normalize attributes ("?" for available options)
 * <br><pr><i>options...</i>: Any options for the normalizer</command>
 * 
 * <p><b><u>Implemented Print Commands</u></b>
 * 
 * <print><p><b>description</b> - Print out short description of this model.</print>
 * 
 * <print><p><b>model</b> - Print out the model</print>
 * 
 * <print><p><b>validation [&lt;command>]</b> - Print out statistics generated during validation
 * <br><pr><i>command</i>: Command to be passed to internal {@linkplain BaseStatistics} object.</print>
 * 
 * <print><p><b>training [&lt;command>]</b> - Print out statistics generated during training
 * <br><pr><i>command</i>: Command to be passed to internal {@linkplain BaseStatistics} object.</print>
 * 
 * <print><p><b>selector</b> - Print out attributes used selected by internal {@link BaseAttributeSelector}, if defined</print>
 * 
 * <p><b><u>Implemented Save Commands</u></b>
 * 
 * <save><p><b>training</b> - Print out performance data for training set</save>
 * 
 * <save><p><b>validation</b> - Print out performance data for validation set</save>
 * 
 * @author Logan Ward
 * @version 1.0
 */
abstract public class BaseModel implements java.io.Serializable, java.lang.Cloneable, 
        Options, Printable, Commandable, Savable, Citable {
    /** Records whether model has been trained */
    protected boolean trained=false;
    /** Records whether model has been validated */
    protected boolean validated=false;
    /** Statistics about performance on training set */
    public BaseStatistics TrainingStats; 
    /** Statistics generated during model validation */
    public BaseStatistics ValidationStats;
    /** BaseAttributeSelector used to screen attributes during training */
    protected BaseAttributeSelector AttributeSelector = null;
    /** Used to normalize attributes before training / running model */
    private BaseDatasetNormalizer Normalizer = null;
    /** Filter used to clean data before training model */
    private BaseDatasetFilter Filter = null;
    /** Stores description how this model validated. */
    private String ValidationMethod = "Unvalidated";
    /** Date model was trained */
    private Date TrainTime = null;
    /** Names of attributes used to train model */
    private String[] AttributeNames;
       
    /**
     * @return Whether this model has been trained
     */
    public boolean isTrained() { return trained; }
    
    /**
     * Return when this model was trained
     * @return When starting was started if this model is trained, null otherwise
     */
    public Date getTrainTime() {
        return isTrained() ? TrainTime : null;
    }
    
    /**
     * @return Whether any sort of validation has been run on this model
     */
    public boolean isValidated() { return validated; }
    
    /**
     * Mark this model as untrained and unvalidated
     */
    public void resetModel() { trained=false; validated=false; };    

    /**
     * Return the BaseAttributeSelector used by this model
     * @return Link to the BaseAttributeSelector object
     */
    public BaseAttributeSelector getAttributeSelector() {
        return AttributeSelector;
    }

    /**
     * Define an attribute selector that will force this model to only use a 
     *  subset of the attributes supplied with a Dataset
     * @param AttributeSelector Untrained BaseAttributeSelector
     */
    public void setAttributeSelector(BaseAttributeSelector AttributeSelector) {
        this.AttributeSelector = AttributeSelector;
    }
    
    /**
     * Set filter used to clean data before training
     * @param filter Desired filter
     */
    public void setFilter(BaseDatasetFilter filter) {
        Filter = filter;
    }
    
    /**
     * Get filter used before training
     * @return Link to internal filter object
     */
    public BaseDatasetFilter getFilter() {
        return Filter;
    }
    
    /** 
     * Perform n-fold cross validation
     * @param folds Number of folds in CV test
     * @param cvData Data to use for CV
     * @return Clone of input dataset. Class variable is the predicted 
     * value of the model used when computing CV statistics
     */
    public Dataset crossValidate(int folds, Dataset cvData) {
        // Split into several parts
        Dataset internalTest = cvData.clone();
        Dataset[] testFolds = internalTest.splitIntoFolds(folds);
        
        // Generate a clone of the model to play with
        BaseModel testModel;
        testModel = (BaseModel) this.clone(); 
        
        for(int i=0; i<folds; i++){
            Dataset TrainData = cvData.emptyClone();
            
            // Build a training set that does not inclue the current iteration
            for(int j=0; j<folds; j++) {
                if (i!=j) {
                    TrainData.combine(testFolds[j]);
                }
            }
            
            // Build a model on the training set, evaluate on the remaining data
            testModel.train(TrainData, false);
            testModel.run(testFolds[i]);
        }
        
        // Evaluate stats on the whole thing
        internalTest.combine(testFolds);
        ValidationStats.evaluate(internalTest);
        validated = true;
        
        // Store that this model was cross valided
        if (folds == cvData.NEntries()) {
            ValidationMethod = "Leave-one-out cross-validation using " 
                    + cvData.NEntries() + " entries";
        } else {
            ValidationMethod = String.format("%d-fold cross-validation using %d entries",
                    folds, cvData.NEntries());
        }           
        
        return internalTest;
    }
    
    /** Use external testing data to validate a model (should not contain any data
     * used to train the model)
     * @param testData External test dataset
     */
    public void externallyValidate(Dataset testData) {
        run(testData); ValidationStats.evaluate(testData);
        validated = true;
        ValidationMethod = "External validation using " + testData.NEntries() 
                + " entries";
    }

    /**
     * Get a description of how this model was validated
     * @return Validation technique if model has been validated. "Unvalidated" otherwise.
     */
    public String getValidationMethod() {
        return validated ? ValidationMethod : "Unvalidated";
    }
    
    /**
     * Train a model on a specified training set and then evaluate performance 
     *  on the training set. Results from running model on training set will be
     *  stored as the predicted class for entries in TrainingData.
     * @param TrainingData Dataset used for training
     */
    public void train(Dataset TrainingData) {
        train(TrainingData, true);
    }
    
    /** 
     * Train a model on a specified training set and then evaluate performance 
     *  on the training set, if desired
     * @param data Dataset to use for training
     * @param recordStats Whether to record training statistics
     */
    public void train(Dataset data, boolean recordStats) {
        // Store information about this model
        TrainTime = new Date();
        AttributeNames = data.getAttributeNames();
        
        // Gather only the entries that have measured classes
		Dataset trainingData = data.getTrainingExamples();
        if (trainingData.NEntries() == 0)
            throw new RuntimeException("Data does not contain any training entries");
        
        // Filter dataset
        List<BaseEntry> beforeFilter = null;
        if (Filter != null) {
            beforeFilter = new ArrayList<>(trainingData.getEntries());
            Filter.train(trainingData);
            Filter.filter(trainingData);
        }
        
        // Perform normalization, if desired
        if (Normalizer != null) {
            Normalizer.train(data);
            Normalizer.normalize(trainingData);
        }
        
        // Perform attribute selection, if desired
        List<double[]> attributes = new ArrayList<>(trainingData.NEntries());
        String[] attributeNames = null;
        if (AttributeSelector != null) {
            // Store original names
            for (BaseEntry entry : trainingData.getEntries()) {
                attributes.add(entry.getAttributes());
            }
            attributeNames = data.getAttributeNames();
            // Run the 
            AttributeSelector.train(trainingData);
            AttributeSelector.run(trainingData);
        }
        
        // Train the model
        if (this instanceof AbstractRegressionModel) {
            // For regression models only: Perform robust training. 
            AbstractRegressionModel Ptr = (AbstractRegressionModel) this;
            Ptr.robustTraining(trainingData);
        } else {
            train_protected(trainingData);
        }
        trained=true;
        
        // Restore attributes
        if (AttributeSelector != null) {
            trainingData.setAttributeNames(Arrays.asList(attributeNames));
            for (int i=0; i<attributes.size(); i++) {
                trainingData.getEntry(i).setAttributes(attributes.get(i));
            }
        }
        
        // De-normalize data
        if (Normalizer != null) {
            Normalizer.restore(trainingData);
        }
        
        // Return filtered entries
        if (Filter != null) {
            trainingData.clearData();
            trainingData.addEntries(beforeFilter);
        }
        
        if (recordStats) {
            run(trainingData);
            TrainingStats.evaluate(trainingData);
        }
    }
    
    /**
     * Run a model on provided data. Results will be stored as the predicted
     *  class variable.
	 * 
     * @param runData Dataset to evaluate. 
     */
    public void run(Dataset runData) {
        // Check that the model has been trained
        if (!isTrained())
            throw new RuntimeException("Model not yet trained");
        
        // Check that the attributes are the same
        if (! Arrays.equals(AttributeNames, runData.getAttributeNames())) {
            throw new RuntimeException("Attribute names are different.");
        }
        
        // Test if run will be parallel
        if (Magpie.NThreads > 1 && runData.NEntries() > Magpie.NThreads) {
            // Original thread count
            int originalNThreads = Magpie.NThreads;
            
            // Make sure any children of this model don't launch competing threads
            Magpie.NThreads = 1; 
            
            // Split data for threads
            Dataset[] threadData = runData.splitForThreading(originalNThreads);
            
            // Launch threads
            ExecutorService service = Executors.newFixedThreadPool(originalNThreads);
            for (int i=0; i<originalNThreads; i++) {
                final Dataset part = threadData[i];
                final BaseModel model = i == 0 ? this : clone();
                Runnable thread = new Runnable() {
                    @Override
                    public void run() {
                        model.run(part);
                    }
                };
                service.submit(thread);
            }
            
            // Wait until everything is done
            service.shutdown();
            try {
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException i) {
                throw new Error(i);
            }
            
            // Restore parallelism
            Magpie.NThreads = originalNThreads;
        } else {
            // Perform normalization, if needed
            if (Normalizer != null) {
                Normalizer.normalize(runData);
            }

            // Perform any attribute filtering 
            Dataset data = runData;
            if (AttributeSelector != null) {
                data = runData.clone();
                AttributeSelector.run(data);
            }
            
            // Run it serially 
            run_protected(data);
            
            // Copy results to original array, if attribute selection was used
            if (AttributeSelector != null) {
                if (data.NClasses() == 1)
                    runData.setPredictedClasses(data.getPredictedClassArray());
                else 
                    runData.setClassProbabilities(data.getClassProbabilityArray());
            }

            // Restore data to original ranges
            if (Normalizer != null) {
                Normalizer.restore(runData);
            }
        }
    }
    
    /** Save the state of this object using serialization
     * @param filename Filename for output
     */
    public void saveState(String filename) {
        UtilityOperations.saveState(this, filename);
    }
    
    /** Read the state from file using serialization
     * @param filename Filename for input
     * @return Model stored in that file
     * @throws java.lang.Exception 
     */
    public static BaseModel loadState(String filename) throws Exception {
        return (BaseModel) UtilityOperations.loadState(filename);
    }
    
    @Override 
    public BaseModel clone() {
        BaseModel x;
        try { 
            x = (BaseModel) super.clone(); 
            x.trained = this.trained;
            x.validated = this.validated;
            x.TrainingStats = (BaseStatistics) this.TrainingStats.clone();
            x.ValidationStats = (BaseStatistics) this.ValidationStats.clone();
            if (AttributeSelector != null)
                x.AttributeSelector = this.AttributeSelector.clone();
            if (Normalizer != null) {
                x.Normalizer = Normalizer.clone();
            }
        } catch (Exception e) { throw new Error(e); }
        return x;
    } 
    
    /** 
     * Train a model without evaluating performance 
     * @param TrainData Training data
     */
    abstract protected void train_protected(Dataset TrainData);
    
    /** 
     * Run a model without checking if stuff is trained (use carefully)
     * @param TrainData Training data
     */
    abstract public void run_protected(Dataset TrainData);

    @Override
    public String about() {
        String Output = "Trained: " + trained + " - Validated: " + validated;
        return Output;
    }
    
    /**
     * If a model is trained, return it formatted as a string.
     * @return Model formatted as string
     */
    public String printModel() {
        if (! isTrained()) return "Model not yet trained";
        return printModel_protected();
    }
    
    /**
     * Internal method that handles printing the model as a string. Should end with a "\n"
     * @return String representation of model
     */
    abstract protected String printModel_protected();
    
    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + "\n";
        
        // Add HTML indentation
        if (htmlFormat) {
            output += "<div style=\"margin-left: 25px\">\n";
        }
        
        // Get model details
        List<String> details = printModelDescriptionDetails(htmlFormat);
        boolean started = false;
        String lastLine = "";
        for (String line : details) {
            output += "\t";
            
            // Add <br> where appropriate
            if (started && // Not for the first line int the block
                    htmlFormat // Only for HTML-formatted output
                    // Not on lines for the "<div>" tags
                    && ! (line.contains("<div") || line.contains("</div>")) 
                    // Not immediately after <div> tags
                    && ! (lastLine.contains("<div") || lastLine.contains("</div>")) 
                    // Not if the line already has a break
                    && ! line.contains("<br>")) {
                output += "<br>";
            }
            
            // Add line to ouput
            output += line + "\n";
            
            // Update loop variables
            started = true;
            lastLine = line;
        }
        
        // Deindent
        if (htmlFormat) {
            output += "</div>\n";
        }
        return output;
    }
    
    /**
     * Print details of the model. Used by {@link #printDescription(boolean)}.
     * 
     * <p>Implementation note: No not add indentation for details. That is handled
     * by {@link #printDescription(boolean)}. You should also call the super 
     * operation to get the Normalizer and Attribute selector settings
     * 
     * <p
     * @param htmlFormat Whether to use HTML format
     * @return List describing model details. Each entry is a different line of the 
     * description (i.e., in place of newline characters).
     */
    protected List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = new LinkedList<>();
        if (Filter != null) {
            output.add("Filter: " + Filter.getClass().getName());
        }
        if (Normalizer != null) {
            output.add("Normalizer: " + Normalizer.getClass().getName());
        }
        if (AttributeSelector != null) {
            output.add("Attribute Selector: " + AttributeSelector.getClass().getName());
        }
        return output;
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return about();
        List<String> SubCommand; // Command to be passed to calls
        SubCommand = Command.subList(1, Command.size());
        switch (Command.get(0).toLowerCase()) {
            case "description":
                return printDescription(false);
            case "model":
                return printModel();
            case "validation":
                if (SubCommand.isEmpty()) return ValidationStats.about();
                if (validated) 
                    return ValidationStats.printCommand(SubCommand);
                else return "ERROR: Not yet validated.";
            case "training":
                if (SubCommand.isEmpty()) return TrainingStats.about();
                if (trained) return TrainingStats.printCommand(SubCommand);
                else return "ERROR: Not yet trained.";                
            case "selector":
                if (AttributeSelector == null)
                    return "No attribute selector used";
                else return AttributeSelector.printSelections();
            default:
                throw new Exception("ERROR: Print command \"" + Command.get(0).toLowerCase()
                        + "\" not recognized");
        }
    }    

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println(about());
            return null;
        }
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "filter": {
                // Usage: filter [exclude|include] <filter name> <filter options...>
                boolean exclude;
                String filterName;
                List<Object> filterOptions;
                try {
                    // Determine whether filter is inclusive or exclusive
                    if (Command.get(1).toString().equalsIgnoreCase("include")) {
                        exclude = false;
                    } else if (Command.get(1).toString().equalsIgnoreCase("exclude")) {
                        exclude = true;
                    } else {
                        throw new Exception();
                    }
                    
                    // Get filter name / options
                    filterName = Command.get(2).toString();
                    filterOptions = Command.subList(3, Command.size());
                    
                } catch (Exception e) {
                    throw new Exception("Usage: filter [exclude|include] <filter name> <options...>");
                }
                
                // Create filter
                BaseDatasetFilter filter = (BaseDatasetFilter) CommandHandler.instantiateClass(
                        "data.utilities.filters." + filterName, filterOptions);
                
                // Set whether to exclude | include
                filter.setExclude(exclude);
                
                // Set filter
                setFilter(filter);
                
                // Print status data
                System.out.format("Set model to %s entries based on a %s before training",
                        exclude ? "exclude" : "include",
                        filterName);
                return null;
            }
            case "train": {
                // Usage: train ${dataset}
                if (Command.size() < 2 || ! (Command.get(1) instanceof Dataset)) {
                    throw new Exception("Usage: train ${dataset}");
                }
                Dataset Data = (Dataset) Command.get(1);
                train(Data);
            } break;
            case "crossvalidate": case "cv": case "cross": {
                // Usage: crossvalidate $<dataset> [<folds = 10>]
                Dataset Data;
                int folds;
                try {
                    Data = (Dataset) Command.get(1);
                    folds = 10;
                    if (Command.size() > 2) {
                        folds = Integer.valueOf(Command.get(2).toString());
                    }
                } catch (Exception e) {
                    throw new Exception("Usage: crossvalidate $<dataset> [<folds = 10>]");
                }
                return crossValidate(folds, Data);
            }
            case "normalize": {
                boolean doAttributes = false, doClass = false;
                String Method; List<Object> Options;
                try {
                    int pos = 1;
                    while (true && pos < 3) {
                        String word = Command.get(pos).toString().toLowerCase();
                        if (word.equals("attributes")) {
                            doAttributes = true;
                        } else if (word.equals("class")) {
                            doClass = true;
                        } else {
                            break;
                        }
                        pos++;
                    }
                    Method = Command.get(pos).toString();
                    if (Method.equals("?")) {
                        System.out.println("Available Normalizers:");
                        System.out.println(CommandHandler.printImplmentingClasses(BaseDatasetNormalizer.class, false));
                        return null;
                    }
                    Method = "data.utilities.normalizers." + Method;
                    Options = Command.subList(pos + 1, Command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: normalize [attributes] [class] <method> [<options...>]");
                }
                Normalizer = (BaseDatasetNormalizer) 
                        CommandHandler.instantiateClass(Method, Options);
                Normalizer.setToNormalizeAttributes(doAttributes);
                Normalizer.setToNormalizeClass(doClass);
            } break;
            case "set":
                handleSetCommand(Command); break;
            case "run": {
                // Usage: run $<dataset>
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: run $<dataset>");
                }
                run(Data);
            } break;
            case "validate": {
                // Usage: validate $<dataset>
                Dataset Data;
                try { Data = (Dataset) Command.get(1); }
                catch (Exception e) { throw new Exception("Usage: validate $<dataset>"); }
                externallyValidate(Data);
            } break;
            default:
                throw new Exception("ERROR: Model command not recognized: " + Action);
        }
        return null;
    }
    
    /**
     * Handle setting components of a model via the command interface
     * @param Command Command to be executed.
     * @throws Exception
     */
    public void handleSetCommand(List<Object> Command) throws Exception {
        if (Command.size() != 3) {
            throw new Exception("Usage: set <component> $<object>");
        }
        String Cmp = Command.get(1).toString();
        setComponent(Cmp, Command.get(2));
    }
    
    /**
     * Set a specific component of a model
     * @param Name Name of component
     * @param Object Instance of component (will be cloned)
     * @throws Exception
     */
    public void setComponent(String Name, Object Object) throws Exception {
        switch (Name.toLowerCase()) {
            case "selector":
                setAttributeSelector((BaseAttributeSelector) Object); break;
            default:
                throw new Exception("ERROR: Model does not contain a " + Name);
        }
    }

    @Override
    public String saveCommand(String Basename, String Format) throws Exception {
        switch (Format.toLowerCase()) {
            case "training":
                return TrainingStats.saveCommand(Basename, "data");
            case "validation":
                return ValidationStats.saveCommand(Basename, "data");
            default:
                throw new Exception("Format not recognized: " + Format);
        }
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        // Initialize output
        List<Pair<String, Citation>> output = new ArrayList<>();
        
        // Add attributes dealing with attribute selector
        if (AttributeSelector instanceof Citable) {
            Citable intf = (Citable) AttributeSelector;
            output.addAll(intf.getCitations());
        }
        
        // Add attributes dealing with normalizer
        if (Normalizer instanceof Citable) {
            Citable intf = (Citable) Normalizer;
            output.addAll(intf.getCitations());
        }
        
        return output;
    }
}
