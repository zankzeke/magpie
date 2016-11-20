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
                UtilityOperations.saveState(Model.Model, output);
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
                UtilityOperations.saveState(Model.Dataset, output);
            }
        };
    }

    /**
     * Generate attributes with the dataset for a certain model
     *
     * @return Dataset streaming back as a JSON file with the format:
     * <p>{
     *     'attributes': [list of attribute names],
     *     'data': [list of entries, their names and attributes]
     * }
     * </p>
     */
    @POST
    @Produces("application/json")
    @Path("attributes")
    public String generateAttributes(@FormParam("entries") String userInput) {
        getModel();

        // Read in the dataset
        final JSONObject entries;
        try {
            entries = new JSONObject(userInput);
        } catch (JSONException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("input failed to parse as JSON: " + e.getMessage()).build());
        }

        // Check format
        if (!entries.has("data")) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("bad format: entries should contain key 'data'").build());
        }

        // Get the user-provided names of entries
        List<String> entryNames = new ArrayList<>(entries.getJSONArray("data").length());
        for (Object entry : entries.getJSONArray("data")) {
            entryNames.add(entry.toString());
        }

        // Read entries into the dataset
        final Dataset data = Model.Dataset.emptyClone();
        for (String entry : entryNames) {
            try {
                data.addEntry(entry);
            } catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity(String.format("entry \"%s\" failed to parse: %s",
                                entry,
                                e.getMessage()))
                        .build());
            }
        }

        // Submit the model to be run
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                try {
                    data.generateAttributes();
                } catch (Exception e) {
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                            .entity("attribute generation failed: " + e.getMessage()).build());
                }
                Model.Model.run(data);
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
        JSONObject output = new JSONObject();

        output.put("attributes", data.getAttributeNames());

        JSONArray entryArray = new JSONArray();
        for (int e = 0; e < entryNames.size(); e++) {
            JSONObject entryJSON = new JSONObject();
            BaseEntry entry = data.getEntry(e);

            // Get the entry data
            entryJSON.put("name", entryNames.get(e));
            entryJSON.put("parsedName", entry.toString());
            entryJSON.put("attributes", UtilityOperations.toJSONArray(entry.getAttributes()));

            // Add it to the list
            entryArray.put(entryJSON);
        }
        output.put("entries", entryArray);

        return output.toString();
    }

}
