package magpie.models.regression;

import java.io.*;
import java.io.PrintStream;
import java.nio.file.*;
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
 * <p><b>Note:</b> This implementation requires the ability to write temporary files
 * on whatever system Magpie is running on.
 * 
 * <usage><p><b>Usage</b>: &lt;model&gt;
 * <br><pr><i>model</i>: Path to a file containing a Scikit-Learn regression object
 * <br>See <a href="http://scikit-learn.org/dev/tutorial/basic/tutorial.html#model-persistence">
 * this tutorial</a> for how to save Scikit-Learn objects.
 * </usage>
 * @author Logan Ward
 */
public class ScikitLearnRegression extends BaseRegression {
    /** Scikit-learn model. This is just the the pickle file as a byte array */
    private byte[] ScikitModel;

    @Override
    public ScikitLearnRegression clone() {
        ScikitLearnRegression x = (ScikitLearnRegression) super.clone();
        x.ScikitModel = ScikitModel.clone();
        return x;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String path;
        try {
            if (Options.size() != 1) {
                throw new Exception();
            }
            path = Options.get(0).toString();
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        readModel(path);
    }

    @Override
    public String printUsage() {
        return "Usage: <path to model.pkl>";
    }
    
    /**
     * Read model from disk.
     * @param path Path to a pickle file describing the model
     * @throws java.io.IOException
     */
    public void readModel(String path) throws IOException {
        ScikitModel = Files.readAllBytes(Paths.get(path));
    }
    
    /**
     * Write stored model to disk.
     * @param path Path to desired output file
     * @throws java.io.IOException
     */
    public void writeModel(String path) throws IOException {
        Files.write(Paths.get(path), ScikitModel, StandardOpenOption.WRITE);
    }
    
    /**
     * Check if the python process is still running
     * @param py Python process
     * @return Whether it is still running
     */
    private boolean processIsRunning(Process py) {
        try {
            py.exitValue();
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        // Create a temporary file to which to output the model
        File modelIn, modelOut;
        String modelInPath, modelOutPath;
        try {
            modelIn = File.createTempFile(Long.toHexString(System.currentTimeMillis()), null);
            modelOut = File.createTempFile(Long.toHexString(System.currentTimeMillis()+1), null);
            modelInPath = modelIn.getCanonicalPath();
            modelOutPath = modelOut.getCanonicalPath();
            writeModel(modelIn.getCanonicalPath());
            modelIn.deleteOnExit();
            modelOut.deleteOnExit();
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Create the program to run the scikit learn model
        String runID = Double.toHexString(Math.random());
        PrintStream scriptWriter;
        File scriptFile;
        try {
            scriptFile = File.createTempFile("script" + runID, null);
            scriptFile.deleteOnExit();
            scriptWriter = new PrintStream(scriptFile);
        } catch (Exception e) {
            throw new Error(e);
        }
        
        //  Load in the model
        scriptWriter.println("from sklearn.externals import joblib");
        scriptWriter.format("model = joblib.load(r\"%s\")\n", modelInPath);
        
        // Read in the data
        scriptWriter.println("import sys");
        scriptWriter.println("X = []");
        scriptWriter.println("y = []");
        scriptWriter.println("for line in sys.stdin:");
        scriptWriter.println(" row = [ float(n) for n in line.split() ]");
        scriptWriter.println(" X.append(row[1:])");
        scriptWriter.println(" y.append(row[0])");
        
        // Fit the model and save it to disc
        scriptWriter.println("model.fit(X,y)");
        scriptWriter.format("joblib.dump(model, r\"%s\")\n", modelOutPath);
        scriptWriter.println("print \"Done\"");
        scriptWriter.close();
        
        // Launch the Python code
        Process python;
        PrintStream pythonInput; // Use to write to Python program
        BufferedReader pythonError; // Use to read Python error output
        try {
            // Launch a python thread 
            ProcessBuilder pb = new ProcessBuilder("python", scriptFile.getCanonicalPath());
            python = pb.start();
            if (! processIsRunning(python)) {
                throw new Exception("System doesn't have python installed");
            }
            pythonInput = new PrintStream(new BufferedOutputStream(python.getOutputStream()));
            pythonError = new BufferedReader(new InputStreamReader(python.getErrorStream()));
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Write the data to Python script's stdin
        for (BaseEntry entry : TrainData.getEntries()) {
            pythonInput.print(Double.toString(entry.getMeasuredClass()));
            for (double a : entry.getAttributes()) {
                pythonInput.print(" " + Double.toString(a));
            }
            pythonInput.println();
        }
        
        // Close the input, signalling no more data will be sent
        pythonInput.close();
        
        // Read the model back in
        try {
            // Wait until Process closes
            python.waitFor();
            readModel(modelOutPath);
            if (ScikitModel.length == 0) {
                throw new Exception();
            }
        } catch (Exception e) {
            System.err.println("Error from scikit-learn script:");
            try {
                String line = pythonError.readLine();
                while (line != null) {
                    System.err.println("\t" + line);
                    line = pythonError.readLine();
                }
            } catch (IOException e2) {
                throw new Error(e2);
            }
            throw new Error(e);
        }
    }

    @Override
    public void run_protected(Dataset TrainData) {
        // Create a temporary file to which to output the model
        File modelFile;
        String modelFilePath;
        try {
            modelFile = File.createTempFile(Long.toHexString(System.currentTimeMillis()), null);
            modelFilePath = modelFile.getCanonicalPath();
            modelFile.deleteOnExit();
            writeModel(modelFile.getCanonicalPath());
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Create the program to run the scikit learn model
        String runID = Double.toHexString(Math.random());
        PrintStream scriptWriter;
        File scriptFile;
        try {
            scriptFile = File.createTempFile("script" + runID, null);
            scriptFile.deleteOnExit();
            scriptWriter = new PrintStream(scriptFile);
        } catch (Exception e) {
            throw new Error(e);
        }
        
        //  Load in the model
        scriptWriter.println("from sklearn.externals import joblib");
        scriptWriter.format("model = joblib.load(r\"%s\")\n", modelFilePath);
        
        // Read in the data
        scriptWriter.println("import sys");
        scriptWriter.println("X = []");
        scriptWriter.println("for line in sys.stdin:");
        scriptWriter.println(" row = [ float(n) for n in line.split() ]");
        scriptWriter.println(" X.append(row)");
        
        // Run the model, print out results
        scriptWriter.println("y = model.predict(X)");
        scriptWriter.println("for val in y: print val"); 
        
        // Launch the Python code
        Process python;
        PrintStream pythonInput; // Use to write to Python program
        BufferedReader pythonOutput; // Use to read Python output
        BufferedReader pythonError; // Use to read Python error output
        try {
            // Launch a python thread 
            ProcessBuilder pb = new ProcessBuilder("python", scriptFile.getCanonicalPath());
            python = pb.start();
            if (! processIsRunning(python)) {
                throw new Exception("System doesn't have python installed");
            }
            pythonInput = new PrintStream(new BufferedOutputStream(python.getOutputStream()));
            pythonOutput = new BufferedReader(new InputStreamReader(python.getInputStream()));
            pythonError = new BufferedReader(new InputStreamReader(python.getErrorStream()));
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Write the data to Python script's stdin
        for (BaseEntry entry : TrainData.getEntries()) {
            for (double a : entry.getAttributes()) {
                pythonInput.print(" " + Double.toString(a));
            }
            pythonInput.println();
        }
        
        // Close the input, signalling no more data will be sent
        pythonInput.close();
        
        // Read the results
        for (int i=0; i<TrainData.NEntries(); i++) {
            try {
                String res = pythonOutput.readLine();
                double y = Double.parseDouble(res);
                TrainData.getEntry(i).setPredictedClass(y);
            } catch (Exception e) {
                System.err.println("Error from scikit-learn script:");
                try {
                    String line = pythonError.readLine();
                    while (line != null) {
                        System.err.println("\t" + line);
                        line = pythonError.readLine();
                    }
                } catch (IOException e2) {
                    throw new Error(e2);
                }
                throw new Error(e);
            }
        }
    }

    @Override
    public int getNFittingParameters() {
        return 0;
    }

    @Override
    protected String printModel_protected() {
        return String.format("%.3f MB Scikit learn model", (double) ScikitModel.length / 1e6);
    }
}
