package magpie.data.utilities.filters;

import java.util.List;
import java.util.Map;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.GCLPCalculator;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Filter based on computed stability. Assumes that the class variable is 
 * is the formation energy. Stability is defined as the difference between the
 * formation energy for a compound and the minimum energy combination of phases
 * at the composition of that phase. Negative "stability" means that the compound
 * is more stable than those phases.
 * 
 * <p>Labels entries with stability less than a threshold
 * 
 * <usage><p><b>Usage</b>: $&lt;hull data&gt; &lt;measured|predicted&gt; &lt;threshold&gt;
 * <br><pr><i>hull data</i>: Formation energy of compounds on the convex hull
 * <br><pr><i>measured|predicted</i>: Whether to use measured or predicted class variable
 * <br><pr><i>threshold</i>: Stability threshold above which compounds are eliminated.</usage>
 * 
 * @author Logan Ward
 */
public class StabilityFilter extends BaseDatasetFilter {
    /** Tool used to perform GCLP */
    private GCLPCalculator GCLPCalculator = new GCLPCalculator();
    /** Whether to use predicted class */
    private boolean UsePredicted = true;
    /** Stability threshold */
    private double Threshold = 0.0;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        CompositionDataset comp;
        boolean usePred;
        double threshold;
        try {
            comp = (CompositionDataset) Options.get(0);
            String temp = Options.get(1).toString().toLowerCase();
            if (temp.startsWith("meas")) {
                usePred = false;
            } else if (temp.startsWith("pred")) {
                usePred = true;
            } else {
                throw new Exception(printUsage());
            }
            threshold = Double.parseDouble(Options.get(2).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Define settings
        setCompounds(comp);
        setUsePredicted(usePred);
        setThreshold(threshold);
    }

    @Override
    public String printUsage() {
        return "Usage: $<hull data> <measured|predicted> <threshold>";
    }
    
    /** 
     * Define the data used to compute stability. This dataset must contain 
     * the energy of each compound as the measured class variable.
     * @param data Data containing the energies of all known phases
     */
    public void setCompounds(CompositionDataset data) {
        GCLPCalculator = new GCLPCalculator();
        GCLPCalculator.addPhases(data);
    }

    /**
     * Set whether to use the measured or predicted class variable.
     * @param x Desired setting
     */
    public void setUsePredicted(boolean x) {
        this.UsePredicted = x;
    }

    /**
     * Set threshold at which compounds are declared "unstable".
     * @param threshold Desired threshold (default = 0)
     */
    public void setThreshold(double threshold) {
        this.Threshold = threshold;
    }

    @Override
    protected int parallelMinimum() {
        return 100;
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing to do
    }

    @Override
    protected boolean[] label(Dataset D) {
        // Make sure the data is materials data
        if (! (D instanceof CompositionDataset)) {
            throw new Error("Data must be a CompositionDataset");
        }
        
        // Compute stabilities
        boolean[] output = new boolean[D.NEntries()];
        for (int e=0; e<D.NEntries(); e++) {
            CompositionEntry entry = (CompositionEntry) D.getEntry(e);
            Pair<Double,Map<CompositionEntry, Double>> res;
            try {
                 res = GCLPCalculator.runGCLP(entry);
            } catch (Exception ex) {
                throw new Error(ex.getMessage() + " - entry: " + entry.toString());
            }
            double stab = (UsePredicted ? entry.getPredictedClass() : 
                    entry.getMeasuredClass()) - res.getKey();
            output[e] = stab < Threshold;
        }
        
        return output;
    }
    
}
