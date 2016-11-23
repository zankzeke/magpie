package magpie.user.server.operations;

import magpie.data.BaseEntry;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import magpie.data.utilities.filters.BaseDatasetFilter;
import magpie.data.utilities.filters.EntryRankerFilter;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.user.CommandHandler;
import magpie.user.server.ModelPackage;
import magpie.user.server.ServerLauncher;
import magpie.user.server.ServerUtilityOperations;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Perform searches that involve several different ML models.
 * <p>
 * <p>Search is formatted as a JSON file with a format:</p>
 * <ul>
 * <li><b>datasetType</b>: Type of dataset used to store initial entries</li>
 * <li><b>entryGenerator</b>: Definition of a {@linkplain magpie.data.utilities.generators.BaseEntryGenerator}</li>
 * <li><b>steps</b>: Steps in the analysis, which can be:
 * <ul>
 * <li>Model: Name of model to be run. Results are stored as a property</li>
 * <li>Filter: Name and options filter to be employed</li>
 * </ul>
 * <p>Each of these should be formatted as a JSON object,
 * with keys type (string) and options (JSON object)</p>
 * </li>
 * <li>
 * <b>entryRanker</b>: Method used to rank entries
 * <ul>
 * <li><b>method</b>: Method used to rank entries</li>
 * <li><b>number</b>: Number of top entries to select</li>
 * <li><b>minimize</b>: Boolean, whether to minimize or maximize the objective function</li>
 * <li><b>options</b>: Array, options for the objective function</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * <p>This generates an output JSON structure:</p>
 * <p>
 * <ul>
 * <li><b>chosenEntries</b>: List of entries, as string</li>
 * <li><b>data</b>: Detailed data for each model run</li>
 * </ul>
 *
 * @author Logan Ward
 */
@Path("search")
public class SearchRunner {
    /**
     * Set of models that have been run
     */
    protected Set<String> Models = new TreeSet<>();

    @POST
    @Produces("application/json")
    public String runSearch(@FormParam("search") String searchForm) {
        // Parse the serach information
        JSONObject searchDefinition;
        try {
            searchDefinition = new JSONObject(searchForm);
        } catch (Exception e) {
            throw ServerUtilityOperations.prepareException("search failed to parse as JSON: "
                    + e.getMessage());
        }

        // Generate entries in search space
        MultiPropertyDataset data = getEntries(searchDefinition);

        // Perform each analysis steps
        if (!searchDefinition.has("steps")) {
            throw ServerUtilityOperations.prepareException("search definition is missing steps");
        }
        if (!(searchDefinition.get("steps") instanceof JSONArray)) {
            throw ServerUtilityOperations.prepareException("steps should be an array");
        }
        for (Object stepPtr : searchDefinition.getJSONArray("steps")) {
            // Check if step is a JSON object
            if (!(stepPtr instanceof JSONObject)) {
                throw ServerUtilityOperations.prepareException("step should be a JSON object");
            }
            JSONObject step = (JSONObject) stepPtr;

            // Run the step
            if (!step.has("type")) {
                throw ServerUtilityOperations.prepareException("step should have key: type");
            }
            if (!(step.has("options") && step.get("options") instanceof JSONObject)) {
                throw ServerUtilityOperations.prepareException("step should have key, options, that maps to an array");
            }
            String stepType = step.getString("type");
            JSONObject options = step.getJSONObject("options");
            switch (stepType.toLowerCase()) {
                case "model":
                    data = runModel(data, options);
                    break;
                case "filter":
                    runFilter(data, options);
                    break;
            }
        }

        // Rank the top entries
        if (!(searchDefinition.has("entryRanker") && searchDefinition.get("entryRanker") instanceof JSONObject)) {
            throw ServerUtilityOperations.prepareException("options should have entryRanker as a JSON object");
        }
        JSONObject entryRankerSpec = searchDefinition.getJSONObject("entryRanker");
        runRanker(data, entryRankerSpec);

        // Clear out the attributes
        data.clearAttributes();

        // Convert dataset to JSON
        return convertToJSON(data).toString();
    }

