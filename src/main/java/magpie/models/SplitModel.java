package magpie.models;

import magpie.data.Dataset;
import magpie.data.utilities.splitters.BaseDatasetSplitter;
import magpie.models.interfaces.MultiModel;
import magpie.models.utility.MultiModelUtility;
import magpie.user.CommandHandler;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Abstract class for a model that splits the dataset and trains
 * several submodels. It contains functions necessary to do this partitioning and keep track
 * of the submodels.
 * 
 * <p><b><u>How to Use a SplitModel</u></b>
 * 
 * <p>SplitModels work by first partitioning a Dataset using a {@linkplain BaseDatasetSplitter}
 * and then training several models 
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>splitter &lt;method> [&lt;options...>]</b> - Define splitter used to partition dataset between models
 * <br><pr><i>method</i>: Method used to split data. Name of a {@linkplain BaseDatasetSplitter} ("?" for options)
 * <br><pr><i>options</i>: Any options for the splitter</command>
 * 
 * <command><p><b>submodel</b> - Print the number of submodels</command>
 *
 * <command><p><b>submodel set generic $&lt;model></b> - Define a model template to use for all submodels
 * <br><pr><i>model</i>: An instance of {@linkplain BaseModel}.
 * Note: Do not use this command for {@linkplain magpie.models.regression.CompositeRegression} unless each
 * model automatically uses a different random number seed. Otherwise, each
 * submodel will be identical.</command>
 *
 * <command><p><b>submodel set &lt;number> $&lt;model></b> - Set a specific submodel
 * <br><pr><i>number</i>: Index of the submodel to set (list starts with 0)
 * <br><pr><i>model</i>: An instance of {@linkplain BaseModel} to use for that model</command>
 *
 * <command><p><b>submodel get generic = &lt;output></b> - Retrieve the template for any unassigned submodels</command>
 *
 * <command><p><b>submodel get &lt;number> = &lt;output></b> - Retrieve a specific submodel
 * <br><pr><i>number</i>: Index of submodel to retrieve (list starts with 0)
 * Returns a clone of the model - you cannot use this to edit the model.</command>
 * 
 * <p><b><u>Implemented Print Commands:</u></b>
 * 
 * <print><p><b>splitter</b> - Print out the name of splitter used by this model</print>
 * 
 * <print><p><b>submodel</b> - Print out number of submodels</print>
 * 
 * <print><p><b>submodel &lt;number> [&lt;command...>]</b> - Pass a print command to one of the submodels
 * <br><pr><i>number</i>: Index of model to operate on (starts at 0)
 * <br><pr><i>command</i>: Print command that gets passed to that submodel</print>
 * 
 * @author Logan Ward
 * @version 1.0
 */
abstract public class SplitModel extends BaseModel implements MultiModel {
    /**
     * List of of models used by this model
     */
    protected ArrayList<BaseModel> Model = new ArrayList<>(2);
    /**
     * Class used to partition data into similar groups
     */
    protected BaseDatasetSplitter Partitioner = null;
    /**
     * Model used to if a model template for a certain split is not defined
     */
    protected BaseModel GenericModel = null;

    @Override public SplitModel clone() {
        SplitModel x;
        x = (SplitModel) super.clone();
        x.Partitioner = Partitioner.clone();
        x.Model = new ArrayList<>(NModels());
        for (int i=0; i<NModels(); i++) {
            x.Model.add(Model.get(i) != null ? Model.get(i).clone() : null);
        }
        return x;
    }

    @Override
    public void setOptions(List Options) throws Exception {
        /** Nothing to set */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options to set*";
    }

    @Override
    public BaseModel getModel(int index) {
        return Model.get(index);
    }
    
    @Override
    public void setNumberOfModels(int n) {
        Model.ensureCapacity(n);
        // If we have too few, add generic models (or null)
        while (Model.size() < n)
            if (GenericModel == null) Model.add(null);
            else Model.add(GenericModel.clone());
        // If we have too many, remove the last ones
        while (Model.size() > n ) {
            Model.remove(Model.size()-1);
        }
    }
    
    /** 
     * Returns the number of model slots currently available 
     */
    @Override
    public int NModels() { return Model.size(); }

    @Override
    public BaseModel getGenericModel() {
        return GenericModel;
    }

    /**
     * Set the model template
     * @param x Template model (will be cloned)
     */
    @Override
    public void setGenericModel(BaseModel x) {
        GenericModel = x.clone();
    }
    
    @Override
    public void setModel(int index, BaseModel x) {
        resetModel();
        if (NModels() <= index)
            setNumberOfModels(index+1);
        this.Model.set(index, x);
    }
        
    /** Set the partitioner.
     * @param S Dataset splitter
     */
    public void setPartitioner(BaseDatasetSplitter S) {
        resetModel();
        this.Partitioner = S;
    }
    
