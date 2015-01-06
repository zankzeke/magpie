package magpie.data.utilities.splitters;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.user.CommandHandler;

/**
 * Split dataset into two groups based on whether it contains certain elements. Any
 *  compound that contains a listed element is placed in set 0, all others in set 1.
 * 
 * <usage><p><b>Usage</b>: &lt;Element names...>
 * <br><pr><i>Element names...</i>: Entries containing these elements will be split into a distinct set</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class SingleElementGroupSplitter extends BaseDatasetSplitter {
    /** List of elements that define a subclass */
    final private List<String> Elements = new LinkedList<>();

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        if (Options.length == 0)
            throw new Exception(printUsage());
        Elements.clear();
        Elements.addAll(Arrays.asList(Options));
    }

    @Override
    public String printUsage() {
        return "Usage: <Element names...>";
    }

    @Override
    public void train(Dataset TrainingSet) {
        /* Nothing to train */
    }

    @Override
    public int[] label(Dataset D) {
        if (! (D instanceof CompositionDataset))
            throw new Error("Dataset must be a CompositionDataset.");
        int[] output = new int[D.NEntries()];
        CompositionDataset Ptr = (CompositionDataset) D;
        // Get a list of integers describing each element
        List<Integer> ElementIndices = getElementIndices(Elements, Ptr);
        for (int i=0; i<D.NEntries(); i++) {
            CompositionEntry E = Ptr.getEntry(i);
            boolean hasElement = false;
            for (Integer ElementIndex : ElementIndices) {
                if (E.getElementFraction(ElementIndex) > 0) {
                    hasElement = true; break;
                }
            }
            output[i] = hasElement ? 0 : 1;
        }
        return output;
    }
    
    /**
     * Given a CompositionDataset, return the index corresponding to each element in a list
     *  of element names
     * @param Data
     * @return 
     */
    static protected List<Integer> getElementIndices(List<String> ElementNames, 
            CompositionDataset Data) {
        List<Integer> output = new LinkedList<>();
        List<String> LookupList = Arrays.asList(Data.ElementNames);
        for (int i=0; i<ElementNames.size(); i++) {
            int index = LookupList.indexOf(ElementNames.get(i));
            if (index == -1) throw new Error("Element " + ElementNames.get(i) + " not recognized.");
            output.add(index);
        }
        return output;
    }
    
}
