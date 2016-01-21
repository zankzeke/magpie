package magpie.data.utilities.splitters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionEntry;

/**
 * Splits CompositionDataset based on whether entries contain all metals.
 * <p>
 * All Metals -> 0<br>
 * Any nonmetal constituent -> 1
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class AllMetalsSplitter extends BaseDatasetSplitter {
    /** List indicating which elements are metals */
    final protected boolean[] isMetal = new boolean[] { false,false,true,true,false,
        false,false,false,false,false,true,true,true,false,false,false,false,
        false,true,true,true,true,true,true,true,true,true,true,true,true,true,
        false,true,false,false,false,true,true,true,true,true,true,true,true,
        true,true,true,true,true,true,true,false,false,false,true,true,true,
        true,true,true,true,true,true,true,true,true,true,true,true,true,true,
        true,true,true,true,true,true,true,true,true,true,true,true,true,false,
        false,true,true,true,true,true,true,true,true,true,true,true,true,true,
        true,true,true,true,true,true,true,true,true,true,true,true,true};

    @Override
    public void setOptions(List Options) { /* None to set */ }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    @Override 
    public int[] label(Dataset D) {
        if (! (D.getEntry(0) instanceof CompositionEntry)) 
            throw new Error("Entries must be an instance of CompositionEntry");
        
        int[] output = new int[D.NEntries()];
        for (int i=0; i < D.NEntries(); i++) {
            boolean allMetal = true;
            CompositionEntry E = (CompositionEntry) D.getEntry(i);
            int[] elements = E.getElements();
            for (int j=0; j < elements.length; j++)
                if (! isMetal[elements[j]]) { allMetal = false; break; }
            if (allMetal) output[i] = 0;
            else output[i] = 1;
        }
        
        return output;
    }

    @Override
    public void train(Dataset TrainingSet) { /* Do nothing */ }

    @Override
    public List<String> getSplitNames() {
        List<String> output = new ArrayList<>(2);
        output.add("Only Metals");
        output.add("Contains Nonmetals");
        return output;
    }

    @Override
    protected List<String> getSplitterDetails(boolean htmlFormat) {
        return new LinkedList<>(); // No options
    }
    
}
