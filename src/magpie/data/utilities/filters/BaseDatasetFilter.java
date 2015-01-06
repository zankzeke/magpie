package magpie.data.utilities.filters;

import java.util.Iterator;
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
     * Given a dataset, determine which entries passes the filter. Please
     * ensure that D remains unaltered (clone if you must)
     * @param D Dataset to be labeled
     * @return A list of booleans marking whether an entry passes
     */
    abstract protected boolean[] label(Dataset D);
    
    /** 
     * Filters entries in a Dataset depending
     * @param D Dataset to be split. Returns empty from this operation
     */
    public void filter(Dataset D) {
        boolean[] labels = label(D);
        // Split it up
        Iterator<BaseEntry> iter = D.getEntries().iterator();
        int i=0; 
        while (iter.hasNext()) {
            iter.next();
            if (labels[i] == Exclude)
                iter.remove();
            i++;
        }
    }
    
    /**
     * Train a dataset splitter, if necessary
     * @param TrainingSet Dataset to use for training
     */
    abstract public void train(Dataset TrainingSet);
}
