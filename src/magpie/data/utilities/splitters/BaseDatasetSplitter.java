package magpie.data.utilities.splitters;

import java.util.ArrayList;
import org.apache.commons.lang3.math.NumberUtils;
import java.util.Iterator;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.interfaces.Options;

/**
 * This class provides an interface to Dataset splitting operations. Any implementation 
 * must fill the following operations:
 * <ul>
 * <li>{@link #label} - Returns a list of integers that specify which subset each entry belongs to</li>
 * <li>{@link #setOptions} - Define settings for this splitter</li>
 * <li>{@link #train} - Train a Dataset splitter, if needed</li>
 * </ul>
 *
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseDatasetSplitter implements java.io.Serializable, Options {
    /** 
     * Given a dataset, determine which subset each entry should belong to. Please
     * ensure that D remains unaltered (clone if you must)
     * @param D Dataset to be labeled
     * @return A list of nonnegative integers marking which subset to split each entry into
     */
    abstract public int[] label(Dataset D);
    
    /** 
     * Splits a dataset into several partitions based on the label operation defined 
     * in this class.
     * @param D Dataset to be split. Returns empty from this operation
     */
    public List<Dataset> split(Dataset D) {
        int[] labels = label(D);
        List<Dataset> output = new ArrayList<>(NumberUtils.max(labels)+1);
        for (int i=0; i<=NumberUtils.max(labels); i++)
            output.add(D.emptyClone());
        // Split it up
        Iterator<BaseEntry> iter = D.getEntries().iterator();
        int i=0; 
        while (iter.hasNext()) {
            BaseEntry E = iter.next();
            output.get(labels[i]).addEntry(E);
            i++; iter.remove();
        }
        return output;
    }
    
    /**
     * Train a dataset splitter, if necessary
     * @param TrainingSet Dataset to use for training
     */
    abstract public void train(Dataset TrainingSet);
}
