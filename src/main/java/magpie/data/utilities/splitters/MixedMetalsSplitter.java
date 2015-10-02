package magpie.data.utilities.splitters;

import java.util.ArrayList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionEntry;

/**
 * This class splits a CompositionDataset into three subsets: all metals (0), no metals (1), mixture (2)
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class MixedMetalsSplitter extends AllMetalsSplitter {

    @Override
    public int[] label(Dataset D) {
        if (! (D.getEntry(0) instanceof CompositionEntry)) 
            throw new Error("Entries must be an instance of CompositionEntry");
        
        int[] output = new int[D.NEntries()];
        for (int i=0; i < D.NEntries(); i++) {
            CompositionEntry E = (CompositionEntry) D.getEntry(i);
            int[] elements = E.getElements();
            output[i] = isMetal[elements[0]] ? 0 : 1;
            for (int j=0; j < elements.length; j++)
                if (isMetal[elements[j]] != isMetal[elements[0]]) { output[i] = 2; break; }
        }
        return output;
    }

    @Override
    public List<String> getSplitNames() {
        List<String> output = new ArrayList<>(2);
        output.add("Only Metals");
        output.add("Only Nonmetals");
        output.add("Metals and Nonmetals");
        return output;
    }
    
}
