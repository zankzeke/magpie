package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Filter entries based on whether all constituents are metals. Only works on 
 * data that fills the {@link CompositionDataset}.
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * @author Logan Ward
 * @version 0.1
 */
public class AllMetalsFilter extends BaseDatasetFilter {
    /** List indicating which elements are metals */
    protected boolean[] isMetal = new boolean[] { false,false,true,true,false,
        false,false,false,false,false,true,true,true,false,false,false,false,
        false,true,true,true,true,true,true,true,true,true,true,true,true,true,
        false,true,false,false,false,true,true,true,true,true,true,true,true,
        true,true,true,true,true,true,true,false,false,false,true,true,true,
        true,true,true,true,true,true,true,true,true,true,true,true,true,true,
        true,true,true,true,true,true,true,true,true,true,true,true,true,false,
        false,true,true,true,true,true,true,true,true,true,true,true,true,true,
        true,true,true,true,true,true,true,true,true,true,true,true,true};

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        /* Nothing to do */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options to set*";
    }

    @Override
    public void train(Dataset TrainingSet) {
        /* Nothing to do */
    }

    @Override
    public boolean[] label(Dataset D) {
        boolean allMetals[] = new boolean[D.NEntries()];
        if (! (D.getEntry(0) instanceof CompositionEntry)) 
            throw new Error("Entries must be an instance of CompositionEntry");
        for (int e=0; e<D.NEntries(); e++) {
            allMetals[e] = true;
            CompositionEntry E = (CompositionEntry) D.getEntry(e);
            int[] Elements = E.getElements();
            for (int el=0; el<Elements.length; el++)
                if (! isMetal[Elements[el]]) { allMetals[e] = false; break; }
        }
        return allMetals;
    }
}
