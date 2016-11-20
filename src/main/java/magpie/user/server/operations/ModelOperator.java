package magpie.user.server.operations;

import magpie.data.Dataset;
import magpie.user.server.ModelPackage;
import magpie.user.server.ServerLauncher;
import magpie.utility.UtilityOperations;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

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
     * @return Dataset streaming back as a JSON file
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

        // Read entries into the dataset
        final Dataset data = Model.Dataset.emptyClone();
        for (Object entry : entries.getJSONArray("data")) {
            try {
                data.addEntry(entry.toString());
            } catch (Exception e) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity("entries failed to parse: " + e.getMessage()).build());
            }
        }

        // Run the model
        try {
            data.generateAttributes();
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("attribute generation failed: " + e.getMessage()).build());
        }
        Model.Model.run(data);

        return data.toJSON().toString();
    }

}
