package magpie.models.regression;

import java.io.*;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Uses Scikit-learn to train a regression model. User must provide the path to
 * a pickle file containing a Python object that fulfills two operations:
 *
 * <ol>
 * <li><b><code>fit(X, y)</code></b>: Train the model given attribute matrix X,
 * and observation matrix y.
 * <li><b><code>predict(X)</code></b>: Run the model
 * </ol>
 *
 * <p>
 * <b>Note:</b> This implementation requires the ability to write temporary
 * files on whatever system Magpie is running on.
 *
 * <usage><p>
 * <b>Usage</b>: &lt;model&gt; [&lt;compression level&gt;]
 * <br><pr><i>model</i>: Path to a file containing a Scikit-Learn regression
 * object
 * <br><pr><i>compression</i>: Degree to which model is compressed before 
 * storing in Magpie. 0: fastest, 9: lowest memory footprint (default: 5)
 * <br>See
 * <a href="http://scikit-learn.org/dev/tutorial/basic/tutorial.html#model-persistence">
 * this tutorial</a> for how to save Scikit-Learn objects.
 * </usage>
 *
 * @author Logan Ward
 */
public class ScikitLearnRegression extends BaseRegression {
    /**
     * Scikit-learn model. This is just the the pickle file as a char array that
     * has been split into chunks
     */
    private List<byte[]> ScikitModel = new ArrayList<>();
    /** 
     * Buffer size when reading in files / from sockets.
     */
    private final int BufferSize = 1024 * 1024 * 16;
    /**
     * Scikit server process
     */
    protected transient Process ScikitServer;
    /**
     * Port on which server communicates
     */
    private transient int Port;
    /**
     * Compression level on ScikitLearn model
     */
    private int CompressionLevel = 5;

    @Override
    protected void finalize() throws Throwable {
        ScikitServer.destroy();
        super.finalize();
    }

