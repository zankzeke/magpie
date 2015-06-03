
package magpie.models.regression;


import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;

/**
 * Regression using a Multilayer Perceptron. Uses the {@linkplain MultilayerPerceptron}
 *  from Weka, which has sigmoid transfer functions.
 * 
 * <usage><p><b>Usage</b>: [-val &lt;val>] [-nstall &lt;number>] [&lt;# in hidden layers...>]
 * <br><pr><i>val</i>: Percentage of entries to be used in validation set. (0-99. default:15)
 * <br><pr><i>number</i>: Stop training after error on validation set increases for this many consecutive epochs (default: 3)
 * <br><pr><i># in hidden layers</i>: List of numbers of nodes in each hidden layer</usage>
 * @author Logan Ward
 */
public class PerceptronRegression extends BaseRegression {
    /** Neural network used for this class */
    protected MultilayerPerceptron network;
    /** Number of nodes in each hidden layer */
    protected List<Integer> hiddenLayers;
    /** Validation set percentage */
    protected int ValSize = 15;
    /** Number of epochs where error in validation set can increase before stopping. */
    protected int NStall = 3;

    public PerceptronRegression() {
        hiddenLayers = new LinkedList<>();
    }

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            int pos = 0;
            if (Options[pos].equalsIgnoreCase("-val")) {
                setValidationSetSize(Integer.parseInt(Options[++pos]));
                pos++;
            }
            if (Options[pos].equalsIgnoreCase("-nstall")) {
                setEpochsBeforeStall(Integer.parseInt(Options[++pos]));
                pos++;
            }
            hiddenLayers.clear();
            for (; pos<Options.length; pos++) {
                hiddenLayers.add(Integer.parseInt(Options[pos]));
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            throw new Exception(printUsage());
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        
    }

    /**
     * Define the number of neurons in each hidden layer.
     * @param hiddenLayers List of number of neurons
     */
    public void setHiddenLayers(List<Integer> hiddenLayers) {
        this.hiddenLayers.clear();
        this.hiddenLayers.addAll(hiddenLayers);
    }
    
    /**
     * Set the size of validation set used when training the network.
     * @param percentage Percentage between 0 and 99
     */
    public void setValidationSetSize(int percentage) {
        if (percentage < 0 || percentage >= 99)
            throw new Error("Percentage must be between 0 and 99");
        ValSize = percentage;
    }
    
    /**
     * Define the number of consecutive epochs where the error in the validation
     *  set increases before training halts.
     * @param nEpochs Number of epochs before stall
     */
    public void setEpochsBeforeStall(int nEpochs) {
        NStall = nEpochs;
    }
    
    @Override
    public String printUsage() {
        return "Usage: [-val <percentage>] [-nstall <number>] [<# in hidden layers...>]";
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        // Step 1: Instantiate network
        network = new MultilayerPerceptron();
        network.setValidationSetSize(ValSize);
        network.setValidationThreshold(NStall);
        
        // Step 2: Define the number of inner nodes
        List<Integer> nodeCount = new LinkedList<>(hiddenLayers);
        if (nodeCount.size() > 0) {
            String h = nodeCount.get(0).toString();
            for (int i=1; i<nodeCount.size(); i++)
                h += "," + nodeCount.get(i).toString();
            network.setHiddenLayers(h);
        }
        
        // Step 3: Train the network
        try {
            Instances trainingData = TrainData.convertToWeka();
            network.setDebug(true);
            network.buildClassifier(trainingData);
        } catch (Exception e) {
            throw new Error("Network failed to build due to: " + e.getMessage());
        }
    }

    @Override
    public void run_protected(Dataset TrainData) {
        double[] prediction = new double[TrainData.NEntries()];
        try {
            Instances trainingData = TrainData.convertToWeka();
            for (int i=0; i<trainingData.numInstances(); i++) {
                prediction[i] = network.classifyInstance(trainingData.get(i));
            }
        } catch (Exception e) {
            throw new Error("Network failed to evaluate due to: " + e.getMessage());
        }
        TrainData.setPredictedClasses(prediction);
    }
    
    @Override
    public int getNFittingParameters() {
        int output = 0;
        for (Integer i : hiddenLayers)
            output += i;
        return output;
    }

    @Override
    protected String printModel_protected() {
        return network.toString();
    }

    @Override
    public String printModelDescription(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
