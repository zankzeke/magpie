
package magpie.user.server;

import magpie.utility.WekaUtility;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;

/**
 * Main class for launching a Magpie server. 
 * 
 * <p><b>How to start server</b>
 * 
 * <p>ServerLauncher can be started simply by starting Magpie with the "-server" 
 * flag. Invoked in this why, Magpie takes a few command line arguments:
 * 
 * <br><b>-port &lt;port&gt;</b>: Port on which to launch server. Socket server (e.g., what
 * you would use with Python client) starts on this server, HTTP server will start
 * on port + 1.
 * <br><b>-models &lt;path&gt;</b>: Path to file describing models to be served.
 * This file must follow the following format:
 * 
 * <div style="padding: 10px 0 0 20px;">
 * This file should contain a list of entries describing how to run each model,
 * and information necessary for someone to properly use it. Each model should
 * be described by an entry in the following format. For longer descriptions, it
 * is advised to use HTML format.
 * <p>entry &lt;name&gt; // Unique name used to describe this model
 * <div style="padding: 0 0 0 20px;">
 * &lt;model path&gt; // Path to model to evaluated
 * &lt;dataset path&gt; // Path to dataset used to generate attributes
 * <br>property &lt;description&gt; // Short description of the property being predicted
 * <br>units &lt;path&gt; // Define the units of this model
 * <br>training &lt;description&gt; // Description of training set
 * <br>author &lt;name&gt; // Contact information for author
 * <br>citation &lt;info&gt; // Information for how to properly cite this model
 * <br>description &lt;words&gt; // Short description of model
 * <br>notes &lt;words&gt; // Longer description of model
 * </div>
 * 
 * </div>
 * 
 * 
 * <p>Example: java -jar Magpie.jar -server -model volume volume.obj -data data.obj
 * 
 * <p><b>Client Implementation Guide</b>
 * 
 * <p>This code uses <a href="http://thrift.apache.org/">Apache Thrift</a> to 
 * define the interface. There are several different commands available through
 * this API, which are described in the "magpie.thrift" file included with this
 * software package. An example for a client that uses the JavaScript interface
 * and an example Python client are provided.
 * 
 * @author Logan Ward
 */
public class ServerLauncher {
    /** Port on which to listen */
    public static int ListenPort = 4581;
    
    /**
     * Handle input passed to the server. See class documentation for format
     * @param args Input 
     * @throws java.lang.Exception 
     */
    public static void parseInput(String[] args) throws Exception {
        int pos = 0;
        while (pos < args.length) {
            String tag = args[pos].toLowerCase();
            switch (tag) {
                case "-port":
                    ListenPort = Integer.parseInt(args[++pos]);
                    System.out.println("Set ports: " + ListenPort);
                    break;
                case "-model":
                    readInformationFile(args[++pos]);
                    break;
                default:
                    throw new Exception("Unknown tag: " + tag);
            }
            pos++;
        }
    }
    
    /**
     * Given model information file, configure the handler
     * @param path Path to model information file
     * @throws Exception
     */
    public static void readInformationFile(String path) throws Exception {
        // Make sure Weka models are available
        WekaUtility.importWekaHome();
    }
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    static public void main(String[] args) throws Exception {
        // Read input arguments
        parseInput(args);
        
        // Fire it up!
        startServer();
    }

    /**
     * Given the current settings, start the Magpie server
     * @throws Exception 
     */
    public static void startServer() throws Exception {
        // Make the HTTP server
        final ResourceConfig cfg = new ResourceConfig().packages("magpie.user.server");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://0.0.0.0:" + ListenPort), cfg);

        // Launch it
        server.start();
    }
}
