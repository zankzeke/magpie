package magpie.models.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.classification.AbstractClassifier;
import magpie.models.interfaces.ExternalModel;

/**
 * Utility operation for models that rely on an external (i.e., non-java) process
 * and use a socket connection to exchage information.
 * 
 * <p>Assumes that...
 * 
 * <ol>
 * <li>The process reads the model state from standard in</li>
 * <li>For each connection, the server takes the command for what to do as a plaintext
 * word in the first line. Commands can be:
 *  <ul>
 *      <li>"train" - Which is followed by the number of entries, and a line
 * for each entry where the first number is the class variable the remaining numbers
 * are the attributes. Once training is complete, the server responds with the 
 * model state as a data stream.
 *      <li>"run" - Which is followed by the number of entries, and a line 
 * for each entry with the attributes printed out. The server responds with
 * the same number of lines of output, which contain either probabilities of
 * membership in each class [classification] or predicted class [regression]
 *      <li>"type" - Followed by no other data. The server responds with 
 * a description of model as plain text.
 *      <li>"exit" - Stop the server
 *  </ul>
 * </li>
 * </ol>
 * 
 * @author Logan Ward
 */
public class ExternalModelUtility {
    
    /**
     * Launch a thread that does nothing but read from the output of another process.
     * 
     * <p>Optionally, it can also write this data to another stream
     * 
     * @param processStream Stream to be read from
     * @param outputStream Stream to be written to. Put "null" for no output
     */
    static public void startReader(final InputStream processStream, 
            final OutputStream outputStream) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int readBtye = processStream.read();
                        if (readBtye == -1) {
                            return;
                        }
                        if (outputStream != null) {
                            outputStream.write(readBtye);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    /**
     * Check whether a process is running
     * @param process Process
     * @return Whether it is running
     */
    public static boolean isRunning(Process process) {
        if (process == null) {
            return false;
        }
        try {
            process.exitValue();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Assuming that a model server is already launched, get the port number
     * and send the model object stream to the standard input of the server process.
     *
     * @param model Model being operated on
     * @param debug Whether to print standard out, or ignore it
     * @return Port number
     * @throws Exception
     */
    public static int initializeServer(final ExternalModel model,
            boolean debug) throws Exception {
        // Launch the process
        Process server = model.getProcess();

        // Make sure to pipe stderr from subprocess to stderr of Magpie
        ExternalModelUtility.startReader(server.getErrorStream(), System.err);

        // Send the model to the subprocess
        model.writeModel(server.getOutputStream());
        server.getOutputStream().close();

        // Get the port number
        int port;
        try {
            BufferedReader fo = new BufferedReader(new InputStreamReader(server.getInputStream()));
            String[] words = fo.readLine().split(" ");
            port = Integer.parseInt(words[words.length - 1]);
        } catch (Exception e) {
            throw new Exception("Server failed to start. Check the subprocess stderr, which should have printed");
        }

        // If in debug mode, write the subprocess stdout to Magpie stdout
        ExternalModelUtility.startReader(server.getInputStream(), debug ? System.out : null);

        // Make sure the subprocess is killed when Magpie shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                model.closeServer();
            }
        }));
        return port;
    }

    /**
     * Make a call to the model server to train a model
     *
     * @param model Model object being trained
     * @param trainData Data used to train model
     * @throws IOException
     * @throws Exception
     * @throws IllegalArgumentException
     */
    public static void trainModel(ExternalModel model, Dataset trainData) throws Exception {
        // Make sure the server is running
        if (! model.serverIsRunning()) {
            model.startServer();
        }
        
        // Open the connection
        Socket socket;
        PrintWriter fo;
        socket = new Socket("localhost", model.getPort());
        fo = new PrintWriter(socket.getOutputStream());
        
        // Tell the server we want to train the model
        fo.println("train");
        
        // Send the training data
        fo.println(trainData.NEntries());
        for (BaseEntry e : trainData.getEntries()) {
            fo.print(e.getMeasuredClass());
            for (int a = 0; a < trainData.NAttributes(); a++) {
                fo.print(" ");
                fo.print(e.getAttribute(a));
            }
            fo.println();
        }
        fo.flush();
        
        // Read the train model back
        model.readModel(socket.getInputStream());
        if (!model.serverIsRunning()) {
            throw new RuntimeException("Model server crashed");
        }
        
        // Done!
        fo.close();
        socket.close();
    }

    /**
     * Connect to server and run model
     * @param model Model to be run
     * @param runData Dataset to be run
     * @throws Exception
     */
    static public void runModel(ExternalModel model, Dataset runData) throws Exception {
        // Check that the model is running
        Socket socket;
        if (!model.serverIsRunning()) {
            model.startServer();
        }
        
        // Connect to the server
        socket = new Socket("localhost", model.getPort());
        PrintWriter fo = new PrintWriter(socket.getOutputStream());
        
        // Tell it we want to run the model
        fo.println("run");
        
        // Send the data to be run
        fo.println(runData.NEntries());
        for (BaseEntry e : runData.getEntries()) {
            for (int a = 0; a < runData.NAttributes(); a++) {
                fo.print(" ");
                fo.print(e.getAttribute(a));
            }
            fo.println();
        }
        fo.flush();
        
        // Read the result
        BufferedReader fi = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        boolean isClassifier = model instanceof AbstractClassifier;
        for (BaseEntry e : runData.getEntries()) {
            try {
                if (isClassifier) {
                    String[] probStrs = fi.readLine().split(" ");
                    double[] probs = new double[probStrs.length];
                    for (int c = 0; c < probStrs.length; c++) {
                        probs[c] = Double.parseDouble(probStrs[c]);
                    }
                    e.setClassProbabilities(probs);
                } else {
                    double cls = Double.parseDouble(fi.readLine());
                    e.setPredictedClass(cls);
                }
            } catch (NumberFormatException ex) {
                // Skip
            }
        }
        
        // Done!
        fo.close();
        socket.close();
    }

    /**
     * Generate a description of the model
     * @param model Model to be accessed
     * @return List of lines of plain text describing the model settings
     * @throws Error
     */
    public static List<String> getModelDescription(ExternalModel model) throws Error {
        List<String> linesToAdd = new ArrayList<>();
        
        // Launch server, if needed
        if (!model.serverIsRunning()) {
            try {
                model.startServer();
            } catch (Exception e) {
                throw new Error(e);
            }
        }
       
        // Connect to server
        try (final Socket socket = new Socket("localhost", model.getPort())) {
            // Tell it we want a description
            PrintWriter fo = new PrintWriter(socket.getOutputStream());
            fo.println("type");
            fo.flush();
            
            // Get the description
            BufferedReader fi = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = fi.readLine();
            while (line != null) {
                linesToAdd.add(line);
                line = fi.readLine();
            }
            fo.close();
            fi.close();
        } catch (Exception e) {
            throw new Error(e);
        }
        return linesToAdd;
    }
    
    /**
     * Send the "exit" command and stop a server
     * @param model Model server to be shut down
     */
    static public void stopServer(ExternalModel model) {
        // Do nothing if model not running
        if (! model.serverIsRunning()) {
            return;
        }
        
        // Send the shutdown command
        try (
            final Socket socket = new Socket("localhost", model.getPort())
        ) {
            PrintWriter fp = new PrintWriter(socket.getOutputStream());
            fp.println("exit");
            fp.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
