package magpie.data.utilities.modifiers.duplicates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.utilities.modifiers.BaseDatasetModifier;

/**
 * Abstract class for modifiers that remove duplicate entries
 * 
 * @author Logan Ward
 */
abstract public class BaseDuplicateResolver extends BaseDatasetModifier {

    @Override
    protected void modifyDataset(Dataset data) {
        // Prepare a new list of entries
        List<BaseEntry> newEntries = new ArrayList<>(data.NEntries());
        
        // Get the duplicate entries
        for (Map.Entry<BaseEntry,List<BaseEntry>> entryGroup : 
                data.getUniqueEntries().entrySet()) {
            
            if (entryGroup.getValue().size() == 1) {
                // If there is only one entry, add it to output set
                newEntries.add(entryGroup.getKey());
            } else {
                // Otherwise, pick/generate a new one
                newEntries.add(resolveDuplicates(data, entryGroup.getValue()));
            }
        }
        
        // Set the entry list
        data.clearData();
        data.addEntries(newEntries);
    }
    
    /**
     * Given a list of duplicate entries, pick a representative entry or
     * generate a new one
     * @param data Dataset being resolved
     * @param entries List of duplicate entries
     * @return The one true entry
     */
    abstract protected BaseEntry resolveDuplicates(Dataset data, List<BaseEntry> entries);
}
