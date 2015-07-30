package magpie.optimization.rankers;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.filters.CompositionSetDistanceFilter;

/**
 * Rank entries based on distance from compositions in a user-provided dataset.
 * Use this if, for instance, you are interested in materials not included in 
 * a training set.
 * 
 * <usage><p><b>Usage</b>: $&lt;dataset&gt; &lt;norm&gt; &lt;threshold&gt;
 * <br><pr><i>dataset</i>: {@linkplain CompositionDataset} containing all compositions being considered
 * <br><pr><i>norm</i>: Which p norm to use when computing distances. Use -1 for
 * the L<sub>inf</sub> norm.</usage>
 * 
 * @author Logan Ward
 * @see CompositionSetDistanceFilter
 */
public class CompositionDistanceRanker extends BaseEntryRanker {
    /** Set of compositions to consider */
    private Set<CompositionEntry> Compositions = new TreeSet<>();
    /** P-norm to use when computing distance (default: 2) */
    private int P = 2;

    @Override
    public CompositionDistanceRanker clone() {
        CompositionDistanceRanker x = (CompositionDistanceRanker) super.clone();
        x.Compositions = new TreeSet<>(Compositions);
        return x;
    }
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        CompositionDataset data;
        int norm;
        try {
            data = (CompositionDataset) Options.get(0);
            norm = Integer.parseInt(Options.get(1).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set options
        setCompositions(data);
        setP(norm);
    }

    @Override
    public String printUsage() {
        return "Usage: $<compositions> <p norm>";
    }
    
    /**
     * Clear the list of compositions in set
     */
    public void clearCompositions() {
        Compositions.clear();
    }
    
    /**
     * Add a new composition to the dataset.
     * @param entry Entry to be added
     */
    public void addComposition(CompositionEntry entry) {
        // Make a copy without any attributes, properties, etc.
        CompositionEntry toAdd = new CompositionEntry(entry.getElements(), entry.getFractions());
        Compositions.add(toAdd);
    }
    
    /**
     * Add a list of compositions to the set
     * @param comps Collection of compositions to be added
     */
    public void addCompositions(Collection<CompositionEntry> comps) {
        for (CompositionEntry comp : comps) {
            addComposition(comp);
        }
    }
    
    /**
     * Set the list of compositions to be considered
     * @param data Dataset containing compositions to use as dataset
     */
    public void setCompositions(CompositionDataset data) {
        clearCompositions();
        for (BaseEntry entry : data.getEntries()) {
            CompositionEntry comp = (CompositionEntry) entry;
            addComposition(comp);
        }
    }

    /**
     * Set the p-norm to use when computing distance between compositions.
     * @param P Desired p norm. Use -1 for <i>L<sub>inf</sub></i> norm.
     * @throws Exception If p &lt; 0 && p != -1.
     */
    public void setP(int P) throws Exception {
        if (P < 0 && P != -1) {
            throw new Exception("P must be greater than 0");
        }
        this.P = P;
    }
    
    @Override
    public void train(Dataset data) {
        // Nothing to do
    }

    @Override
    public double objectiveFunction(BaseEntry Entry) {
        return CompositionSetDistanceFilter.computeDistance(Compositions, (CompositionEntry) Entry, P);
    }
    
}
