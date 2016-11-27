package magpie.user.server.operations;

import magpie.data.Dataset;
import magpie.user.CommandHandler;
import magpie.user.server.ModelPackage;
import magpie.user.server.ServerLauncher;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Run operations to gather models that fit certain criteria.
 *
 * @author Logan Ward
 */
@Path("models")
public class ModelQueryHandler {
    /**
     * Get all models that match the users query
     *
     * @param datasetType         Desired dataset type, looks for exact match
     * @param supportsDatasetType Get models that at least support this kind of dataset type
     * @return JSON object describing each matching model
     */
    @GET
    @Produces("application/json")
    public String serveModels(@DefaultValue("") @QueryParam("datasetType") String datasetType,
                              @DefaultValue("") @QueryParam("supportsDatasetType") String supportsDatasetType) {
        // Get all models
        Map<String, ModelPackage> modelsToOutput = new TreeMap<>(ServerLauncher.Models);

        // Run query based on dataset type
        if (datasetType.length() > 0) {
            // Get the class from the user provided dataset type
            Dataset dataset = getDatasetByName(datasetType);

            // Test whether the dataset types match
            Iterator<Map.Entry<String, ModelPackage>> iter = modelsToOutput.entrySet().iterator();
            while (iter.hasNext()) {
                if (!iter.next().getValue().datasetMatches(dataset)) {
                    iter.remove();
                }
            }
        }

        // Run query based on dataset type
        if (supportsDatasetType.length() > 0) {
            // Get the class from the user provided dataset type
            Dataset dataset = getDatasetByName(supportsDatasetType);

            // Test whether the dataset types match
            Iterator<Map.Entry<String, ModelPackage>> iter = modelsToOutput.entrySet().iterator();
            while (iter.hasNext()) {
                if (!iter.next().getValue().modelSupports(dataset)) {
                    iter.remove();
                }
            }
        }

        // Return the model information for a model
        JSONObject output = new JSONObject();
        for (Map.Entry<String, ModelPackage> entry : modelsToOutput.entrySet()) {
            output.put(entry.getKey(), entry.getValue().toJSON());
        }
        return output.toString();
    }

    /**
     * Given the name of a dataset object, return an instance of that class
     *
     * @param datasetType Name of dataset, without the "magpie.data" (e.g., "materials.CompositionDataset")
     * @return Dataset object
     */
    public Dataset getDatasetByName(@DefaultValue("") @QueryParam("datasetType") String datasetType) {
        Dataset dataset;
        try {
            dataset = (Dataset) CommandHandler.instantiateClass("data." + datasetType, null);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("dataset type not recognized or invalid: " + datasetType).build());
        }
        return dataset;
    }
}

