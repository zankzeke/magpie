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
 * <usage><p><b>Usage</b>: &lt;-manhanttan|-euclidean&gt;
 * <br><pr><i>dataset</i>: {@linkplain CompositionDataset} containing all compositions being considered
 * <br><pr><i>-manhattan</i>: Use Manhattan distance metric
 * <br><pr><i>-euclidean</i>: Use Euclidean distance metric</usage>
 * 
 * @author Logan Ward
 * @see CompositionSetDistanceFilter
 */
public class CompositionDistanceRanker extends BaseEntryRanker {
    /** Set of compositions to consider */
    private CompositionSetDistanceFilter DistanceComputer = new CompositionSetDistanceFilter();

    @Override
    public CompositionDistanceRanker clone() {
        CompositionDistanceRanker x = (CompositionDistanceRanker) super.clone();
        x.DistanceComputer = DistanceComputer.clone();
        return x;
    }
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        CompositionDataset data;
        boolean manhattan;
        try {
            data = (CompositionDataset) Options.get(0);
            String distChoice = Options.get(1).toString().toLowerCase();
            if (distChoice.startsWith("man")) {
                manhattan = true;
            } else if (distChoice.startsWith("euclid")) {
                manhattan = false;
            } else {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set options
        clearCompositions();
        setCompositions(data);
        setUseManhattan(manhattan);
    }

    @Override
    public String printUsage() {
        return "Usage: $<compositions> <-manhattan|-euclidean>";
    }
    
    /**
     * Clear the list of compositions in set
     */
    public void clearCompositions() {
        DistanceComputer.clearCompositions();
    }
    
    /**
     * Add a new composition to the dataset.
     * @param entry Entry to be added
     */
    public void addComposition(CompositionEntry entry) {
        DistanceComputer.addComposition(entry);
    }
    
    /**
     * Add a list of compositions to the set
     * @param comps Collection of compositions to be added
     */
    public void addCompositions(Collection<CompositionEntry> comps) {
        DistanceComputer.addCompositions(comps);
    }
    
    /**
     * Set the list of compositions to be considered
     * @param data Dataset containing compositions to use as dataset
     */
    public void setCompositions(CompositionDataset data) {
       DistanceComputer.setCompositions(data);
    }

    /**
     * Set whether to use
     * @param manhattan
     */
    public void setUseManhattan(boolean manhattan) {
        if (manhattan) {
            DistanceComputer.setUseManhattan();
        } else {
            DistanceComputer.setUseEuclidean();
        }
    }
    
    @Override
    public void train(Dataset data) {
        // Nothing to do
    }

    @Override
    public double objectiveFunction(BaseEntry Entry) {
        return DistanceComputer.computeDistance((CompositionEntry) Entry);
    }
}
