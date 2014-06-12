/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.filters;

import java.util.Arrays;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
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

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            if (Options.length == 0) throw new Exception();
            ElementList = Arrays.copyOf(Options, Options.length);
        } catch (Exception e) {
            throw new Exception(printUsage());
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
    protected boolean[] label(Dataset D) {
        // Check input data
        if (! (D instanceof CompositionDataset))
            throw new Error("Dataset must be a CompositionDataset");
        CompositionDataset Data = (CompositionDataset) D;
        int[] ExcludedIndex = getElementIndices(ElementList, Data);
        
        // Find entries that contain one of those elements
        boolean[] contains = new boolean[D.NEntries()];
        for (int i=0; i<D.NEntries(); i++) {
            contains[i] = false;
            CompositionEntry E = Data.getEntry(i);
            for (int j=0; j<ElementList.length; j++)
                if (E.getElementFraction(ExcludedIndex[j]) > 0) {
                    contains[i] = true; break;
                }           
        }
        return contains;
    }

    /**
     * Given a list of elements, return their indices. Uses element list from a CompositionDataset
     * @param ElementList List of element names to operate on
     * @param Data Dataset containing list of element names
     * @return Array containing index of each element in ElementList
     */
    static protected int[] getElementIndices(String[] ElementList, CompositionDataset Data) {
        // Get the index of each element
        int[] ElementIndex = new int[ElementList.length];
        List<String> ElementNamesAsList = Arrays.asList(Data.ElementNames);
        for (int i=0; i<ElementIndex.length; i++) {
            int index = ElementNamesAsList.indexOf(ElementList[i]);
            if (index == -1) throw new Error(ElementList[i] + " is not a valid element");
            ElementIndex[i] = index;
        }
        return ElementIndex;
    }
    
}
