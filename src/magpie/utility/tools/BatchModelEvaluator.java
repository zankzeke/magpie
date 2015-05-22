package magpie.utility.tools;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;

/**
 * Generate attributes and run models in batches. Useful if a dataset contains too
 * many entries to store all of their attributes in memory at one time.
 * 
 * <usage><p><b>Usage</b>: $&lt;model&gt; &lt;batch size&gt;
 * <br><pr><i>model</i>: Model to be evaluated 
 * <br><pr><i>batch size</i>: Number of entries to evaluate at one time</usage>
 * 
 * <p><b><u>Implemented Commands</u></b>
 * 
 * <command><p><b>run $&lt;data&gt;</b> - Evaluate a model in batch model
 * <br><pr><i>data</i>: Dataset to be evaluated</command>
 * @author Logan Ward
 */
public class BatchModelEvaluator implements Commandable, Options {
    /** Model to be evaluated */
    private BaseModel Model;
    /** Number of entries to evaluate at once */
    private int BatchSize = 10000;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            if (Options.size() != 2) {
                throw new Exception();
            }
            setModel((BaseModel) Options.get(0));
            setBatchSize(Integer.parseInt(Options.get(1).toString()));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Define which model this class is used to evaluate
     * @param model Model to be evaluated
     */
    public void setModel(BaseModel model) {
        this.Model = model;
    }

    /**
     * Define how many entries to evaluate at once. 
     * @param size Desired batch size
     */
    public void setBatchSize(int size) {
        this.BatchSize = size;
    }
    
    /**
     * Evaluate the model on a dataset in batch mode.
     * @param data Dataset to be evaluated
     * @throws Exception 
     */
    public void evaluate(Dataset data) throws Exception {
        // Create clone used to store results
        Dataset runData = data.emptyClone();
        
        // Get all entries
        List<BaseEntry> entries = data.getEntries();
        
        // Loop through
        int iteration = 0;
        while (true) {
            // Determine swath of entries to be run
            boolean lastRun = false;
            int startPos = iteration * BatchSize;
            int endPos = startPos + BatchSize;
            if (endPos > entries.size()) {
                endPos = entries.size();
                lastRun = true;
            }
            iteration++;
            
            // Get the subset
            List<BaseEntry> subList = entries.subList(startPos, endPos);
            
            // Run them
            runData.addEntries(subList);
            runData.generateAttributes();
            Model.run(runData);
            
            // Clean up
            for (BaseEntry entry : subList) {
                entry.clearAttributes();
            }
            runData.clearData();
            System.gc();
            
            // Check if we're done
            if (lastRun) {
                break;
            }
        }
        
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println("Evaluator with a " + Model.getClass().getSimpleName());
            return null;
        }
        
        // Parse input
        String action = Command.get(0).toString().toLowerCase();
        if (! action.equals("run")) {
            throw new Exception("Only one command: run $<data>");
        }
        Dataset data;
        try {
            data = (Dataset) Command.get(1);
        } catch (Exception e) {
            throw new Exception("Usage: run $<data>");
        }
        
        // Run it
        evaluate(data);
        System.out.println("\tEvaluated a " + Model.getClass().getSimpleName() + 
                " on " + data.NEntries() + " entries in blocks of " + BatchSize);
        return null;
    }
}
