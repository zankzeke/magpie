package magpie.user.server;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds information about a model.
 * @author Logan Ward
 */
public class ModelPackage {
    /** Dataset used to generate attributes */
    final protected Dataset Dataset;
    /** Model to be evaluated */
    final protected BaseModel Model;
    /** Name of property being modeled. HTML format suggested */
    public String Property = "Unspecified";
    /** Training set description */
    public String TrainingSet  = "Unspecified";
    /** Author of this model */
    public String Author = "Unspecified";
    /** Citation for this model */
    public String ModelCitation = "Unspecified";
    /** Short description of this model */
    public String Description = "";
    /** Long form description of model */
    public String Notes;
    /**
     * Units for property
     */
    protected String Units = "Unspecified";
    /**
     * How many times this model has been run
     */
    protected AtomicLong NumberRuns = new AtomicLong(0);
    /** Number of entries evaluated */
    protected AtomicLong NumberEvaluated = new AtomicLong(0);
    /** How long this model has been run for, in milliseconds */
    protected AtomicLong RunTime = new AtomicLong(0);
    /**
     * Maximum mumber of entries to run, overrules {@linkplain ServerLauncher#MaxNumEntries}
     */
    protected Integer MaxNumEntries = null;

    /**
     * Initialize model package
     * @param data Dataset used to generate attributes
     * @param model Model to be evaluated
     */
    public ModelPackage(Dataset data, BaseModel model) {
        this.Dataset = data.createTemplate();
        this.Model = model.clone();
    }

    /**
     * @return Number of times model has been run
     */
    public long getNumberRuns() {
        return NumberRuns.get();
    }

    /**
     * @return Number of entries that were evaluated
     */
    public long getNumberEntriesEvaluated() {
        return NumberEvaluated.get();
    }

    /**
     * @return Milliseconds Magpie has spent evalauting this model
     */
    public long getRunTime() {
        return RunTime.get();
    }

    /**
     * Get whether model is a classification model
     *
     * @return
     */
    public boolean isClassifer() {
        return Model instanceof AbstractClassifier;
    }

    /**
     * Get the units for the model (regression), or the possible classes (classification)
     *
     * @return Model units or classes
     */
    public String getUnits() {
        return isClassifer() ? StringUtils.join(getPossibleClasses(), ", ") : Units;
    }

    public void setUnits(String units) {
        Units = units;
    }

    /**
     * Get the names of classes for a classification model
     *
     * @return Possible classes
     */
    public String[] getPossibleClasses() {
        if (!isClassifer()) {
            throw new RuntimeException("Model is not a classifier");
        }
        return ((AbstractClassifier) Model).getClassNames();
    }

    /**
     * Get the maximum number of entries this model is allowed to run.
     *
     * @return Maximum for this model, if defined, or the global maximum
     */
    public int getMaxNumEntries() {
        return MaxNumEntries == null ? ServerLauncher.MaxNumEntries : MaxNumEntries;
    }

    /**
     * Run the model stored in this package.
     *
     * <p>Synchronized because some ML algorithms (e.g., ANNs in Weka) do not handle concurrent execution</p>
     *
     * @param data Dataset to be run, attributes will also be computed
     */
    public synchronized void runModel(Dataset data) throws Exception {
        // Check if dataset is too big
        if (data.NEntries() > getMaxNumEntries()) {
            throw new IllegalArgumentException("Dataset is too large: " + data.NEntries() + " > " + getMaxNumEntries());
        }

        // Run the mode
        long startTime = System.currentTimeMillis();
        data.generateAttributes();
        Model.run(data);

        // Record model running statistics
        RunTime.addAndGet(System.currentTimeMillis() - startTime);
        NumberRuns.incrementAndGet();
        NumberEvaluated.addAndGet(data.NEntries());
    }

    /**
     * Get a copy of the dataset
     *
     * @return Clone of dataset
     */
    public Dataset getDatasetCopy() {
        return Dataset.emptyClone();
    }

