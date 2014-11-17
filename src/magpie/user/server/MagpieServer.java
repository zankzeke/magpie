/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.user.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import magpie.data.Dataset;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.models.BaseModel;
import magpie.optimization.rankers.AdaptiveScalarizingEntryRanker;
import magpie.optimization.rankers.PropertyFormulaRanker;

/**
 * Main class for launching a Magpie server. 
 * 
 * <p><b>How to start server</b>
 * 
 * <p>Server can be started simply by calling java -jar /path/to/MagpieServer.jar
 * 
 * <p>This jar file takes several command-line arguments;
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
 * <p>This server is designed to receive text commands regarding what input to provide
 * a model, perform the specified operation, and return the results as plain text.
 * As such, a client needs to prepare correctly-formatted commands and process the results.
 * 
 * <p>There are currently a few different commands, which have slightly different syntax:
 * 
 * <p><i>Evaluating Batches of Entries</i>
 * 
 * <p>One capability of the MagpieServer is to read in a batch of entries and make predictions
 * about at least one property of those entries. The format for this entry is as follows:
 * 
 *  <p>evaluate &lt;property names...&gt;
 * <br>&lt;entry #1&gt;
 * <br>&lt;entry #2&gt;
 * <br>&lt;...&gt;
 * <br>#done
 * 
 * <p>The first line species that the client wants to evaluate many entries, and what 
 * properties to predict.
 * 
 * <p>The subsequent lines are simply text strings that describe each entry (i.e. the composition 
 * of the material). Avoid tabs in the names of entries because the output file
 * will be tab-delimited. The last line <b>must</b> be "#done" which signals to MagpieServer
 * to stop parsing entries and to compute the result.
 * 
 * <p>This command returns a tab-delimited file where the first column is the entry
 * name after being parsed (i.e. for an entry "Na,1,Cl,1,", this will be 
 * NaCl). The following columns are the predicted value for the request properties.
 * 
 * <p><i>Searching for Optimal Entries</i>
 * 
 * <p>This server is also equipped to generate a large batch of entries and searching
 * for the ones that best match a certain criterion. The input text for this 
 * follows the format:
 * 
 *  <p>search &lt;EntryGenerator method&gt; &lt;generator options%lt;
 * <br>[Description of the criteria[
 * <br>number &lt;number of top entries to return&gt;
 * <br>#done
 * 
 * <p>The first line of this file defines the space over which the server is to
 * search. This is accomplished by first providing the name of a 
 * {@linkplain BaseEntryGenerator} and then any options for that generator.
 * 
 * <p>Next, it is necessary to define the objective function. There are multiple 
 * kinds of objective functions available:
 * 
 * <ul>
 * <li><b>Single Objective</b> - Find entries that optimize
 * a single objective function. This method only takes one input line to define, which
 * must be in the following format: 
 * <br>objective &lt;property name&lt; 
 * &lt;minimize|maximize|target|class&gt; [&lt;target value|class name&gt;]
 * <li><b>Multi-Objective: Scalarizing</b> - <i>Not yet implemented</i> Find entries 
 * that fit multiple goals simultaneously by combining multiple objectives using a 
 * {@linkplain AdaptiveScalarizingEntryRanker}. To define this kind of search,
 * the second line in the command must be "multi &lt;p&gt;", where "p" is the 
 * tradeoff parameter in the scalaraizing function. The following lines follow
 * the same format as the single objective.
 * <li><b>Multi-Objective: Formula</b> - <i>Not yet implemented</i> Find entries
 * that match a certain objective function that requires prediction of multiple
 * properties (i.e. specific stiffness: <i>E</i>/<i>&rho;&gt;</i>). To do this,
 * the first line must be "formula &lt;function&gt;", where "function" follows the
 * syntax used in {@linkplain PropertyFormulaRanker}. No other input is required
 * </ul>
 * 
 * <p>The final line of input for the search function is simply the number of entries
 * that will be returned. The results will be returned in the same format as 
 * the simple entry evaluator, but with an extra column to store the objective function
 * result for multi-objective searches.
 * 
 * @author Logan Ward
 */
public class MagpieServer {
    /** Maximum number of connections allowed */
    protected static int MaxConnections = 500;
    /** Port on which to listen */
    protected static int ListenPort = 4581;
    /** Number of execution threads */
    protected static int NThreads = 2;
    /** Debugging purposes: Number of connections to accept before quitting (-1 to turn off) */
    protected static int NToAccept = -1;
    /** Dataset template (used to calculate attributes) */
    protected static Dataset TemplateDataset = null;
    /** Model to be run */
	protected static Map<String,BaseModel> Models = new TreeMap<>();
	/** Maximum number of entries to evaluate (security measure) */
	public static long MaxEntryEvaluations = 50000;
    
    /**
     * Handle input passed to the server. See class documentation for format
     * @param args Input 
     */
    public static void parseInput(String[] args) {
        int pos = 0;
        while (pos < args.length) {
            String tag = args[pos].toLowerCase();
            switch (tag) {
                case "-data":
                    MagpieServer.TemplateDataset = Dataset.loadState(args[++pos]);
                    break;
                case "-model":
                    String property = args[++pos];
					System.out.print("Loading model for " + property + " from " 
							+ args[pos+1] + "...");
                    BaseModel model = BaseModel.loadState(args[++pos]);
                    MagpieServer.Models.put(property, model);
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
     */
    public static void main(String[] args) {
        // Read input arguments
        parseInput(args);
        
        // Check that everything it set up
		if (MagpieServer.TemplateDataset == null) {
            throw new Error("Dataset not set");
        }
        if (MagpieServer.Models.isEmpty()) {
            throw new Error("No models set");
        }
        
        // Initialize the server
        ServerSocket server;
        try {
            server = new ServerSocket(MagpieServer.ListenPort);
            server.setSoTimeout(0);
        } catch (IOException e) {
            throw new Error("I/O Error when starting server: " + e.getMessage());
        }
       
        // Act as a server
        int currentlyConnected = 0;
        long numberAccepted = 0;
        ExecutorService threadEngine = Executors.newFixedThreadPool(NThreads);
        while (currentlyConnected <= MaxConnections 
                && numberAccepted != MagpieServer.NToAccept) {
            // Wait for a connection
            Socket connection;
            try {
                connection = server.accept();
            } catch (IOException e) {
                System.err.println("Problem accepting connection: " + e.getMessage());
                continue;
            }
            numberAccepted++;
            
            // Pass it off to the thread pool
            System.out.println("New connection. Host: " + connection.getInetAddress().getCanonicalHostName());
            Worker newWorker = new Worker(connection, Models, TemplateDataset);
            threadEngine.submit(newWorker);
        }
        
        // Wait until threads finish
        threadEngine.shutdown();
        
        // We're done, close up shop
        try {
            server.close();
        } catch (IOException e) {
            throw new Error("Error when closing server: " + e.getMessage());
        }
    }
    
}
