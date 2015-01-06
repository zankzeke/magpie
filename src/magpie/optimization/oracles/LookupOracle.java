package magpie.optimization.oracles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * BaseOracle that looks up measured class properties from a list of already-solved entries.
 * 
 * <usage><p><b>Usage</b>: $&lt;Dataset>
 * <br><pr><i>Dataset</i>: {@linkplain Dataset} used to look up results from</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class LookupOracle extends BaseOracle {
    /** Map that links a Dataset entry to one with measured properties */
    protected Map<BaseEntry,BaseEntry> LookupTable = null;

    @Override
    public void setOptions(List Options) throws Exception {
        try {
            setLookupTable((Dataset) Options.get(0));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<dataset>";
    }
    
    /**
     * Define the lookup table used by this oracle. Must contain every entry that you
     * expect to see
     * @param Data Dataset containing representative entries
     */
    public void setLookupTable(Dataset Data) {
        LookupTable = new TreeMap<>();
        for (int i=0; i<Data.NEntries(); i++)
            LookupTable.put(Data.getEntry(i),Data.getEntry(i));
    }
    
    @Override protected void evaluate_protected() {
        List<BaseEntry> new_entries = new ArrayList<>(ToEvaluate.NEntries());
        BaseEntry Entry;
        for (int i=0; i<ToEvaluate.NEntries(); i++) {
            Entry = LookupTable.get(ToEvaluate.getEntry(i));
            if (Entry != null)
                new_entries.add(Entry);
            else 
                throw new Error("Lookup table missing entry: "+ToEvaluate.getEntry(i));
        }
        ToEvaluate.clearData();
        ToEvaluate.addEntries(new_entries);
    }
    
    @Override public LookupOracle clone() {
        LookupOracle x = (LookupOracle) super.clone();
        x.LookupTable = new TreeMap<>();
        x.LookupTable.putAll(LookupTable);
        return x;
    }
}
