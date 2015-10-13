package magpie.data.utilities.splitters;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.user.CommandHandler;

/**
 * Splits entries into multiple groups depending on which elements they contain.
 * All entries containing the first set of elements are assigned into the first group.
 * Remaining entries that contain elements in the second set are assigned into the second group. 
 * This process proceeds until all entries have been assigned, or all element groups 
 * have been treated.
 * 
 * <usage><p><b>Usage</b>: "&lt;Element set 1...>" ["&lt;Element set 2...>"] [...]
 * <br><pr><i>Element set N</i>: Any compound containing elements from this list (and none from any previous set) will be assigned to this set</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class MultipleElementGroupsSplitter extends SingleElementGroupSplitter {
    /** List of element groups */
    List<String[]> ElementGroups = new LinkedList<>();

    @Override
    public MultipleElementGroupsSplitter clone() {
        MultipleElementGroupsSplitter x = 
                (MultipleElementGroupsSplitter) super.clone();
        x.ElementGroups = new LinkedList<>();
        for (String[] group : ElementGroups) {
            x.ElementGroups.add(group.clone());
        }
        return x;
    }

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        clearElementGroups();
        if (Options.length == 0) 
            throw new Exception(printUsage());
        for (String option : Options) {
            addElementGroup(option);
        }
    }
    
    /**
     * Clear the current list of element groups
     */
    public void clearElementGroups() {
        ElementGroups.clear();
    }
    
    /**
     * Add a list of elements to be defined as the same group.
     * @param elems Element symbols separated by spaces
     */
    public void addElementGroup(String elems) {
        addElementGroup(Arrays.asList(elems.split(" ")));
    }    
    /**
     * Add a list of elements to be treated as a single group.
     * @param elems List of elements to define as a group
     */
    public void addElementGroup(Collection<String> elems) {
        ElementGroups.add(elems.toArray(new String[0]));
    }

    @Override
    public String printUsage() {
        return "Usage: \"<Element set 1...>\" [\"<Element set 2...>\"] [...]";
    }
    
    @Override
    public int[] label(Dataset D) {
        if (! (D instanceof CompositionDataset))
            throw new Error("Dataset must be a CompositionDataset.");
        int[] output = new int[D.NEntries()];
        CompositionDataset Ptr = (CompositionDataset) D;
        // Get a list of integers describing each element group
        List<List<Integer>> ElementIndices = new LinkedList<>();
        for (String[] ElementGroup : ElementGroups) {
            ElementIndices.add(getElementIndices(Arrays.asList(ElementGroup), Ptr));
        }
        // Sort the entries
        for (int e=0; e<D.NEntries(); e++) {
            CompositionEntry E = Ptr.getEntry(e);
            boolean wasAssigned = false;
            for (int g=0; g < ElementGroups.size(); g++) 
                for (int el=0; el<ElementIndices.get(g).size(); el++)
                    if (E.getElementFraction(ElementIndices.get(g).get(el)) > 0) {
                        wasAssigned = true; output[e] = g; break;
                    }
            if (! wasAssigned) output[e] = ElementGroups.size();
        }
        return output;
    }

    @Override
    protected List<String> getSplitterDetails(boolean htmlFormat) {
        List<String> output = new LinkedList<>();
        
        for (int i=0; i<ElementGroups.size(); i++) {
            String line = "Group " + i + ":";
            for (String elem : ElementGroups.get(i)) {
                line += " " + elem;
            }
            output.add(line);
        }
        
        return output;
    }

    @Override
    public List<String> getSplitNames() {
        List<String> output = new LinkedList<>();
        
        for (int i=0; i<ElementGroups.size(); i++) {
            String line = "Contains ";
            for (String elem : ElementGroups.get(i)) {
                line += " " + elem;
            }
            output.add(line);
        }
        
        output.add("Otherwise");

        return output;
    }
}
