package magpie.data.utilities.filters;

import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Filter entries from certain phase diagrams. Removes entries that only contain
 * elements from a certain set and those that contain all elements from that set.
 * 
 * <p>Example: When excluding entries from the Al-Ni phase diagram
 * <ol>
 * <li>Al will be removed because it contains Al and no other element
 * <li>AlNi will be removed because it contains both Al and Ni
 * <li>AlZr will not be removed because it contains another element
 * <li>AlNiZr will be removed because it contains both Al and Ni
 * </ol>
 * 
 * <usage><p><b>Usage</b>: &lt;Element names...>
 * <br><pr><i>Element names...</i>: Entries containing these elements pass the filter</usage>
 * @author Logan Ward
 * @version 0.1
 */
public class PhaseDiagramExclusionFilter extends ContainsElementFilter {

    @Override
    public boolean[] label(Dataset D) {
        // Check input data
        if (! (D instanceof CompositionDataset))
            throw new Error("Dataset must be a CompositionDataset");
        CompositionDataset Data = (CompositionDataset) D;
        int[] ExcludedIndex = getElementIndices(ElementList);
        
        boolean[] inDiagram = new boolean[D.NEntries()];
        for (int i=0; i<D.NEntries(); i++) {
            inDiagram[i] = true;
            int[] elems = Data.getEntry(i).getElements();
            
            if (elems.length <= ExcludedIndex.length) {
                // Check if this entry contains only elements from the 
                //  phase diagram in consideration
                for (int elem : elems) {
                    if (! ArrayUtils.contains(ExcludedIndex, elem)) {
                        inDiagram[i] = false;
                        break;
                    }
                }
            } else {
                // Check if this entry contains all of the elements in the diagram
                for (int elem : ExcludedIndex) {
                    if (! ArrayUtils.contains(elems, elem)) {
                        inDiagram[i] = false;
                        break;
                    }
                }
            }
        }
        return inDiagram;
	}
}
