
package magpie.user.server;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.user.server.thrift.MagpieServer;
import magpie.user.server.thrift.MagpieServerHandler;
import magpie.utility.WekaUtility;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.TServerTransport;
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
 * <br>-template &lt;dataset&gt; : Give path to (serialized) Dataset that can be used
 *  to generate attributes
 * <br>-model &lt;property&gt; &lt;model&gt; : For a certain property, give the path
 * to the model to be used
 * 
 * <p>Example: java -jar Magpie.jar -server -model volume volume.obj -data data.obj
 * 
 * <p><b>Client Implementation Guide</b>
 * 
 * <p>This code uses <a href="http://thrift.apache.org/">Apache Thrift</a> to 
 * define the interface. There are several different commands available through
 * this API, which are described in the "magpie.thrift" file included with this
 * software package.
 * 
 * @author Logan Ward
 */
public class ServerLauncher {
    /** Maximum number of connections allowed */
    protected static int MaxConnections = 500;
    /** Port on which to listen */
    protected static int ListenPort = 4581;
    /** Number of execution threads */
    protected static int NThreads = 2;
    
    /**
     * Handle input passed to the server. See class documentation for format
     * @param args Input 
     * @param handler Processor used to handle server requests
     */
    public static void parseInput(String[] args, MagpieServerHandler handler) {
        int pos = 0;
        while (pos < args.length) {
            String tag = args[pos].toLowerCase();
            switch (tag) {
                case "-data":
                    handler.setTemplateDataset(Dataset.loadState(args[++pos]));
                    break;
                case "-model":
                    WekaUtility.importWekaHome();
                    String property = args[++pos];
                    System.out.print("Loading model for " + property + " from " 
			+ args[pos+1] + "...");
                    BaseModel model = BaseModel.loadState(args[++pos]);
                    handler.addModel(property, model);
					System.out.println(" Done.");
                    break;
                default:
                    throw new Error("Unknown tag: " + tag);
            }
            pos++;
        }
    }
    
    /**
     * @param args the command line arguments
     * @return Server
     * @throws java.lang.Exception
     */
    static public TServer main(String[] args) throws Exception {
        // Initialize processor handler
        MagpieServerHandler handler = new MagpieServerHandler();
        MagpieServer.Processor processor = new MagpieServer.Processor(handler);

        // Read input arguments
        parseInput(args, handler);
        
        // Initialize the server
        final TServer server;
        TServerTransport trans = new TServerSocket(ListenPort);
        server = new TThreadPoolServer(new TThreadPoolServer.Args(trans)
                .processor(processor));
        
        // Initialize HTTP Server
        final Server httpServer = new Server(ListenPort + 1);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        httpServer.setHandler(context);
        TServlet tServlet;
        tServlet = new TServlet(processor, new TJSONProtocol.Factory());
        context.addServlet(new ServletHolder(tServlet), "/*");
        httpServer.start();
        
        // Fork server to the background
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                server.serve();
            }
        });
        thr.start();
        
        return server;
    }
    
}
