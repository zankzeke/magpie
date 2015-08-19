
package magpie.data.utilities.modifiers;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.filters.CompositionSetDistanceFilter;

/**
 * Compute the distance of composition from those in a user-supplied dataset,
 *  store as a property. Distance is defined as the minimum distance of a composition
 *  from the composition of any entry in another dataset.
 * 
 * <p>Name of the new property is compdistance
 * 
 * <usage><p><b>Usage</b>: $&lt;dataset&gt; &lt;p norm&gt;
 * <br><pr><i>dataset</i>: {@linkplain CompositionDataset} from which to measure distance
 * <br><pr><i>p norm</i>: P norm to use when computing distance</usage>
 * 
 * @author Logan Ward
 * @see CompositionSetDistanceFilter
 */
public class CompositionSetDistanceModifier extends BaseDatasetModifier {
    /** Set of compositions to consider */
    private final Set<CompositionEntry> Compositions = new TreeSet<>();
    /** P-norm to use when computing distance (default: 2) */
    private int P = 2;
    
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
	protected void modifyDataset(Dataset Data) {
		if (! (Data instanceof CompositionDataset)) {
			throw new Error("Data must extend CompositionDataset");
		}
		CompositionDataset p = (CompositionDataset) Data;
		
		// Add property to dataset
		p.addProperty("compdistance");
        
        // Check if large enough to consider parallelization
        if (p.NEntries() > 2000) {
            // Split the dataset for threading
            Dataset[] threads = Data.splitForThreading(Magpie.NThreads);

            // Parallel run over the entries
            ExecutorService service = Executors.newFixedThreadPool(Magpie.NThreads);
            for (final Dataset part : threads) {
                Runnable thread = new Runnable() {
                    @Override
                    public void run() {
                        for (BaseEntry ptr : part.getEntries()) {
                            CompositionEntry e = (CompositionEntry) ptr;
                            double x = CompositionSetDistanceFilter.computeDistance(Compositions, e, P);
                            e.addProperty(x, x);
                        }
                    }
                };
                service.submit(thread);
            }
            
            // Wait for results
            service.shutdown();
            try {
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException i) {
                throw new Error(i);
            }
        } else {
            // Run it serially
            for (BaseEntry ptr : p.getEntries()) {
                CompositionEntry e = (CompositionEntry) ptr;
                double x = CompositionSetDistanceFilter.computeDistance(Compositions, e, P);
                e.addProperty(x, x);
            }
        }
	}
	
}