    /**
     * Convert the dataset into a JSON object
     *
     * @param data Dataset to be converted
     * @return Output describing the selected entries
     */
    public JSONObject convertToJSON(MultiPropertyDataset data) {
        // Gather list of entries to be parsed
        JSONObject output = new JSONObject();
        List<String> chosenEntries = new ArrayList<>(data.NEntries());
        for (BaseEntry entry : data.getEntries()) {
            chosenEntries.add(entry.toHTMLString());
        }
        output.put("chosenEntries", chosenEntries);

        // Write out datasets for each
        JSONObject datasetData = new JSONObject();
        for (String modelName : Models) {
            JSONObject datasetJSON = ServerUtilityOperations.createDatasetJSON(
                    ServerLauncher.Models.get(modelName),
                    null,
                    data
            );
            datasetData.put(modelName, datasetJSON);
        }
        output.put("data", datasetData);

        return output;
    }

    /**
     * Run the entry ranker to get the top entries
     *
     * @param data          [in/out] Dataset to be ranked, will contain only top entries after running
     * @param rankerOptions Optiosn for the ranker
     */
    public void runRanker(MultiPropertyDataset data, JSONObject rankerOptions) {
        // Check the format of the input
        if (!rankerOptions.has("method")) {
            throw ServerUtilityOperations.prepareException("ranker options should have: method");
        }
        String rankerMethod = rankerOptions.getString("method");
        if (!rankerOptions.has("number")) {
            throw ServerUtilityOperations.prepareException("ranker options should have: number");
        }
        int number = rankerOptions.getInt("number");
        if (!(rankerOptions.has("minimize") && rankerOptions.get("minimize") instanceof Boolean)) {
            throw ServerUtilityOperations.prepareException("ranker options should have: minimize as a boolean");
        }
        boolean toMinimize = rankerOptions.getBoolean("minimize");
        if (!(rankerOptions.has("options") && rankerOptions.get("options") instanceof JSONArray)) {
            throw ServerUtilityOperations.prepareException("ranker options should have: options as an array");
        }
        List<Object> options = rankerOptions.getJSONArray("options").toList();

        // Create the entry ranker
        BaseEntryRanker ranker;
        try {
            ranker = (BaseEntryRanker) CommandHandler.instantiateClass("optimization.rankers." + rankerMethod, options);
        } catch (Exception e) {
            throw ServerUtilityOperations.prepareException("instantiating ranker failed: " + e.getMessage());
        }
        ranker.setMaximizeFunction(!toMinimize);
        ranker.setUseMeasured(false);
        ranker.train(data);

        // Run the ranker as a filter
        EntryRankerFilter filter = new EntryRankerFilter();
        filter.setExclude(false);
        filter.setNumberToFilter(number);
        filter.setRanker(ranker);

        filter.train(data);
        filter.filter(data);

        // Sort entries according to ranker
        ranker.sortByRanking(data);
    }

    /**
     * Run a new model, add entries to dataset
     *
     * @param data          Dataset of entries to run
     * @param filterOptions Options for model
     */
    public void runFilter(MultiPropertyDataset data, JSONObject filterOptions) {
        // Get the methods
        if (!filterOptions.has("name")) {
            throw ServerUtilityOperations.prepareException("filter options should have: name");
        }
        String name = filterOptions.get("name").toString();
        if (!(filterOptions.has("exclude") && filterOptions.get("exclude") instanceof Boolean)) {
            throw ServerUtilityOperations.prepareException("filter options should have: exclude");
        }
        Boolean toExclude = filterOptions.getBoolean("exclude");
        if (!(filterOptions.has("options") && filterOptions.get("options") instanceof JSONArray)) {
            throw ServerUtilityOperations.prepareException("filter options should have: options");
        }
        List<Object> options = filterOptions.getJSONArray("options").toList();

        // Instantiate it
        BaseDatasetFilter filter;
        try {
            filter = (BaseDatasetFilter) CommandHandler.instantiateClass("data.utilities.filters." + name, options);
        } catch (Exception e) {
            throw ServerUtilityOperations.prepareException("problem loading filter: " + e.getMessage());
        }
        filter.setExclude(toExclude);

        // Train and run the filter
        filter.train(data);
        filter.filter(data);
    }

