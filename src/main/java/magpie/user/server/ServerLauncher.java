
package magpie.user.server;

import java.io.BufferedReader;
import java.io.FileReader;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.user.server.thrift.MagpieServer;
import magpie.user.server.thrift.MagpieServerHandler;
import magpie.utility.WekaUtility;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

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
    /** Socket server */
    public static TServer SocketServer;
    /** HTTP server */
    public static Server HTTPServer;
    /** Tool used to handle requests */
    public static MagpieServerHandler Handler = new MagpieServerHandler();
    
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
                    System.out.println("Set listen ports:");
                    System.out.println("\tSocket port: " + ListenPort);
                    System.out.println("\tHTTPClient port: " + (ListenPort + 1));
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
     * @see MagpieServer
     */
    public static void readInformationFile(String path) throws Exception {
        // Make sure Weka models are available
        WekaUtility.importWekaHome();
        
        // Open up the reader
        BufferedReader reader = new BufferedReader(new FileReader(path));
        
        // Read in model information
        String line = "";
        while (true) {
            if (! line.toLowerCase().startsWith("entry ")) {
                line = reader.readLine();
            }
            if (line == null) {
                break;
            }
            String[] words = line.split("[ \t]");
            if (words.length == 0) {
                continue;
            }
            if (! words[0].equalsIgnoreCase("entry")) {
                continue;
            }
            
            // Get the name of this model
            String name = words[1];
            System.out.println("Creating model: " + name);
            
            // Read in model and dataset
            line = reader.readLine();
            if (line == null) {
                throw new Exception("Format error: Missing line for model path");
            }
            System.out.println("\tReading in model from: " + line);
            BaseModel model;
            try {
                model = BaseModel.loadState(line);
            } catch (Exception e) {
                System.err.println("Model failed to read: " + e.getLocalizedMessage());
                continue;
            }
            
            line = reader.readLine();
            if (line == null) {
                throw new Exception("Format error: Missing line for dataset path");
            }
            System.out.println("\tReading in dataset from: " + line);
            Dataset data;
            try {
                data = Dataset.loadState(line).emptyClone();
            } catch (Exception e) {
                System.err.println("Dataset failed to read: " + e.getLocalizedMessage());
                continue;
            }
            
            // Create the information holder
            ModelPackage modelInfo = new ModelPackage(data, model);
            
            // Read in other stuff
            line = reader.readLine();
            while (line != null && ! line.toLowerCase().startsWith("entry ")) {
                words = line.split("[ \t]");
                if (words.length == 1) {
                    line = reader.readLine();
                    continue;
                }
                switch (words[0]) {
                    case "property":
                        modelInfo.Property = line.replaceFirst("property", "").trim();
                        System.out.println("\tProperty: " + modelInfo.Property);
                        break;
                    case "units":
                        modelInfo.Units = line.replaceFirst("units", "").trim();
                        System.out.println("\tUnits: " + modelInfo.Units);
                        break;
                    case "author":
                        modelInfo.Author = line.replaceFirst("author", "").trim();
                        System.out.println("\tAuthor: " + modelInfo.Author);
                        break;
                    case "citation":
                        modelInfo.Citation = line.replaceFirst("citation", "").trim();
                        System.out.println("\tCitation: " + modelInfo.Citation);
                        break;
                    case "description":
                        modelInfo.Description = line.replaceFirst("description", "").trim();
                        System.out.println("\tdescription: " + modelInfo.Description);
                        break;
                    case "training":
                        modelInfo.TrainingSet = line.replaceFirst("training", "").trim();
                        System.out.println("\tTraining set: " + modelInfo.TrainingSet);
                        break;
                    case "notes":
                        modelInfo.Notes = line.replaceFirst("notes", "").trim();
                        System.out.println("\tNotes: " + modelInfo.Notes);
                        break;
                    default:
                        System.out.println("Unrecognized property: " + words[0]);
                }
                line = reader.readLine();
            }
            
            // Add in model to handler
            Handler.addModel(name, modelInfo);

            // If at end of file, break
            if (line == null) break;
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
     * @throws TTransportException
     * @throws Exception 
     */
    public static void startServer() throws TTransportException, Exception {
        // Create the processor
        MagpieServer.Processor processor = new MagpieServer.Processor(Handler);
        
        // Initialize the server
        TServerTransport trans = new TServerSocket(ListenPort);
        SocketServer = new TThreadPoolServer(new TThreadPoolServer.Args(trans)
                .processor(processor));
        
        // Initialize HTTP Server
        HTTPServer = new Server(ListenPort + 1);
        
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        
        FilterHolder filter = new FilterHolder();
        filter.setInitParameter("allowedOrigins", "*");
        filter.setFilter(new CrossOriginFilter());
        handler.addFilter(filter, "/*", null);
        
        HTTPServer.setHandler(handler);
        TServlet tServlet;
        tServlet = new TServlet(processor, new TJSONProtocol.Factory());
        handler.addServlet(new ServletHolder(tServlet), "/*");
        HTTPServer.start();
        
        // Fork server to the background
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                SocketServer.serve();
            }
        });
        thr.start();
        
        // System status message
        System.out.println("Started servers:");
        System.out.println("\tSocket w/ TBinaryProtocol: " + ListenPort);
        System.out.println("\tHTTPServer w/ TJSONProtocol: " + (ListenPort + 1));
    }
    
    /**
     * Stop the servers
     * @throws Exception 
     */
    public static void stopServer() throws Exception {
        SocketServer.stop();
        HTTPServer.stop();
    }
    
    /**
     * Check if the servers are running
     * @return Whether they are running
     */
    public static boolean isRunning() {
        if (SocketServer == null || HTTPServer == null) {
            return false;
        } else {
            return SocketServer.isServing() && HTTPServer.isStarted();
        }
    }
}
