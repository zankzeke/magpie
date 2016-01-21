package magpie.data.utilities;

import org.apache.commons.collections.Predicate;
import java.util.Iterator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;


/**
 * This class provides useful operations for handling Datasets (and their derivatives)
 * @author Logan Ward
 */
abstract public class DatasetHelper extends Dataset {
    /** 
     * Split a dataset given a predicate (see Apache Common's Collections)
     * Predicate returns a boolean about whether an entry should be moved
     * 
     * @param D Dataset to be operated on
     * @param P Predicate capable on operating on each entry
     */
    static public Dataset split(Dataset D, Predicate P) {
        Dataset other = D.clone();
        other.clearData();
        Iterator iter = D.getEntriesWriteAccess().iterator();
        while (iter.hasNext()) {
            BaseEntry E = (BaseEntry) iter.next();
            if (P.evaluate(E)) {
                iter.remove();
                other.addEntry(E);
            }
        }
        return other;
    }
}