    /** 
     * Checks if enough models are defined. Throw error otherwise
     * @param n Number of models required
     */
    protected void checkModelCount(int n) {
        if (NModels() < n)
            throw new Error("Insufficent number of models. Need: "
                        +n+" - Available: "+NModels());
        for (int i=0; i<n; i++) {
            if (Model.get(i) == null) {
                if (GenericModel == null) {
                    throw new Error("Model " + i + " not defined.");
                } else {
                    Model.set(i, GenericModel.clone());
                }
            }
        }
    }
    
    @Override 
    protected void train_protected(Dataset TrainingData) {
        // Get the desired labels
        Partitioner.train(TrainingData);
        int[] label = Partitioner.label(TrainingData);
        
        // Determine the number of splits
        int nSplits = NumberUtils.max(label) + 1;
        
        // Create subsets
        List<Dataset> splitData = new ArrayList<>(nSplits);
        for (int s=0; s<nSplits; s++) {
            splitData.add(TrainingData.emptyClone());
        }
        for (int e=0; e<label.length; e++) {
            splitData.get(label[e]).addEntry(TrainingData.getEntry(e));
        }
        
        // Train the submodels
        setNumberOfModels(splitData.size());
        checkModelCount(splitData.size());
        for (int i=0; i<splitData.size(); i++) {
            if (splitData.get(i).NEntries() == 0)
                System.err.println("WARNING: No entries provided to train submodel #" + i);
            else
                Model.get(i).train(splitData.get(i), false);
        }
        trained=true; 
    }
    
    @Override public void run_protected(Dataset Data) {
		// Get the desired labels
        int[] label = Partitioner.label(Data);
        
        // Determine the number of splits
        int nSplits = NumberUtils.max(label) + 1;
        
        // Create subsets
        List<Dataset> splitData = new ArrayList<>(nSplits);
        for (int s=0; s<nSplits; s++) {
            splitData.add(Data.emptyClone());
        }
        for (int e=0; e<label.length; e++) {
            splitData.get(label[e]).addEntry(Data.getEntry(e));
        }
		
		// Run the models
        checkModelCount(splitData.size());
        for (int i = 0; i < splitData.size(); i++) {
            if (splitData.get(i).NEntries() > 0) {
                if (Model.get(i).isTrained()) {
                    Model.get(i).run(splitData.get(i));
                } else {
                    throw new Error("ERROR: Submodel #" + i + "has not yet been trained");
                }
            }
        }
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return super.printCommand(Command);
        // Handle extra commands for split models
        switch (Command.get(0).toLowerCase()) {
            case "submodel": {
                return MultiModelUtility.handleSubmodelPrintCommand(this, Command);
			}
            case "splitter":
                return "Splitter type: " + Partitioner.getClass().getSimpleName();
            default:
                return super.printCommand(Command); 
        }
    }

    @Override
    protected String printModel_protected() {
        String output = "";
        List<String> splitNames = Partitioner.getSplitNames();
        for (int i=0; i < NModels(); i++) {
            output += splitNames.get(i) + ":\n";
            output += getModel(i).printModel();
        }
        return output;
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        // Print out partitioner and its details
        String[] partr = Partitioner.printDescription(htmlFormat).split("\n");
        partr[0] = "Partitioner: " + partr[0];
        output.addAll(Arrays.asList(partr));
        
        // Print out submodel details
        List<String> splitNames = Partitioner.getSplitNames();
        for (int i=0; i<NModels(); i++) {
            String[] submodel = getModel(i).printDescription(htmlFormat).split("\n");
            submodel[0] = splitNames.get(i) + ": " + submodel[0];
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
			case "splitter": {
				String Method; List<Object> MethodOptions;
				try {
					Method = Command.get(1).toString();
					MethodOptions = Command.subList(2, Command.size());
				} catch (Exception e) {
					throw new IllegalArgumentException("splitter <method> <options...>");
				}
				BaseDatasetSplitter splitter = (BaseDatasetSplitter)
						CommandHandler.instantiateClass("data.utilities.splitters." + Method, MethodOptions);
                setPartitioner(splitter);
				return null;
			} 
            default:
                return super.runCommand(Command);
        }
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        Set<Pair<String, Citation>> allCitations = new HashSet<>();

        // Collect all reasons for all citations
        for (BaseModel model : Model) {
            allCitations.addAll(model.getCitations());
        }

        // Combine them into the output
        List<Pair<String, Citation>> output = new ArrayList<>(super.getCitations());
        output.addAll(allCitations);

        // Add in citations for clusterer
        if (Partitioner instanceof Citable) {
            output.addAll(((Citable) Partitioner).getCitations());
        }

        return new ArrayList<>(output);
    }
}
