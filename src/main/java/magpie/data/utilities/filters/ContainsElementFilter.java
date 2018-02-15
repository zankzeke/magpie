package magpie.data.utilities.filters;

import java.util.Arrays;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.user.CommandHandler;

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
    private int[] ExcludedIndex;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            if (Options.length == 0) {
                  throw new Exception();
            }
            setElementList(Options);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    /**
     * Define list of elements to use for filter.
     * @param ElementList List of element abbreviations
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
        for (int j=0; j<ElementList.length; j++) {
            if (entry.getElementFraction(ExcludedIndex[j]) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given a list of elements, return their indices. 
     * @param ElementList List of element names to operate on
     * @return Array containing index of each element in ElementList
     */
    static protected int[] getElementIndices(String[] ElementList) {
        // Get the index of each element
        int[] ElementIndex = new int[ElementList.length];
        List<String> ElementNamesAsList = Arrays.asList(LookupData.ElementNames);
        for (int i=0; i<ElementIndex.length; i++) {
            int index = ElementNamesAsList.indexOf(ElementList[i]);
            if (index == -1) throw new Error(ElementList[i] + " is not a valid element");
            ElementIndex[i] = index;
        }
        return ElementIndex;
    }
    
}
