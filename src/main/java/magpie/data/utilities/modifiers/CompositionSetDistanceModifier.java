package magpie.data.utilities.modifiers;

import java.util.*;
import java.util.concurrent.*;

import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.filters.CompositionSetDistanceFilter;
import weka.Run;

/**
 * Compute the distance of composition from those in a user-supplied dataset,
 * store as a property. Distance is defined as the minimum distance of a
 * composition from the composition of any entry in another dataset.
 *
 * <p>Name of the new property is compdistance</p>
 *
 * <p>Note: Euclidean distance is much faster than Manhattan. Weka's algorithms for fast neighbor search are 
 *  not designed to work with Manhattan distance.</p>
 *
 * <usage><p>
 * <b>Usage</b>: $&lt;dataset&gt; &lt;-manhattan|-euclidean&gt;
 * <br><pr><i>dataset</i>: {@linkplain CompositionDataset} from which to measure
 * distance
 * <br><pr><i>-manhattan</i>: Compute Manhattan distance
 * <br><pr><i>-euclidean</i>: Compute Euclidean distance</usage>
 *
 * @author Logan Ward
 * @see CompositionSetDistanceFilter
 */
public class CompositionSetDistanceModifier extends BaseDatasetModifier {
    /** Set of compositions to compute distance from */
    protected final Set<CompositionEntry> Compositions = new TreeSet<>();
    /** Whether to use Manhattan distance (vs Euclidean) */
    protected boolean UseManhattan = false;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        CompositionDataset data;
        boolean manhattan;
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
        } catch (Exception e) {
            throw new Exception(printUsage());
        }

        // Set options
        setCompositions(data);
        setUseManhattan(manhattan);
    }

    @Override
    public String printUsage() {
        return "Usage: $<compositions> <-manhattan|-euclidean>";
    }

    /**
     * Set whether to use Manhattan (vs Euclidean) distance
     * @param manhattan Desired setting
     */
    public void setUseManhattan(boolean manhattan) {
        UseManhattan = manhattan;
    }

    /**
     * Clear the list of compositions in set
     */
    public void clearCompositions() {
        Compositions.clear();
    }

    /**
     * Add a new composition to the dataset.
     *
     * @param entry Entry to be added
     */
    public void addComposition(CompositionEntry entry) {
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
     *
     * @param comps Collection of compositions to be added
     */
    public void addCompositions(Collection<CompositionEntry> comps) {
        for (CompositionEntry comp : comps) {
            addComposition(comp);
        }
    }

    /**
     * Set the list of compositions to be considered
     *
     * @param data Dataset containing compositions to use as dataset
     */
    public void setCompositions(CompositionDataset data) {
        clearCompositions();
        for (BaseEntry entry : data.getEntries()) {
            CompositionEntry comp = (CompositionEntry) entry;
            addComposition(comp);
        }
    }

    @Override
    protected void modifyDataset(Dataset Data) {
        if (!(Data instanceof CompositionDataset)) {
            throw new RuntimeException("Data must extend CompositionDataset");
        }
        CompositionDataset p = (CompositionDataset) Data;

        // Add property to dataset, if needed
        final int propID = p.getPropertyIndex("compdistance");
        if (propID == -1) {
            p.addProperty("compdistance");
        }

        // Split the dataset for threading
        Dataset[] threads = Data.splitForThreading(Data.NEntries() > 10 ? Magpie.NThreads : 1);

        // Parallel run over the entries
        ExecutorService service = Executors.newFixedThreadPool(Magpie.NThreads);
        List<Future> futures = new ArrayList<>(Magpie.NThreads);
        for (final Dataset part : threads) {
            Runnable thread = new Runnable() {
                @Override
                public void run() {
                    // Create a local copy of the used to compute distances
                    CompositionSetDistanceFilter filter = new CompositionSetDistanceFilter();
                    filter.addCompositions(Compositions);
                    filter.setUseManhattan(UseManhattan);

                    for (BaseEntry ptr : part.getEntries()) {
                        CompositionEntry e = (CompositionEntry) ptr;
                        double x = filter.computeDistance(e);
                        if (propID == -1) {
                            e.addProperty(x, x);
                        } else {
                            e.setMeasuredProperty(propID, x);
                            e.setPredictedProperty(propID, x);
                        }
                    }
                }
            };
            futures.add(service.submit(thread));
        }

        // Wait for results
        service.shutdown();

        // Make sure each thread finishes
        try {
            for (Future future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException i) {
            throw new RuntimeException(i);
        }
    }

}
