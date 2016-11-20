package magpie.user.server.operations;

import magpie.Magpie;
import magpie.user.server.ServerLauncher;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * REST responses that give the status of the server
 *
 * @author Logan Ward
 */
@Path("server")
public class ServerInformationGetter {

    /**
     * @return REST API version
     */
    @GET
    @Produces("text/simple")
    @Path("version")
    public String getVersion() {
        return "0.0.1";
    }

    /**
     * @return Some statistics about when this server was launched
     */
    @GET
    @Produces("application/json")
    @Path("status")
    public String serverStatistics() {
        // Create the output object
        JSONObject output = new JSONObject();

        // Information about version of server
        output.put("apiVersion", getVersion());
        output.put("magpieVersion", "not yet implemented. complain at Logan Ward");

        // Get information about the server
        Runtime runtime = Runtime.getRuntime();
        output.put("startDate", ServerLauncher.StartDate.toString());
        long upTime = new Date().getTime() - ServerLauncher.StartDate.getTime();
        output.put("uptime", String.format("%d days, %02d hours, %02d minutes",
                TimeUnit.MILLISECONDS.toDays(upTime),
                TimeUnit.MILLISECONDS.toHours(upTime) - 24 * TimeUnit.MILLISECONDS.toDays(upTime),
                TimeUnit.MILLISECONDS.toMinutes(upTime) - 60 * TimeUnit.MILLISECONDS.toHours(upTime)
                        - 24 * TimeUnit.MILLISECONDS.toDays(upTime)
        ));
        output.put("availableProcessors", runtime.availableProcessors());
        output.put("allowedProcessors", Magpie.NThreads);
        output.put("availableMemory", runtime.maxMemory());
        output.put("freeMemory", runtime.freeMemory());

        return output.toString();
    }
}
