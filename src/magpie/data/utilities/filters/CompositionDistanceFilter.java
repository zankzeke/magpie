package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Filter compositions based on distance from a target composition. Filters
 * any composition where the maximum change in any element is less than a certain
 * value.
 * 
 * <usage><p><b>Usage</b>: &lt;composition&gt; &lt;distance&gt;
 * <br><pr><i>composition</i>: Target composition
 * <br><pr><i>distance</i>: Maximum change an the fraction of any element.</usage>
 * 
 * @author Logan Ward
 */
public class CompositionDistanceFilter extends BaseDatasetFilter {
    /** Target composition */
    private CompositionEntry TargetComposition;
    /** Threshold distance */
    private double Threshold;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String comp;
        double thresh;
        try {
            if (Options.size() != 2) {
                throw new Exception();
            }
            comp = Options.get(0).toString();
            thresh = Double.parseDouble(Options.get(1).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setTargetComposition(new CompositionEntry(comp));
        setDistanceThreshold(thresh);
    }

    @Override
    public String printUsage() {
        return "Usage: <composition> <distance>";
    }

    /**
     * Define the target composition
     * @param composition Target composition
     */
    public void setTargetComposition(CompositionEntry composition) {
        this.TargetComposition = composition;
    }

    /**
     * Define the threshold composition distance. Here, distance is defined as the 
     * maximum change in the fraction of any element.
     * @param distance Target threshold in %
     */
    public void setDistanceThreshold(double distance) {
        this.Threshold = distance / 100.0;
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing to do
    }

    @Override
    protected boolean[] label(Dataset D) {
        if (! (D instanceof CompositionDataset)) {
            throw new Error("Data must be a CompositionDataset.");
        }
        
        // Get target composition
        int[] targetElems = TargetComposition.getElements();
        double[] targetFrac = TargetComposition.getFractions();
        
        CompositionDataset ptr = (CompositionDataset) D;
        boolean[] label = new boolean[D.NEntries()];
        for (int en=0; en<D.NEntries(); en++) {
            boolean withinBounds = true;
            for (int el=0; el<targetElems.length; el++) {
                if (Math.abs(ptr.getEntry(en).getElementFraction(targetElems[el]) 
                        - targetFrac[el]) > Threshold) {
                    withinBounds = false;
                    break;
                }
            }
            
            label[en] = withinBounds;
        }
        
        return label;
    }
    
}
