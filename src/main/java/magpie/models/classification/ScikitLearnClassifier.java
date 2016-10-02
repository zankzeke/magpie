package magpie.models.classification;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterOutputStream;
import magpie.data.Dataset;
import magpie.models.interfaces.ExternalModel;
import magpie.models.utility.ExternalModelUtility;
import magpie.utility.UtilityOperations;

/**
 * Uses Scikit-learn to train a classification model. User must provide the path to
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
public class ScikitLearnClassifier extends BaseClassifier implements ExternalModel {
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
    /** 
     * Whether to print output from python stdout to screen
     */
    public boolean Debug = false;

    @Override
    public void close() throws Exception {
        closeServer();
    }
    
    @Override
    public void closeServer() {
        if (serverIsRunning()) {
            ScikitServer.destroy();
        }
    }
    
    @Override
    public ScikitLearnClassifier clone() {
        ScikitLearnClassifier x = (ScikitLearnClassifier) super.clone();
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
                throw new IllegalArgumentException();
            }
            path = Options.get(0).toString();
            if (Options.size() > 1) {
                level = Integer.parseInt(Options.get(1).toString());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
        readModel(new FileInputStream(path));
        setCompressionLevel(level);
    }

    @Override
    public String printUsage() {
        return "Usage: <path to model.pkl>";
    }

    @Override
    public int getPort() {
        return Port;
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
    @Override
    public void setCompressionLevel(int level) throws Exception {
        if (level < 0 || level > 9) {
            throw new IllegalArgumentException("Compression level must be 0-9");
        }
        this.CompressionLevel = level;
    }
    
    /**
     * Read model from an input stream
     *
     * @param input Input stream providing model data
     * @throws java.io.IOException
     */
    @Override
    public void readModel(InputStream input) throws Exception {
        // Clear out old model
        ScikitModel.clear();
        
        // Read in data from input stream, and iteratively compress it
        DeflaterInputStream deflater = new DeflaterInputStream(input, 
                new Deflater(CompressionLevel));
        while (true) {
            byte[] chunk = new byte[BufferSize];
            int nRead = deflater.read(chunk);
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
     * Write model to output stream
     *
     * @param output
     * @throws java.io.IOException
     */
    @Override
    public void writeModel(OutputStream output) throws IOException {
        // Create the inflater
        InflaterOutputStream inflater = new InflaterOutputStream(output);
        
        // For each chunk, uncompress it
        for (byte[] chunk : ScikitModel) {
            inflater.write(chunk);
        }
    }

    @Override
    public void startServer() throws Exception {
        // Find the server code
        File scriptFile = UtilityOperations.findFile("py/scikit_server.py");
        
        // Start the process
        ScikitServer = new ProcessBuilder("python", 
                scriptFile.getCanonicalPath())
                .start();
        
        // Launch the server
        Port = ExternalModelUtility.initializeServer(this, Debug);
    }

    @Override
    public boolean serverIsRunning() {
        Process process = getProcess();
        return ExternalModelUtility.isRunning(process);
    }

    @Override
    public Process getProcess() {
        return ScikitServer;
    }

    @Override
    protected void train_protected(Dataset TrainData) {        
        try {
            // Connect to server and call the subprocess
            ExternalModelUtility.trainModel(this, TrainData);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } 
    }
    
    @Override
    public void run_protected(Dataset runData) {
        try {
            ExternalModelUtility.runModel(this, runData);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    @Override
    protected String printModel_protected() {
        return String.format("Scikit learn model");
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        // Get the output
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        List<String> linesToAdd = ExternalModelUtility.getModelDescription(this);
        
        output.addAll(linesToAdd);
        return output;
    }
}
