
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
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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
 * <br>property &lt;description&gt; // TBD: Short description of the property being predicted
 * <br>units &lt;path&gt; // TBD: Define the units of this model
 * <br>training &lt;description&gt; // TBD: Description of training set
 * <br>author &lt;name&gt; // TBD: Contact information for author
 * <br>citation &lt;info&gt; // TBD: Information for how to properly cite this model
 * <br>notes &lt;words&gt; // TBD: Anything else the author should know about this model
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
                    System.out.println("\tSet listen port to: " + ListenPort);
                    System.out.println("\t\tSocket port: " + ListenPort);
                    System.out.println("\t\tHTTPClient port: " + (ListenPort + 1));
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
        String line = reader.readLine();
        while (true) {
            if (line == null) {
                break;
            }
            String[] words = line.split("[ \t]");
            if (words[0].equalsIgnoreCase("entry"));
            
            // Get the name of this model
            String name = words[1];
            
            // Read in model and dataset
            line = reader.readLine();
            if (line == null) {
                throw new Exception("Format error: Missing line for model path");
            }
            BaseModel model = BaseModel.loadState(line);
            line = reader.readLine();
            if (line == null) {
                throw new Exception("Format error: Missing line for dataset path");
            }
            Dataset data = Dataset.loadState(line).emptyClone();
            
            // Create the information holder
            ModelPackage modelInfo = new ModelPackage(data, model);
            
            // Read in other stuff
            line = reader.readLine();
            while (line != null && ! line.toLowerCase().startsWith("entry ")) {
                line = reader.readLine();
            }
            
            // Add in model to handler
            Handler.addModel(name, modelInfo);
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
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        HTTPServer.setHandler(context);
        TServlet tServlet;
        tServlet = new TServlet(processor, new TJSONProtocol.Factory());
        context.addServlet(new ServletHolder(tServlet), "/*");
        HTTPServer.start();
        
        // Fork server to the background
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                SocketServer.serve();
            }
        });
        thr.start();
    }
    
    /**
     * Stop the servers
     * @throws Exception 
     */
    public static void stopServer() throws Exception {
        SocketServer.stop();
        HTTPServer.stop();
    }
}
