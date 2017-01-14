package magpie.data.utilities.filters;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
 * <br><pr><i>distance</i>: Maximum change an the fraction of any element in %.</usage>
 * 
 * @author Logan Ward
 */
public class CompositionDistanceFilter extends BaseDatasetFilter {
    /** Target composition */
    private CompositionEntry TargetComposition;
    /** Threshold distance */
    private double Threshold;

    /**
     * Compute the distance between two entries. Distance is defined as the
     * <i>L<sub>p</sub></i> norm of the distance between the element fractions.
     * @param entry1 Entry A
     * @param entry2 Entry B
     * @param p Desired <i>L<sub>p</sub></i> norm (must be &ge; 0)
     * @return Distance
     */
    static public double computeDistance(CompositionEntry entry1,
            CompositionEntry entry2, int p) {
        // Get the set of elements to consider
        Set<Integer> elems = new TreeSet<>();
        for (int elem : entry1.getElements()) {
            elems.add(elem);
        }
        for (int elem : entry2.getElements()) {
            elems.add(elem);
        }

        // Compute differences
        double dist = 0.0;
        for (Integer elem : elems) {
            double diff = entry1.getElementFraction(elem)
                    - entry2.getElementFraction(elem);
            if (p == 0) {
                if (diff != 0) dist++;
            } else if (p == -1) {
                dist = Math.max(dist, diff);
            } else {
                dist += Math.pow(Math.abs(diff), p);
            }
        }

        // Compute distance
        if (p == 0 || p == -1) {
            return dist;
        } else {
            return Math.pow(dist, 1.0 / p);
        }
    }

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
