/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.algorithms.genetic.operators;

import java.util.Arrays;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;

/**
 * CrossoverFunction designed to deal with CompositionEntry that represent a
 * compound with a fixed number of sites that must contain different elements.
 * Assumptions that it makes:
 * <p>
 * <ul>
 * <li> Each entry has the same number of sites </li>
 * <li> Order can be randomly assigned </li>
 * <li> Elements cannot be duplicated </li>
 * </ul>
 *
 * @author Logan Ward
 * @version 1.0
 */
public class CompoundCrossover extends BaseCrossoverFunction {

    @Override
    public BaseEntry crossover(BaseEntry A_ptr, BaseEntry B_ptr) {
        CompositionEntry A = (CompositionEntry) A_ptr,
                B = (CompositionEntry) B_ptr, C;
        int nelem = A.getElements().length;
        int[] elem_choice = new int[nelem];
        int index = 0, to_add;
        while (index < nelem) {
            // Randomly pick an elem from either compound
            if (Math.random() > 0.5) {
                to_add = A.getElements()[(int) (Math.random() * nelem)];
            } else {
                to_add = B.getElements()[(int) (Math.random() * nelem)];
            }
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
            elem_choice[index] = to_add;
            index++;
        }
        // Build a new entry
        C = new CompositionEntry(elem_choice, Arrays.copyOf(A.getFractions(), nelem),
                A.getElementNameList(), A.getSortingOrder());
		C.setAttributes(A.getAttributes());
        return C;
    }
}
