package magpie.data.utilities.filters;

import com.sun.java.swing.plaf.windows.WindowsTreeUI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Filter based on whether the distance of a composition from any other composition in
 * a dataset is past a threshold. The distance between two compositions is based on 
 * the <i>L<sub>p</sub></i> norm of the difference in element fractions. The distance 
 * between a composition and the set is defined as the minimum distance between a 
 * composition and any entry in the dataset.
 * 
 * <p>Entries are labeled based on whether they are farther than a certain threshold.
 * 
 * <usage><p><b>Usage</b>: $&lt;dataset&gt; &lt;norm&gt; &lt;threshold&gt;
 * <br><pr><i>dataset</i>: {@linkplain CompositionDataset} containing all compositions being considered
 * <br><pr><i>norm</i>: Which p norm to use when computing distances. Use -1 for
 * the L<sub>inf</sub> norm.
 * <br><pr><i>threshold</i>: Distance threshold</usage>
 * 
 * @author Logan Ward
 */
public class CompositionSetDifferenceFilter extends BaseDatasetFilter {
    /** Set of compositions to consider */
    private Set<CompositionEntry> Compositions = new TreeSet<>();
    /** P-norm to use when computing distance (default: 2) */
    private int P = 2;
    /** Set distance threshold */
    private double Threshold = 0.1;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    
    /**
     * Set the distance threshold
     * @param dist Desired distance threshold
     * @throws Exception If distance is &le; 0
     */
    public void setDistanceThreshold(double dist) throws Exception {
        if (dist <= 0) {
            throw new Exception("Distance must be greater than 0");
        }
        Threshold = dist;
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing to train
    }    

    @Override
    protected boolean[] label(Dataset D) {
        if (! (D instanceof CompositionDataset)) {
            throw new Error("Data must be a CompositionDataset");
        }
        
        // Run filter
        boolean[] output = new boolean[D.NEntries()];
        for (int i=0; i<D.NEntries(); i++) {
            CompositionEntry entry = (CompositionEntry) D.getEntry(i);
            double dist = computeDistance(Compositions, entry, P);
            output[i] = dist > Threshold;
        }
        return output;
    }
    
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
    
    /**
     * Compute the distance between a composition and a set of other compositions.
     * Distance from the set is defined as the minimum distance from any points. 
     * Distance between two compositions is defined in 
     * {@linkplain #computeDistance(magpie.data.materials.CompositionEntry, magpie.data.materials.CompositionEntry, int) }
     * @param set Set of compositions to consider
     * @param entry Composition of entry
     * @param p Desired <i>L<sub>p</sub></i> norm. Use -1 for <i>L<sub>inf</sub></i>
     * @return Distance from set
     */
    static public double computeDistance(Collection<CompositionEntry> set, 
            CompositionEntry entry, int p) {
        double dist = Double.MAX_VALUE;
        for (CompositionEntry setEntry : set) {
            dist = Math.min(dist, computeDistance(setEntry, entry, p));
        }
        return dist;
    }
}
