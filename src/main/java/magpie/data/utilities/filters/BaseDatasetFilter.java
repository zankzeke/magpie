package magpie.data.utilities.filters;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.interfaces.Options;

/**
 * Abstract model for classes that filter undesired entries out of a dataset. User has
 * the ability to determine whether entries that pass a filter should be removed
 * or kept.
 * 
 * <p>Implementations must fulfill the following operations:
 * <ul>
 * <li>{@link #label} Generate a array defining if each entry passes the filter</li>
 * <li>{@link #train} If needed, train before filtering</li>
 * <li>{@link #setOptions} Set options to fit the user's desires</li>
 * </ul>
 *
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseDatasetFilter implements java.io.Serializable, Options {
    /** Whether to exclude entries that pass a filter */
    private boolean Exclude = true;

    /** 
     * Define whether entries that pass should be removed, or whether an entry 
     * must pass the filter to be kept.
     * @param Exclude Whether entries that pass a filter should be removed
     */
    public void setExclude(boolean Exclude) {
        this.Exclude = Exclude;
    }
    
    /**
     * @return Whether this filter is set to exclude or include entries that pass
     */
    public boolean toExclude() {
        return Exclude;
    }
    
    /**
     * Minimum number of entries to label in parallel. 
     * @return Implementer-specified minimum threshold
     */
    protected int parallelMinimum() {
        return 10000;
    }
    
    /** 
     * Given a dataset, determine which entries passes the filter. Please
     * ensure that D remains unaltered (clone if you must)
     * @param D Dataset to be labeled
     * @return A list of booleans marking whether an entry passes
     */
    abstract protected boolean[] label(Dataset D);
    
    /**
     * Label entries in parallel. 
     * @param D Dataset to be labeled
     * @return A list of entries to be labeled
     */
    protected boolean[] parallelLabel(Dataset D) {
        final boolean[] output = new boolean[D.NEntries()];
        
        // Set thread count to zero, prevent children from spawning threads
        final int nThreads = Magpie.NThreads;
        Magpie.NThreads = 1;
        
        // Split dataset into threads
        Dataset[] parts = D.splitForThreading(nThreads);
        
        // Launch parallel threads
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        for (int i=0; i<nThreads; i++) {
            final int threadID = i;
            final Dataset part = parts[i];
            Runnable thread = new Runnable() {

                @Override
                public void run() {
                    boolean[] subLabels = label(part);
                    for (int e=0; e<subLabels.length; e++) {
                        output[e * nThreads + threadID] = subLabels[e];
                    }
                }
            };
            service.submit(thread);
        }
        
        // Wait until the threads complete
        service.shutdown();
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Reset thread count
        Magpie.NThreads = 1;
        
        return output;
    }
    
    /** 
     * Filters entries out of dataset
     * @param D Dataset to filter
     */
    public void filter(Dataset D) {
        boolean[] labels;
        if (Magpie.NThreads > 1 && D.NEntries() > parallelMinimum()) {
            labels = parallelLabel(D);
        } else {
            labels = label(D);
        }
        
        // Create a list containing only the entires that pass the filter
        ArrayList<BaseEntry> newList = new ArrayList<>(D.NEntries());
        for (int i=0; i<D.NEntries(); i++) {
            if (labels[i] != Exclude) {
                newList.add(D.getEntry(i));
            }
        }
        
        // Set that as the entry list for D
        D.clearData();
        D.addEntries(newList);
    }
    
    /**
     * Train a dataset splitter, if necessary
     * @param TrainingSet Dataset to use for training
     */
    abstract public void train(Dataset TrainingSet);
}
