package magpie.user.server.operations;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * REST responses that give the status of the server
 *
 * @author Logan Ward
 */
@Path("version")
public class ServerVersion {

    @GET
    @Produces("text/simple")
    public String returnVersion() {
        return "0.0.1";
    }
}
