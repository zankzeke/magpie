/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.utilities.splitters;

import java.util.List;
import magpie.data.Dataset;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.optimization.rankers.SimpleEntryRanker;
import magpie.user.CommandHandler;

/**
 * Split based on value of objective function. User must specify whether class 0 
 *  should consistent of entries above or below a certain threshold using a certain
 *  {@link BaseEntryRanker} method.
 * 
 * <usage><p><b>Usage</b>: &lt;above|below> &lt;threshold> &lt;objective function> [&lt;o.f. options...>]
 * <br><pr><i>above|below</i>: Whether class 0 consists of entries above/below a threshold
 * <br><pr><i>threshold</i>: Objective function value on which to split entries
 * <br><pr><i>objective function</i>: Name of {@link BaseEntryRanker} to use as objective
 * function ("?" for options)
 * <br><pr><i>o.f. options</i>: Any options for the objective function</usage>
 * @author Logan Ward
 */
public class ObjectiveFunctionSplitter extends BaseDatasetSplitter {
    /** Whether class 0 should be entries above the threshold */
    private boolean SplitAbove = true;
    /** Threshold on which to split data */
    private double Threshold = 0.0;
    /** Objective function used to split entries */
    private BaseEntryRanker objFun = new SimpleEntryRanker();
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String OFMethod;
        List<Object> OFOptions;
        try {
            if (Options.get(0).toString().toLowerCase().startsWith("ab")) {
                setSplitAbove(true);
            } else if (Options.get(0).toString().toLowerCase().startsWith("be")) {
                setSplitAbove(false);
            } else {
                throw new Exception();
            }
            setThreshold(Double.parseDouble(Options.get(1).toString()));
            if (Options.get(2).toString().startsWith("?")) {
                System.out.println(CommandHandler.printImplmentingClasses(BaseEntryRanker.class, false));
                return;
            }
            OFMethod = Options.get(2).toString();
            OFOptions = Options.subList(3, Options.size());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setObjectiveFunction((BaseEntryRanker) CommandHandler.instantiateClass(
                "optimization.rankers." + OFMethod, OFOptions));
    }

    @Override
    public String printUsage() {
        return "Usage: <above|below> <threshold> <objective function> [<o.f. options...>]";
    }

    /**
     * Set whether class #0 should be entries above a threshold
     * @param splitAbove Whether class#0 is entries above threshold
     */
    public void setSplitAbove(boolean splitAbove) {
        this.SplitAbove = splitAbove;
    }

    /**
     * Set threshold on which to partition entries
     * @param Threshold Desired threshold
     */
    public void setThreshold(double Threshold) {
        this.Threshold = Threshold;
    }

    /**
     * Define objective function by which to split entries
     * @param objFun Desired objective function
     */
    public void setObjectiveFunction(BaseEntryRanker objFun) {
        this.objFun = objFun;
        this.objFun.setUseMeasured(true);
    }

    @Override
    public int[] label(Dataset D) {
        int[] label = new int[D.NEntries()];
        // Run objective function 
        double[] OFvalue = objFun.runObjectiveFunction(D);
        // Split appropriately
        for (int i=0; i<D.NEntries(); i++) {
            label[i] = OFvalue[i] > Threshold == SplitAbove ? 0 : 1;
        }
        return label;
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing to train
    }    
}
