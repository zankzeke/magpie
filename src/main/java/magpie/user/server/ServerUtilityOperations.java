package magpie.user.server;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.UtilityOperations;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Utility operations useful for the REST server.
 *
 * @author Logan Ward
 */
abstract public class ServerUtilityOperations {
    /**
     * Prepare an Exception in a form that will get the CORS headers
     *
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
     * Turn a dataset into a JSON object. Follows the schema described in the Swagger API
     *
     * @param model      Model used to generate this data
     * @param entryNames Names of entries, as strings
     * @param data       Dataset to be converted
     * @return Dataset as a JSON object
     */
    static public JSONObject createDatasetJSON(ModelPackage model, List<String> entryNames, Dataset data) {
        JSONObject output = new JSONObject();

        // Put in model details
        String[] classNames = model.isClassifer() ? model.getPossibleClasses() : null;
        if (model.isClassifer()) {
            output.put("possibleClasses", classNames);
            output.put("modelType", "classification");
        } else {
            output.put("units", model.getUnits());
            output.put("modelType", "regression");
        }
        output.put("property", model.Property);

        // Put in the attribute names, if present
        if (data.NAttributes() > 0) {
            output.put("attributes", data.getAttributeNames());
        }

        JSONArray entryArray = new JSONArray();
        for (int e = 0; e < data.NEntries(); e++) {
            JSONObject entryJSON = new JSONObject();
            BaseEntry entry = data.getEntry(e);

            // Get the entry data
            if (entryNames != null) {
                entryJSON.put("name", entryNames.get(e));
            }
            entryJSON.put("parsedName", entry.toString());

            // Add in the attributes, if present
            if (entry.NAttributes() > 0) {
                entryJSON.put("attributes", UtilityOperations.toJSONArray(entry.getAttributes()));
            }

            // If predicted values
            if (entry.hasPrediction()) {
                // Get the predictions
                entryJSON.put("predictedValue", entry.getPredictedClass());
                if (model.isClassifer()) {
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
}