    @Override
    public ScikitLearnRegression clone() {
        ScikitLearnRegression x = (ScikitLearnRegression) super.clone();
        x.ScikitModel = new LinkedList<>(ScikitModel);
        x.ScikitServer = null;
        x.Port = Integer.MIN_VALUE;
        return x;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String path;
        int level = 5;
        try {
            if (Options.size() > 2) {
                throw new Exception();
            }
            path = Options.get(0).toString();
            if (Options.size() > 1) {
                level = Integer.parseInt(Options.get(1).toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        readModel(new FileInputStream(path));
        setCompressionLevel(level);
    }

    @Override
    public String printUsage() {
        return "Usage: <path to model.pkl>";
    }

    /**
     * Define how well model is compressed after training.
     * 
     * <p>This class works by launching a server than runs a scikit-learn model.
     * After training, this server sends back the model as a pickle file. For
     * large datasets, this could be a huge file. This option allows one to compress
     * it before transmission.
     * 
     * @param level Desired level. 1: Fastest, 9: Smallest memory footprint
     * @throws Exception 
     * @see #ScikitModel
     */
    public void setCompressionLevel(int level) throws Exception {
        if (level < 0 || level > 9) {
            throw new Exception("Compression level must be 0-9");
        }
        this.CompressionLevel = level;
    }
    
    

    /**
     * Read model from an input stream
     *
     * @param input Input stream providing model data
     * @throws java.io.IOException
     */
    public void readModel(InputStream input) throws Exception {
        // Clear out old model
        ScikitModel.clear();
        
        // Create the byte reader
        BufferedInputStream fp = new BufferedInputStream(input);
        while (true) {
            byte[] chunk = new byte[BufferSize];
            int nRead = fp.read(chunk);
            if (nRead == chunk.length) {
                ScikitModel.add(chunk);
            } else if (nRead > 0) {
                ScikitModel.add(Arrays.copyOf(chunk, nRead));
            } else {
                break;
            }
        }
    }

    /**
     * Write stored model to disk.
     *
     * @param path Path to desired output file
     * @throws java.io.IOException
     */
    public void writeModel(String path) throws IOException {
        BufferedOutputStream fo = new BufferedOutputStream(new FileOutputStream(path));
        for (byte[] chunk : ScikitModel) {
            fo.write(chunk);
        }
        fo.close();
    }

    /**
     * Start the server hosting the Scikit model. This process will initialize
     * the server and get the communication port number.
     *
     * @throws java.lang.Exception
     */
    protected void startScikitServer() throws Exception {
        String script = "import pickle\n"
                + "from sys import argv\n"
                + "import socket\n"
                + "import array\n"
                + "import gzip\n"
                + "import binascii\n"
                + "from sys import stdout\n"
                + "\n" 
                + "startPort = 5482; # First port to check\n"
                + "endPort = 5582; # Last port to check\n"
                + "try:\n"
                + " fp = gzip.open(argv[1], 'rb')\n"
                + " model = pickle.load(fp)\n"
                + " fp.close()\n"
                + "except:\n"
                + " fp = open(argv[1], 'rb')\n"
                + " model = pickle.load(fp)\n"
                + " fp.close()\n"
                + "\n"
                + "port = startPort;\n"
                + "ss = socket.socket(socket.AF_INET, socket.SOCK_STREAM)\n"
                + "while port <= endPort:\n"
                + "	try:\n"
                + "		ss.bind(('localhost', port))\n"
                + "	except:\n"
                + "		port = port + 1\n"
                + "		continue\n"
                + "	break\n"
                + "ss.listen(0) # Only allow one connection\n"
                + "print \"Listening on port\", port\n"
                + "stdout.flush();\n"
                + "\n"
                + "def trainModel(fi, fo):\n"
                + "	nRows = int(fi.readline())\n"
                + "	X = []\n"
                + "	y = []\n"
                + "	for i in range(nRows):\n"
                + "		line = fi.readline()\n"
                + "		x = array.array('d')\n"
                + "		temp = [ float(w) for w in line.split() ]\n"
                + "		x.fromlist(temp[1:])\n"
                + "		X.append(x)\n"
                + "		y.append(temp[0])\n"
                + "	model.fit(X,y)\n"
                + "	\n"
				+ "	gzo = gzip.GzipFile(fileobj = fo, mode='wb', compresslevel=" + CompressionLevel + " )\n"
                + "	pickle.dump(model, gzo)\n"
                + "\n"
                + "def runModel(fi, fo):\n"
                + "	\n"
                + "	# Recieve\n"
                + "	nRows = int(fi.readline())\n"
                + "	X = []\n"
                + "	y = []\n"
                + "	for i in range(nRows):\n"
                + "		line = fi.readline()\n"
                + "		x = array.array('d')\n"
                + "		temp = [ float(w) for w in line.split() ]\n"
                + "		x.fromlist(temp)\n"
                + "		X.append(x)\n"
                + "	\n"
                + "	# Compute\n"
                + "	y = model.predict(X)\n"
                + "	\n"
                + "	# Send back\n"
                + "	for yi in y:\n"
                + "		print >>fo, yi\n"
                + "\n"
                + "while 1:\n"
                + " (client, address) = ss.accept()\n"
                + " \n"
                + " fi = client.makefile('r')\n"
                + " fo = client.makefile('w')\n"
                + " command = fi.readline()\n"
                + " if \"train\" in command:\n"
                + "     trainModel(fi, fo)\n"
                + " elif \"run\" in command:\n"
                + "     runModel(fi, fo)\n"
                + " elif \"type\" in command:\n" 
                + "     print >>fo, model\n"
                + " fi.close()\n"
                + " fo.close()\n"
                + "		\n"
                + "	# Close the client\n"
                + " client.close()";
        
        // Write out the script file
        File scriptFile = File.createTempFile("scikit", "py");
        scriptFile.deleteOnExit();
        PrintWriter fp = new PrintWriter(scriptFile);
        fp.println(script);
        fp.close();
        
        // Write out the model
        File modelFile = File.createTempFile("scikit", "pkl");
        modelFile.deleteOnExit();
        writeModel(modelFile.getCanonicalPath());
        
        // Start the process
        ScikitServer = new ProcessBuilder("python", 
                scriptFile.getCanonicalPath(), 
                modelFile.getCanonicalPath()).start();
        
        // Get the port number
        BufferedReader fo = new BufferedReader(
                new InputStreamReader(ScikitServer.getInputStream()));
        String[] words = fo.readLine().split(" ");
        Port = Integer.parseInt(words[3]);
        
        // Check if file is running
        if (! serverIsRunning()) {
            BufferedReader fe = new BufferedReader(
                    new InputStreamReader(ScikitServer.getErrorStream()));
            String line = fe.readLine();
            while (line != null) {
                System.err.println(line);
                line = fe.readLine();
            }
            throw new Exception("Server failed to start");
        }
        
        // Make sure the process is killed
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ScikitServer.destroy();
            }
        }));
    }

    /**
     * Check if the python process is still running
     *
     * @return Whether it is still running
     */
    private boolean serverIsRunning() {
        if (ScikitServer == null) {
            return false;
        }
        try {
            ScikitServer.exitValue();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        PrintWriter fo = null;
        Socket socket = null;
        
        try {
            if (! serverIsRunning()) {
                startScikitServer();
            }
            
            // Connect to server
            socket = new Socket("localhost", Port);
            
            // Write to the socket that we want to train
            fo = new PrintWriter(socket.getOutputStream());
            fo.println("train");
            
            // Write out the data
            fo.println(TrainData.NEntries());
            for (BaseEntry e : TrainData.getEntries()) {
                fo.print(e.getMeasuredClass());
                for (int a=0; a<TrainData.NAttributes(); a++) {
                    fo.print(" ");
                    fo.print(e.getAttribute(a));
                }
                fo.println();
            }
            fo.flush();
            
            // Recieve the model
            readModel(socket.getInputStream());

            // Check if server is still running
            if (! serverIsRunning()) {
                throw new Exception();
            }
            
            socket.close();
        } catch (Exception ex) {
            // Read python error out
                // Do nothing
            try {
                BufferedReader er = new BufferedReader(
                        new InputStreamReader(ScikitServer.getErrorStream()));
                String line = er.readLine();
                while (line != null) {
                    System.err.println(line);
                    line = er.readLine();
                }
            } catch (Exception e) {
            }
            throw new Error(ex);
        } 
    }

    @Override
    public void run_protected(Dataset TrainData) {
        PrintWriter fo = null;
        Socket socket = null;
        
        try {
            if (! serverIsRunning()) {
                startScikitServer();
            }
            
            // Connect to server
            socket = new Socket("localhost", Port);
            
            // Write to the socket that we want to run
            fo = new PrintWriter(socket.getOutputStream());
            fo.println("run");
            
            // Write out the data
            fo.println(TrainData.NEntries());
            for (BaseEntry e : TrainData.getEntries()) {
                for (int a=0; a<TrainData.NAttributes(); a++) {
                    fo.print(" ");
                    fo.print(e.getAttribute(a));
                }
                fo.println();
            }
            fo.flush();
            
            // Recieve the results
            BufferedReader fi = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            for (BaseEntry e : TrainData.getEntries()) {
                double x;
                try {
                    x = Double.parseDouble(fi.readLine());
                    e.setPredictedClass(x);
                } catch (NumberFormatException ex) {
                    // Skip
                }
            }
            
            // Done!
            socket.close();
        } catch (Exception ex) {
            try {
                BufferedReader er = new BufferedReader(
                        new InputStreamReader(ScikitServer.getErrorStream()));
                String line = er.readLine();
                while (line != null) {
                    System.err.println(line);
                    line = er.readLine();
                }
            } catch (Exception e) {
            }
            throw new Error(ex);
        } finally {
            fo.close();
        }
    }

    @Override
    public int getNFittingParameters() {
        return 0;
    }

    @Override
    protected String printModel_protected() {
        return String.format("Scikit learn model");
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        if (! serverIsRunning()) {
            try {
                startScikitServer();
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        
        // Get the output
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        // Open up socket to server
        try {
            Socket socket = new Socket("localhost", Port);
            
            // Indicate that we want type info
            PrintWriter fo = new PrintWriter(socket.getOutputStream());
            fo.println("type");
            fo.flush();
            
            // Get the model info
            BufferedReader fi = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            String line = fi.readLine();
            while (line != null) {
                output.add(line);
                line = fi.readLine();
            }
            
            // Close the socket
            socket.close();
        } catch (Exception e) {
            throw new Error(e);
        }
        
        return output;
    }
}
