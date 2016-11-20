package magpie.user.server.operations;

import magpie.user.server.ModelPackage;
import magpie.user.server.ServerLauncher;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * Queries for getting the information about models.
 *
 * @author Logan Ward
 */
@Path("model/")
public class ModelInformation {
    @GET
    @Produces("application/json")
    @Path("{name}/info")
    public String getModelInfo(@PathParam("name") String modelName) {
        if (ServerLauncher.Models.containsKey(modelName)) {
            ModelPackage modelPackage = ServerLauncher.Models.get(modelName);
            return modelPackage.toJSON().toString();
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
}
