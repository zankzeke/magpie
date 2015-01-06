
package magpie.optimization.algorithms.genetic.operators;

import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Crossover function for the Heusler Dataset
 *
 * @author Logan Ward
 */
public class HeuslerCrossover extends BaseCrossoverFunction {
    @Override
    public BaseEntry crossover(BaseEntry A_ptr, BaseEntry B_ptr) {
        CompositionEntry A = (CompositionEntry) A_ptr,
                B = (CompositionEntry) B_ptr, C;
        int nelem = A.getElements().length;
        int[] elem_choice = new int[nelem];
        double[] composition = new double[]{0.5, 0.25, 0.25};

        // Deal with the A site
        int a_asite = ArrayUtils.indexOf(A.getFractions(), 0.5),
                b_asite = ArrayUtils.indexOf(B.getFractions(), 0.5);
        elem_choice[0] = Math.random() > 0.5 ? A.getElements()[a_asite]
                : B.getElements()[b_asite];

        // Deal with the B sites
        int[] candidates = new int[nelem * 2 - 2];
        int index = 0;
        while (index < candidates.length) {
            for (int i = 0; i < nelem; i++) {
                if (i != a_asite) {
                    candidates[index++] = A.getElements()[i];
                }
                if (i != b_asite) {
                    candidates[index++] = B.getElements()[i];
                }
            }
        }

        index = 1;
        int to_add;
        while (index < nelem) {
            // Randomly pick an elem from either compound
            to_add = candidates[(int) (Math.random() * (double) candidates.length)];
            // Check whether it is already in the compound
            for (int j = 0; j < index; j++) {
                if (elem_choice[j] == to_add) {
                    to_add = -1;
                    break;
                }
            }
            if (to_add == -1) {
                continue; // If so, try again
            }                    // Add it to the compound
            elem_choice[index++] = to_add;
        }
        // Build a new entry
        C = new CompositionEntry(elem_choice, composition);
        return C;
    }
}
