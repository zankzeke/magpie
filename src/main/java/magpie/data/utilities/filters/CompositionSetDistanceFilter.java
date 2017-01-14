package magpie.data.utilities.filters;

import java.util.*;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import weka.core.*;
import weka.core.neighboursearch.*;

/**
 * Filter based on whether the distance of a composition from any other composition in
 * a dataset is past a threshold. The distance between two compositions is based on 
 * the distance between vectors defined by element fractions. We use either the Manhattan
 * distance (equivalent to the L<sub>1</sub> norm used in {@linkplain CompositionDistanceFilter})
 * or the Euclidean distance (L<sub>2</sub> norm). The distance
 * between a composition and the set is defined as the minimum distance between a 
 * composition and any entry in the dataset.
 * 
 * <p>Entries are labeled based on whether they are farther than a certain threshold.</p>
 *
 * <p>Note: Euclidean distance is much faster than Manhattan. Weka's algorithms for fast neighbor search are 
 *  not designed to work with Manhattan distance.</p>
 *
 * <p>Developer note: This class uses different distance metrics than {@linkplain CompositionDistanceFilter}<
 * in order to use the {@linkplain weka.core.neighboursearch.NearestNeighbourSearch} classes from
 * Weka to accelerate finding the closest compositions.</p>
 * 
 * <usage><p><b>Usage</b>: $&lt;dataset&gt; &lt;-mahanttan|-euclidean&gt; &lt;threshold&gt;
 * <br><pr><i>dataset</i>: {@linkplain CompositionDataset} containing all compositions being considered
 * <br><pr><i>-manhattan</i>: Use Manhattan distance metric
 * <br><pr><i>-euclidean</i>: Use Euclidean distance metric
 * <br><pr><i>threshold</i>: Distance threshold</usage>
 * 
 * @author Logan Ward
 */
public class CompositionSetDistanceFilter extends BaseDatasetFilter implements Cloneable {
    /** Set of compositions to consider */
    private Set<CompositionEntry> Compositions = new TreeSet<>();
    /** Whether to use Manhattan distance (or Euclidean) */
    private boolean UseManhattan = false;
    /** Set distance threshold */
    private double Threshold = 0.1;
    /** Neighbor search tool. */
    transient private NearestNeighbourSearch Searcher = null;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        CompositionDataset data;
        boolean manhattan = true;
        double threshold;
        try {
            data = (CompositionDataset) Options.get(0);
            String distChoice = Options.get(1).toString().toLowerCase();
            if (distChoice.startsWith("-man")) {
                manhattan = true;
            } else if (distChoice.startsWith("-euclid")) {
                manhattan = false;
            } else {
                throw new IllegalArgumentException();
            }
            threshold = Double.parseDouble(Options.get(2).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set options
        setCompositions(data);
        setUseManhattan(manhattan);
        setDistanceThreshold(threshold);
    }

    @Override
    public CompositionSetDistanceFilter clone() {
        try {
            CompositionSetDistanceFilter x = (CompositionSetDistanceFilter) super.clone();
            x.Compositions = new HashSet<>(Compositions);
            return x;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<compositions> <-manhattan|-euclidean> <threshold>";
    }

    /**
     * Set whether to use Manhattan (vs Euclidean) distance
     * @param manhattan Desired setting
     */
    public void setUseManhattan(boolean manhattan) {
        if (manhattan) {
            setUseManhattan();
        } else {
            setUseEuclidean();
        }
    }

    /**
     * Use Manhattan distance as the distance metric
     * @see #setUseEuclidean()
     */
    public void setUseManhattan() {
        UseManhattan = true;
    }

    /**
     * Use Euclidean distance as the distance metric
     * @see #setUseManhattan()
     */
    public void setUseEuclidean() {
        UseManhattan = false;
        Searcher = null;
    }

    /**
     * Clear the list of compositions in set
     */
    public void clearCompositions() {
        Compositions.clear();
        Searcher = null;
    }
    
    /**
     * Add a new composition to the dataset.
     * @param entry Entry to be added
     */
    public void addComposition(CompositionEntry entry) {
        // Clear the NN search tool
        Searcher = null;

        // Make a copy without any attributes, properties, etc.
        try {
            CompositionEntry toAdd = new CompositionEntry(entry.getElements(), entry.getFractions());
            Compositions.add(toAdd);
        } catch (Exception e) {
            throw new Error(e);
        }
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
    protected int parallelMinimum() {
        return 200;
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
            output[i] = computeDistance(entry) > Threshold;
        }
        return output;
    }

    /**
     * Convert a composition to a Weka instance. Puts it in a form that
     * can be used with Weka's neighbor search methods. Attributes for
     * the instance are the fraction of each element.
     * @param entry Entry to be converted
     * @return Instance describing the composition of the entry
     */
    static public Instance convertCompositionToInstance(CompositionEntry entry) {
        Instance output = new SparseInstance(LookupData.ElementNames.length);

        // Set values to zero
        for (int i=0; i<LookupData.ElementNames.length; i++) {
            output.setValue(i, 0.0);
        }

        // Add the composition
        int[] elems = entry.getElements();
        double[] fracs = entry.getFractions();
        for (int i=0; i<elems.length; i++) {
            output.setValue(elems[i], fracs[i]);
        }

        return output;
    }

    /**
     * Convert a collection of entries to a Weka Instances object. Another step in
     * getting ready to make a Weka nearest neighbor search tool.
     * @param entries Entries to be converted
     * @return Weka dataset object
     */
    static protected Instances convertCompositionsToInstances(Collection<CompositionEntry> entries) {
        // Make the attributes
        ArrayList<Attribute> attrInfo = new ArrayList<>(LookupData.ElementNames.length);
        for (String elem : LookupData.ElementNames) {
            attrInfo.add(new Attribute(elem));
        }

        // Make the Instances
        Instances output = new Instances("compositions", attrInfo, entries.size());
        for (CompositionEntry entry : entries) {
            output.add(convertCompositionToInstance(entry));
        }

        return output;
    }

    /**
     * Generate a tool to enable fast searching for nearest compositions. Stored in the class.
     */
    public void makeNeighborSearchTool() {
        Instances data = convertCompositionsToInstances(Compositions);
        NearestNeighbourSearch search = UseManhattan ? new LinearNNSearch() : new CoverTree();

        try {
            // Set the distances to search through
            search.setInstances(data);

            // Set the distance measure (making sure to turn off normalization)
            NormalizableDistance dist = UseManhattan ? new ManhattanDistance() : new EuclideanDistance();
            dist.setDontNormalize(true);
            dist.setInstances(data);
            search.setDistanceFunction(dist);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Searcher = search;
    }

    /**
     * Compute the distance between a composition and the set of other compositions stored
     * in this object. Distance is defined by your choice of {@linkplain #setUseManhattan()}
     * @param entry Composition of entry
     * @return Distance from set
     */
    public double computeDistance(CompositionEntry entry) {
        Instance inst = convertCompositionToInstance(entry);
        // Generate the search tool, if not already available
        if (Searcher == null) {
            makeNeighborSearchTool();
        }

        // Get the set distance
        try {
            Instance nearestInst = Searcher.nearestNeighbour(inst);
            return Searcher.getDistanceFunction().distance(inst, nearestInst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
