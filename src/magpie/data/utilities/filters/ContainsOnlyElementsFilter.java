package magpie.data.utilities.filters;

import java.util.Arrays;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.user.CommandHandler;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Filter entries that contain only certain elements. Only works on {@link CompositionDataset}.
 * Consider this a way to remove any entries from a certain phase diagram from 
 * a dataset.
 * 
 * <usage><p><b>Usage</b>: &lt;Element names...>
 * <br><pr><i>Element names...</i>: Entries containing these elements pass the filter</usage>
 * @author Logan Ward
 * @version 0.1
 */
public class ContainsOnlyElementsFilter extends ContainsElementFilter {

    @Override
    protected boolean[] label(Dataset D) {
        // Check input data
        if (! (D instanceof CompositionDataset))
            throw new Error("Dataset must be a CompositionDataset");
        CompositionDataset Data = (CompositionDataset) D;
        int[] ExcludedIndex = getElementIndices(ElementList);
        
        // Find entries that contain one of those elements
        boolean[] containsOnly = new boolean[D.NEntries()];
        for (int i=0; i<D.NEntries(); i++) {
            containsOnly[i] = true;
            int[] elems = Data.getEntry(i).getElements();
            for (int elem : elems) {
				if (! ArrayUtils.contains(ExcludedIndex, elem)) {
					containsOnly[i] = false;
					break;
				}
			}
        }
        return containsOnly;
	}
}
