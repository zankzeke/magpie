package magpie.user.server.operations;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.user.server.ModelPackage;
import magpie.user.server.ServerLauncher;
import magpie.utility.UtilityOperations;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Queries for getting the information about and running models.
 *
 * @author Logan Ward
 */
@Path("model/{name}")
public class ModelOperator {
    /**
     * Name of the model
     */
    @PathParam("name")
    private String Name;

    /**
     * Package storing the model
     */
    private ModelPackage Model;

    /**
     * Prepare an Exception in a form that will get the CORS headers
     * @param message Exception message
     * @return Exception with the desired message
     */
    public static WebApplicationException prepareException(String message) {
        return new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity(message)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
                .build());
    }

    /**
     * Get the model used for this request
     */
    private void getModel() {
        if (!ServerLauncher.Models.containsKey(Name)) {
            throw new WebApplicationException("No such model: " + Name, Response.Status.NOT_FOUND);
        }
        Model = ServerLauncher.Models.get(Name);
    }

    /**
     * Get information about the model
     */
    @GET
    @Produces("application/json")
    @Path("info")
    public String getModelInfo() {
        getModel();
        return Model.toJSON().toString();
    }

    /**
     * Download the model file via serialization
     */
    @GET
    @Produces("application/octet-stream")
    @Path("model")
    public StreamingOutput downloadModel() {
        getModel();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Model.writeModel(output);
            }
        };
    }

    /**
     * Download the model file via serialization
     */
    @GET
    @Produces("application/octet-stream")
    @Path("dataset")
    public StreamingOutput downloadDataset() {
        getModel();
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                Model.writeDataset(output);
            }
        };
    }

    /**
     * Generate attributes with the dataset for a certain model
     *
     * @return Dataset as a JSON file with the format:
     * <p>{
     *     'attributes': [list of attribute names],
     *     'entries': [list of entries, their names and attributes]
     * }
     * </p>
     */
    @POST
    @Produces("application/json")
    @Path("attributes")
    public String generateAttributes(@FormParam("entries") String userInput) {
        getModel();

        // Get the data about the entries, as provided by user
        List<String> entryNames = getEntryData(userInput);

        // Read entries into the dataset
        final Dataset data = assembleDataset(entryNames);

        // Submit the model to be run
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                try {
                    data.generateAttributes();
                } catch (Exception e) {
                    throw prepareException("attribute generation failed: " + e.getMessage());
                }
            }
        };
        Future future = ServerLauncher.ThreadPool.submit(thread);

        // Wait until thread finishes
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Assemble the output
        JSONObject output = createDatasetJSON(entryNames, data);
        return output.toString();
    }

    /**
     * Run a certain model
     *
     * @return Dataset as a JSON file with the format:
     * <p>{
     * 'possibleClasses': [list of class names], <-- Only if classification model
     * 'units': [units], <-- Only if a regression model
     * 'entries': [list of entries, their names predicted values]
     * }
     * </p>
     */
    @POST
    @Produces("application/json")
    @Path("run")
    public String runModel(@FormParam("entries") String userInput) {
        getModel();

        // Get the data about the entries, as provided by user
        List<String> entryNames = getEntryData(userInput);

        // Read entries into the dataset
        final Dataset data = assembleDataset(entryNames);

        // Submit the model to be run
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                try {
                    Model.runModel(data);
                    data.clearAttributes();
                } catch (Exception e) {
                    throw prepareException("attribute generation failed: " + e.getMessage());
                }
            }
        };
        Future future = ServerLauncher.ThreadPool.submit(thread);

        // Wait until thread finishes
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Assemble the output
        JSONObject output = createDatasetJSON(entryNames, data);
        return output.toString();
    }

    /**
     * Turn a dataset into a JSON object. Follows the schema described in the Swagger API
     *
     * @param entryNames Names of entries, as strings
     * @param data       Dataset to be converted
     * @return Dataset as a JSON object
     */
    public JSONObject createDatasetJSON(List<String> entryNames, Dataset data) {
        JSONObject output = new JSONObject();

        // Put in model details
        String[] classNames = Model.isClassifer() ? Model.getPossibleClasses() : null;
        if (Model.isClassifer()) {
            output.put("possibleClasses", classNames);
            output.put("modelType", "classification");
        } else {
            output.put("units", Model.getUnits());
            output.put("modelType", "regression");
        }
        output.put("property", Model.Property);

        // Put in the attribute names, if present
        if (data.NAttributes() > 0) {
            output.put("attributes", data.getAttributeNames());
        }

        JSONArray entryArray = new JSONArray();
        for (int e = 0; e < entryNames.size(); e++) {
            JSONObject entryJSON = new JSONObject();
            BaseEntry entry = data.getEntry(e);

            // Get the entry data
            entryJSON.put("name", entryNames.get(e));
            entryJSON.put("parsedName", entry.toString());

            // Add in the attributes, if present
            if (entry.NAttributes() > 0) {
                entryJSON.put("attributes", UtilityOperations.toJSONArray(entry.getAttributes()));
            }

            // If predicted values
            if (entry.hasPrediction()) {
                // Get the predictions
                entryJSON.put("predictedValue", entry.getPredictedClass());
                if (Model.isClassifer()) {
                    // Text name of class
                    entryJSON.put("predictedClass", classNames[(int) entry.getPredictedClass()]);

                    // Class probabilities
                    JSONObject probs = new JSONObject();
                    double[] predProbs = entry.getClassProbilities();
                    for (int cl = 0; cl < classNames.length; cl++) {
                        probs.put(classNames[cl], predProbs[cl]);
                    }
                    entryJSON.put("classProbabilities", probs);
                }
            }

            // Add it to the list
            entryArray.put(entryJSON);
        }
        output.put("entries", entryArray);
        return output;
    }

    /**
     * Given a list of entry names, parse them to make a dataset
     * @param entryNames Names of entries to be parsed
     * @return Dataset object containing those entries
     */
    public Dataset assembleDataset(List<String> entryNames) {
        final Dataset data = Model.getDatasetCopy();
        for (String entry : entryNames) {
            try {
                data.addEntry(entry);
            } catch (Exception e) {
                prepareException(String.format("entry \"%s\" failed to parse: %s"));
            }
        }
        return data;
    }

    /**
     * Given a JSON file send from a user, get the names of entries to be run
     *
     * @param userInput Input from user
     * @return List of entry names, ready to be parsed
     */
    protected List<String> getEntryData(String userInput) {
        // Read in the dataset
        final JSONObject entries;
        try {
            entries = new JSONObject(userInput);
        } catch (JSONException e) {
            throw prepareException("input failed to parse as JSON: " + e.getMessage());
        }

        // Check format
        if (!entries.has("entries")) {
            throw prepareException("bad format: dataset should contain key 'entries'");
        }

        // Get the user-provided names of entries
        List<String> entryNames = new ArrayList<>(entries.getJSONArray("entries").length());
        for (Object entryPtr : entries.getJSONArray("entries")) {
            if (!(entryPtr instanceof JSONObject)) {
                throw prepareException("bad format: entries does not contain JSON objects");
            }
            JSONObject entry = (JSONObject) entryPtr;
            if (!entry.has("name")) {
                throw prepareException("bad format: entry should contain key 'name'");
            }
            entryNames.add(entry.getString("name"));
        }
        return entryNames;
    }

}
