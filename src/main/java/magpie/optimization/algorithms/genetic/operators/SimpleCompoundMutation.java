
package magpie.optimization.algorithms.genetic.operators;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionEntry;

/**
 * Mutate a {@linkplain CompositionEntry} that represents a compound by only changing
 *  which elements are present. Assumptions:
 * <ul>
 * <li>The fractions don't matter</li>
 * <li>One element should be permuted, on average</li>
 * <li>No elements should be duplicated</li>
 * <li>Elements should only come from an approved list</li>
 * </ul>
 *
 * @author Logan Ward
 * @version 0.1
 */
public class SimpleCompoundMutation extends BaseMutationFunction {
    /**
     * Elements in the search space
     */
    int[] ElementList;

    @Override
    public void configureFunction(Set<BaseEntry> searchSpace) {
        Set<Integer> elem_set = new TreeSet<>();
        CompositionEntry Entry;
        for (BaseEntry E_ptr : searchSpace) {
            Entry = (CompositionEntry) E_ptr;
            for (int j = 0; j < Entry.getElements().length; j++) {
                elem_set.add(Entry.getElements()[j]);
            }
        }
        ElementList = new int[elem_set.size()];
        Iterator<Integer> iter = elem_set.iterator();
        for (int i = 0; i < ElementList.length; i++) {
            ElementList[i] = iter.next();
        }
    }

    @Override
    public void mutate(BaseEntry Entry_Ptr) {
        CompositionEntry Entry = (CompositionEntry) Entry_Ptr;
        // Get access to the element list of the entry
        int[] new_elem = Entry.getElements();
        // Make it so we expect to mutate one element
        int nelem = Entry.getElements().length;
        double probability = 1.0 / (double) nelem;

        for (int i = 0; i < nelem; i++) {
            if (Math.random() < probability) {
                while (true) {
                    new_elem[i] = ElementList[(int) (Math.random() * ElementList.length)];
                    boolean allClear = true; // Whether the new element is already in the compound
                    for (int j = 0; j < nelem; j++) {
                        if (i != j && new_elem[i] == new_elem[j]) {
                            allClear = false;
                            break;
                        }
                    }
                    if (allClear) break;
                }
            }
        }
        Entry.rectifyEntry(true);
    }
}
