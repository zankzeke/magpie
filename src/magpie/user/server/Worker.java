
package magpie.user.server;

import java.io.*;
import java.util.*;
import java.net.Socket;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.user.server.util.*;

/**
 * Object that performs the actual work. Capabilities:
 * <ol>
 * <li>Read in input from a client
 * <li>(Assemble dataset based on requested entries)
 * <li>(Run models on requested entries)
 * <li>(Return results to client in requested format)
 * </ol>
 * 
 * @author Logan Ward
 */
public class Worker implements Runnable {
    /** Connection to client */
    final private Socket Socket;
	/** Model to run */
	final private Map<String,BaseModel> Models;
	/** Template dataset */
	final private Dataset DatasetTemplate;

    /**
     * Create a new worker
     * @param socket Connection 
	 * @param models Model to be evaluated
	 * @param datasetTemplate Dataset
     */
    public Worker(Socket socket, Map<String,BaseModel> models, Dataset datasetTemplate) {
        this.Socket = socket;
		this.Models = models;
		this.DatasetTemplate = datasetTemplate;
    }

    @Override
    public void run() {
        try ( 
            BufferedReader in = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
            PrintWriter out = new PrintWriter(Socket.getOutputStream(), true);
                ) {
            
            // Read in input from client
            List<String> command = new LinkedList<>();
            do {
                String line  = in.readLine();
                if (line == null || line.equalsIgnoreCase("#done")) break;
                command.add(line);
                // System.out.println("Recieved input: " + line);
            } while (true);
			
			// Determine appropriate action
			String action;
			List<String> output = new LinkedList<>();
			if (command.isEmpty()) {
				closeMe(); 
                return; // Nothing to do
			} else {
				action = command.get(0).toLowerCase().split("\\s+")[0];			
				switch (action) {
					case "evaluate":
						output = EvaluateCommandRunner.runEvaluation(command, Models, DatasetTemplate);
                        System.out.println("Evaluated entries. Host: " +
                            Socket.getInetAddress().getCanonicalHostName());
						break;
					case "search":
						output = SearchCommandRunner.runSearch(command, Models, DatasetTemplate);
                        System.out.println("Ran search. Host: " +
                            Socket.getInetAddress().getCanonicalHostName());
						break;
					default:
						output.add("Command not recognized: " + action);	
				}
			}
			
			// Send result to client
			for (String line : output) {
				out.println(line);
			}
			closeMe();
			
        } catch (Exception | Error e) {
			System.err.println("Execution failure: " + e.getMessage());
            closeMe();
        }
    }
    
    /**
     * Handle closing the thread
     */
    public void closeMe() {
        try {
            Socket.close();
            System.out.println("Connection closed. Host: " + Socket.getInetAddress().getCanonicalHostName());
        } catch (IOException e) {
            System.err.println("Error when closing socket: " + Socket.getInetAddress().getCanonicalHostName());
        }
    }
}
