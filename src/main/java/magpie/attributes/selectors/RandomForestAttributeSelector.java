package magpie.attributes.selectors;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;

/**
 * Select attributes using the feature-importance scores from RandomForest.
 * 
 * <p>This selector works by iteratively selecting only the best-performing 
 * attributes as selected by a RandomForest model, and then repeating the process 
 * on the reduced set of attributes.
 * 
 * <p>Procedure:
 * 
 * <ol>
 * <li>Determine a constant factor, f, such that f^n times the original number 
 * of attributes equals the target number of attributes, where n is the number 
 * of loop steps
 * <li>Train a RandomForest model on the data
 * <li>Identify the top f% of attributes based on the "feature importance" score 
 * from the RandomForest model
 * <li>Eliminate all other attributes from the dataset
 * <li>If the number of remaining attributes is greater than the target number, 
 * repeat from Step 2
 * </ol>
 * 
 * <usage><p><b>Usage</b>: -num_attr &lt;attrs&gt; [-num_steps &lt;steps&gt;]
 * [-num_trees &lt;trees&gt;]
 * <br><pr><i>attrs</i>: Number of attributes to select
 * <br><pr><i>steps</i>: Number of selection steps to employ (default = 8)
 * <br><pr><i>trees</i>: Number of trees to use in RandomForest (default = 100)</usage>
 * 
 * @author Logan Ward
 * 
 */
public class RandomForestAttributeSelector extends PythonBasedAttributeSelector {
    /** Number of attributes to select. */
    protected int NumAttributes = 2;
    /** Number of selection steps */
    protected int NumSteps = 8;
    /** Number of trees to use in RandomForest */
    protected int NumTrees = 100;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int n_attr; // Mandatory
        int n_step = 8, n_trees = 100; // Optional
        
        
        try {
            // Get number of attributes
            n_attr = Integer.parseInt(Options.get(1).toString());
            
            // Get the remaining arguments
            int pos = 2;
            while (pos < Options.size()) {
                switch (Options.get(pos).toString().toLowerCase()) {
                    case "-num_steps":
                        pos++;
                        n_step = Integer.parseInt(Options.get(pos).toString());
                        break;
                    case "-num_trees":
                        pos++;
                        n_trees = Integer.parseInt(Options.get(pos).toString());
                        break;
                    case "-debug":
                        Debug = true;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                pos++;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
        
        // Set the values
        setNumAttributes(n_attr);
        setNumSteps(n_step);
        setNumTrees(n_trees);
    }

    @Override
    public String printUsage() {
        return "Usage: -num_attr <attrs> [-num_steps <steps>] [-num_trees <trees>]";
    }

    @Override
    protected String getScriptPath() {
        return "py/rf_attr_selection.py";
    }

    /**
     * Set the target number of attributes to be selected
     * @param numAttributes Target number of attributes 
     */
    public void setNumAttributes(int numAttributes) {
        this.NumAttributes = numAttributes;
    }

    /**
     * Number of iterations during attribute selection
     * @param numSteps Desired number of steps
     */
    public void setNumSteps(int numSteps) {
        this.NumSteps = numSteps;
    }

    /**
     * Number of trees for the RandomForest model
     * @param numTrees Desired number of trees
     */
    public void setNumTrees(int numTrees) {
        this.NumTrees = numTrees;
    }
    
    @Override
    protected List<String> assembleSystemCall(File codePath, Dataset data) {
        // Call to the Python script
        List<String> call = new LinkedList<>();
        call.add("python");
        call.add(codePath.getAbsolutePath());

        // Add in the settings
        call.add("-num_attr"); 
        call.add(Integer.toString(NumAttributes));
        
        call.add("-num_steps"); 
        call.add(Integer.toString(NumSteps));
        
        if (data.NClasses() == 1) {
            call.add("-regression");
        }
        
        call.add("-num_trees");
        call.add(Integer.toString(NumTrees));
        
        return call;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = String.format("Iteratively uses RandomForest to determine "
                + "the most important %d attributes. ", NumAttributes);
        
        output += String.format("Iteratively identifies the best attributes over"
                + " %d different steps using a RandomForest model with %d subtrees",
                NumSteps, NumTrees);
        
        return output;
    }
}