    /**
     * Run a new model, add entries to dataset
     *
     * @param data    Dataset of entries to run
     * @param options Options for model. Should have 1 entry: model name
     * @return Entries in a new dataset
     */
    public MultiPropertyDataset runModel(MultiPropertyDataset data, JSONObject options) {
        // Get the model
        if (options.length() != 1 || !options.has("name")) {
            throw ServerUtilityOperations.prepareException("should have only one option: name");
        }
        String name = options.get("name").toString();
        if (!ServerLauncher.Models.containsKey(name)) {
            throw ServerUtilityOperations.prepareException("no such model: " + name);
        }
        ModelPackage model = ServerLauncher.Models.get(name);
        Models.add(name);

        // Add entries to dataset for this model
        MultiPropertyDataset newDataset = (MultiPropertyDataset) model.getDatasetCopy();
        try {
            data.clearAttributes(); // Remove attributes first
            newDataset.addEntries(data, true);
        } catch (Exception e) {
            throw ServerUtilityOperations.prepareException("failed when merging dataset: this is a problem with Magpie - not you");
        }

        // Add a new property for this model
        if (model.isClassifer()) {
            newDataset.addProperty(name, model.getPossibleClasses());
        } else {
            newDataset.addProperty(name);
        }
        for (BaseEntry entry : newDataset.getEntries()) {
            ((MultiPropertyEntry) entry).addProperty();
        }
        newDataset.setTargetProperty(name, true);

        // Run the model
        try {
            model.runModel(newDataset);
        } catch (Exception e) {
            throw ServerUtilityOperations.prepareException("failed when running model: " + e.getMessage());
        }

        return newDataset;
    }

    /**
     * Generate a list of entries
     *
     * @param searchDefinition Definition of search routine, input to the search routine
     * @return Dataset containing entries
     */
    public MultiPropertyDataset getEntries(JSONObject searchDefinition) {
        // Create the entry generator
        if (!searchDefinition.has("entryGenerator")) {
            throw ServerUtilityOperations.prepareException("search definition missing entryGenerator");
        }
        if (!(searchDefinition.get("entryGenerator") instanceof JSONArray)) {
            throw ServerUtilityOperations.prepareException("entryGenerator must be an array");
        }
        BaseEntryGenerator generator;
        try {
            JSONArray genData = searchDefinition.getJSONArray("entryGenerator");
            if (genData.length() == 0) {
                throw ServerUtilityOperations.prepareException("entryGenerator must have entries");
            }
            String genName = genData.getString(0);
            List<Object> genOptions = genData.toList().subList(1, genData.length());

            generator = (BaseEntryGenerator) CommandHandler.instantiateClass(
                    "data.utilities.generators." + genName, genOptions
            );
        } catch (Exception e) {
            throw ServerUtilityOperations.prepareException("problem instantiating entryGenerator: " + e.getMessage());
        }

        // Make entries
        if (!searchDefinition.has("datasetType")) {
            throw ServerUtilityOperations.prepareException("search definition missing datasetType");
        }
        Object dataObj;
        try {
            dataObj = CommandHandler.instantiateClass("data." + searchDefinition.get("datasetType").toString(), null);
        } catch (Exception e) {
            throw ServerUtilityOperations.prepareException("no such dataset type: " + searchDefinition.get("datasetType").toString());
        }
        MultiPropertyDataset data = (MultiPropertyDataset) dataObj;
        generator.addEntriesToDataset(data);
        return data;
    }
}