    /**
     * Write the dataset out via serialization
     *
     * @param output Output stream
     * @throws IOException
     */
    public void writeDataset(OutputStream output) throws IOException {
        UtilityOperations.saveState(Dataset, output);
    }

    /**
     * Write the model out via serialization
     *
     * @param output Output stream
     * @throws IOException
     */
    public void writeModel(OutputStream output) throws IOException {
        UtilityOperations.saveState(Model, output);
    }


    /**
     * Check whether dataset is a superclass of a certain class.
     * <p>
     * <p>You can use this to check whether the model supports data of a certain type. For example, a
     * {@linkplain magpie.data.materials.CrystalStructureDataset} is a subclass of
     * {@linkplain magpie.data.materials.CompositionDataset} so entries supported the CrystalStructureDataset are
     * also valid entires for a CompositionDataset</p>
     *
     * @param toCheckAgainst Dataset type to check against
     * @return Whether this model is compatible with data stored in that object
     */
    public boolean modelSupports(Dataset toCheckAgainst) {
        return toCheckAgainst.getClass().isInstance(Dataset);
    }

    /**
     * Check whether the dataset type is the same as a provided type.
     *
     * @param toCheckAgainst Dataset type to check against
     * @return Whether the type of dataset are the same
     */
    public boolean datasetMatches(Dataset toCheckAgainst) {
        return Dataset.getClass().getName().equals(toCheckAgainst.getClass().getName());
    }

    /**
     * Render the model information as a JSON file
     * @return JSON object holding various data about the model
     */
    public JSONObject toJSON() {
        JSONObject output = new JSONObject();

        // Add in the data
        output.put("property", Property);
        output.put("modelType", Model instanceof AbstractClassifier ? "classification" : "regression");
        output.put("units", Model instanceof AbstractClassifier ?
                ((AbstractClassifier) Model).getClassNames() :
                Units);
        output.put("trainingSetDescription", TrainingSet);
        output.put("trainingSetSize", Model.TrainingStats.NumberTested);
        output.put("author", Author);
        output.put("citation", ModelCitation);
        output.put("description", Description);
        output.put("notes", Notes);
        output.put("modelTrainedDate", Model.getTrainTime().toString());
        output.put("modelClass", Model.getClass().getCanonicalName());
        output.put("modelDetails", Model.printDescription(true));
        output.put("datasetClass", Dataset.getClass().getCanonicalName());
        output.put("datasetDetails", Dataset.printDescription(true));

        // Add in suggested citations
        JSONObject citations = new JSONObject();
        JSONArray partCitations = new JSONArray();
        for (Pair<String, Citation> toAdd : Model.getCitations()) {
            JSONObject cit = toAdd.getValue().toJSON();
            cit.put("reason", toAdd.getKey());
            partCitations.put(cit);
        }
        citations.put("model", partCitations);
        partCitations = new JSONArray();
        for (Pair<String, Citation> toAdd : Dataset.getCitations()) {
            JSONObject cit = toAdd.getValue().toJSON();
            cit.put("reason", toAdd.getKey());
            partCitations.put(cit);
        }
        citations.put("dataset", partCitations);
        output.put("suggestedCitations", citations);

        // Add in the model statistics
        JSONObject stats = new JSONObject();
        if (Model.isTrained()) {
            stats.put("training", Model.TrainingStats.getStatisticsNoNaNs());
        }
        if (Model.isValidated()) {
            stats.put("validation", Model.ValidationStats.getStatisticsNoNaNs());
            stats.getJSONObject("validation").put("method", Model.getValidationMethod());
        }
        output.put("modelStats", stats);

        // Output usage information
        JSONObject usageData = new JSONObject();
        usageData.put("numberTimesRun", NumberRuns);
        usageData.put("numberEntriesEvaluated", NumberEvaluated);
        usageData.put("totalRunTime", UtilityOperations.millisecondsToString(RunTime.get()));
        usageData.put("totalRunTimeMilliseconds", RunTime);
        output.put("usageStats", usageData);

        return output;
    }
}
