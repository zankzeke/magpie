package magpie.data.utilities.filters;

import java.util.Arrays;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.user.CommandHandler;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Filter entries that contain certain elements. Only works on {@link CompositionDataset}.
 * 
 * <usage><p><b>Usage</b>: &lt;Element names...>
 * <br><pr><i>Element names...</i>: Entries containing these elements pass the filter</usage>
 * @author Logan Ward
 * @version 0.1
 */
public class ContainsElementFilter extends BaseDatasetFilter {
    /** Elements that compounds are not allowed to contain */
    protected String[] ElementList;
    /** List of IDs of those elements */
    protected int[] ExcludedIndex;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            if (Options.length == 0) {
                  throw new IllegalArgumentException();
            }
            setElementList(Options);
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
    }

    /**
     * Define list of elements to use for filter.
     * @param elements List of element abbreviations
     */
    public void setElementList(String[] elements) {
        this.ElementList = elements.clone();
        ExcludedIndex = getElementIndices(elements);
    }

    /**
     * Define a list of elements to use for the filter
     * @param elements Index of the elements
     */
    public void setElementListByIndex(int[] elements) {
        ExcludedIndex = elements.clone();
        ElementList = new String[elements.length];
        for (int i=0; i<elements.length; i++) {
            ElementList[i] = LookupData.ElementNames[elements[i]];
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <Element names...>";
    }
    
    @Override
    public void train(Dataset TrainingSet) {
        /* Nothing to do */
    }

    @Override
    public boolean[] label(Dataset D) {
        // Check input data
        if (! (D instanceof CompositionDataset))
            throw new Error("Dataset must be a CompositionDataset");
        CompositionDataset Data = (CompositionDataset) D;
        
        // Find entries that contain one of those elements
        boolean[] contains = new boolean[D.NEntries()];
        for (int i=0; i<D.NEntries(); i++) {
            contains[i] = false;
            CompositionEntry E = Data.getEntry(i);
            contains[i] = entryContainsElement(E);
        }
        return contains;
    }

    /**
     * Determine whether an entry contains one of the specified elements.
     * @param entry Entry in question
     * @return Whether it contains at least one of the specified elements
     */
    public boolean entryContainsElement(CompositionEntry entry) {
        int[] elems = entry.getElements();
        for (int elem : ExcludedIndex) {
            if (ArrayUtils.contains(elems, elem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given a list of elements, return their indices. 
     * @param elements List of element names to operate on
     * @return Array containing index of each element in elements
     */
    static protected int[] getElementIndices(String[] elements) {
        // Get the index of each element
        int[] ElementIndex = new int[elements.length];
        for (int i=0; i<ElementIndex.length; i++) {
            int index = ArrayUtils.indexOf(LookupData.ElementNames, elements[i]);
            if (index == -1) {
                throw new RuntimeException(elements[i] + " is not a valid element");
            }
            ElementIndex[i] = index;
        }
        return ElementIndex;
    }
    
}
