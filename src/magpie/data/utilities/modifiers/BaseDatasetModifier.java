package magpie.data.utilities.modifiers;

import java.io.Serializable;
import magpie.data.Dataset;
import magpie.utility.interfaces.Options;

/**
 * Perform some kind of manipulation on a Dataset, and all of the entries. 
 * 
 * <p>Implementations of this class must provide:
 * <ul>
 * <li>{@link #modifyDataset} - Make changes to the dataset as whole and each entry</li>
 * <li>{@link #clone} - Ensure clones of this class do not share references</li>
 * <li>{@link #setOptions} and {@link #printUsage} - Handle user options</li>
 * </ul>
 * 
 * <p>Note, you may want to make special considerations for MultiPropertyDatasets if you
 * are adjusting the class variables. See example in {@link NonZeroClassModifier}.
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseDatasetModifier implements Serializable, Cloneable, Options {
    /**
     * Apply some change to the dataset and all of its entries
     * @param Data Dataset to be modified
     */
    public void transform(Dataset Data) {
        modifyDataset(Data);
    }
    
    /**
     * Apply changes to the Dataset. Should modify both each entries and the dataset
     *  as a whole.
     * @param Data Dataset to be modified
     */
    abstract protected void modifyDataset(Dataset Data);
}
