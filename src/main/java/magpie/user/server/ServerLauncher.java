
package magpie.user.server;

import magpie.Magpie;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.utility.WekaUtility;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * This file should be formatted in YAML and follow the following structure:
 * 
 * <div style="padding: 10px 0 0 20px;">
 *     This YAML file should be partitioned into several separate documents, which each describe a different model and
 *     are separated by lines of '---'.
 *
 *     <p>Each document should contain the following fields:</p>
 *     <ul>
 *         <li><b>name</b> Name for this model</li>
 *         <li><b>modelPath</b> Path to the model file</li>
 *         <li><b>datasetPath</b> Path to the dataset</li>
 *         <li><b>description</b> Short description of model</li>
 *         <li><b>property</b> Name of property being modeled, or a short description</li>
 *         <li><b>units</b> (Optional) Units for the property</li>
 *         <li><b>training</b> Short description of the training set</li>
 *         <li><b>citation</b> Citation for the model</li>
 *         <li><b>author</b> Name of author of the model</li>
 *         <li><b>notes</b> Longer description of the model</li>
 *     </ul>
 *
 *     <p>Feel free to use HTML formatting in the YAML file. This information will likely be rendered by a web browser</p>
 * </div>
 * 
 * 
 * <p>Example: java -jar Magpie.jar -server -model volume volume.obj -data data.obj
 * 
 * <p><b>Client Implementation Guide</b>
 *
 * <p>This code uses a REST interface. Eventually, we may provide a wrapper for this API in other languages. There will
 * at least be a few examples of webpages using this interface.</p>
 * 
 * @author Logan Ward
 */
public class ServerLauncher {
    /** Port on which to listen */
    public static int ListenPort = 4581;
    /**
     * List of models available to this program
     */
    public static Map<String, ModelPackage> Models = new TreeMap<>();
    /**
     * Server currently being used
     */
    public static HttpServer Server = null;
    /**
     * Time server was launched
     */
    public static Date StartDate;
    /**
     * Executor used to prevent too many complex calculations at once.
     */
    public static ExecutorService ThreadPool;

    
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
                    System.out.println("Set port: " + ListenPort);
                    break;
                case "-models":
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

        // Clear the list of models
        Models.clear();

        // Parse the input file
        Yaml yaml = new Yaml();
        FileInputStream fp = new FileInputStream(path);
        for (Object modelDataObj : yaml.loadAll(fp)) {
            Map<String, Object> modelData = (Map) modelDataObj;

            // Get the name of the model
            String modelName = modelData.get("name").toString();

            // Get the path to the model and dataset
            String modelPath = modelData.get("modelPath").toString();
            String dataPath = modelData.get("datasetPath").toString();

            // Read in the files
            Dataset dataset = Dataset.loadState(dataPath);
            dataset = dataset.emptyClone();
            BaseModel model = BaseModel.loadState(modelPath);

            // Read both to generate model package
            ModelPackage modelPackage = new ModelPackage(dataset, model);

            // Read in the other information
            modelPackage.Description = modelData.get("description").toString();
            modelPackage.Property = modelData.get("property").toString();
            modelPackage.Units = modelData.containsKey("units") ? modelData.get("units").toString() : "None";
            modelPackage.TrainingSet = modelData.get("training").toString();
            modelPackage.Author = modelData.get("author").toString();
            modelPackage.Citation = modelData.get("citation").toString();
            modelPackage.Notes = modelData.get("notes").toString();

            // Store the model
            Models.put(modelName, modelPackage);
        }
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
        Server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://0.0.0.0:" + ListenPort), cfg);

        // Add hook to shutdown Server
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Server.shutdownNow();
            }
        }));

        // Create the thread pool for running models, etc.
        ThreadPool = Executors.newFixedThreadPool(Magpie.NThreads);
        Magpie.NThreads = 1; // Prevent any other parallel operations

        // Launch it
        Server.start();
        StartDate = new Date();
    }
}
