
package magpie.csp.diagramdata;

import magpie.data.materials.CompositionEntry;
import java.util.*;

/**
 * Holds information about a set of ternary phase diagrams
 *
 * @author Logan Ward
 * @deprecated 
 */
public class TernaryPhaseDiagramStatistics extends PhaseDiagramStatistics {
    /**
     * For each ternary, store list of known compounds and their prototype. Elements in
     * the phase diagram are they key, which are mapped to two integers. x[0] is
     * the index of {@link #CommonCompositions} corresponding to the composition
     * of a compound in this diagram, x[1] is the index of the prototype of this compound in the list
     * of prototypes stored for that composition.
     */
    private Map<int[], List<int[]>> PhaseDiagram;
    
    @Override
    protected void processCompounds(Map<CompositionEntry, String> compounds) {
        if (NComponents != 3) {
            throw new Error("This class is only currently implented for DiagramOrder == 3");
        }
        Comparator diagramComparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                int[] list1 = (int[]) o1;
                int[] list2 = (int[]) o2;
                if (list1.length != list2.length) {
                    return Integer.compare(list1.length, list2.length);
                } else {
                    int c;
                    for (int i = 0; i < list1.length; i++) {
                        c = Integer.compare(list1[i], list2[i]);
                        if (c != 0) {
                            return c;
                        }
                    }
                    return 0;
                }
            }
        };
        PhaseDiagram = new TreeMap<>(diagramComparator);
        for (int i = 0; i < NElements; i++) {
            for (int j = 0; j < NElements; j++) {
                if (j == i) {
                    continue;
                }
                for (int k = 0; k < NElements; k++) {
                    if (k == i || k == j) {
                        continue;
                    }
                    PhaseDiagram.put(new int[]{i, j, k}, new LinkedList<int[]>());
                }
            }
        }
        // Add all compounds to all pertainent phase diagrams
        for (Map.Entry<CompositionEntry, String> entry : compounds.entrySet()) {
            CompositionEntry compositionEntry = entry.getKey();
            String prototype = entry.getValue();
            addCompoundToPhaseDiagrams(compositionEntry, prototype);
        }
    }

    @Override
    protected void calculateStructureProbabilities() {
        // Probability that each compound (or the absence of one) appears at
        //  each composition for a random phase diagram
        int[][] compoundHitCount = new int[CommonCompositions.size()][];
        for (int i = 0; i < CommonCompositions.size(); i++) {
            compoundHitCount[i] = new int[CommonCompositions.get(i).getRight().size() + 1];
        }
        for (int[] diagram : PhaseDiagram.keySet()) {
            int[] compoundVector = getCompoundVector(diagram);
            for (int i = 0; i < compoundVector.length; i++) {
                compoundHitCount[i][compoundVector[i]]++;
            }
        }
        StructureProbability = new double[compoundHitCount.length][];
        for (int i = 0; i < CommonCompositions.size(); i++) {
            StructureProbability[i] = new double[compoundHitCount[i].length];
            for (int j = 0; j < StructureProbability[i].length; j++) {
                StructureProbability[i][j] = ((double) compoundHitCount[i][j] + 1.0 / StructureProbability[i].length) / PhaseDiagram.size();
            }
        }
    }

    /**
     * Add compound to all relevant ternary phase diagrams
     * @param compound Compound to be added
     */
    private void addCompoundToPhaseDiagrams(CompositionEntry compound, String Prototype) {
        int[] e = compound.getElements();
        if (e.length == 1) {
            addUnaryCompoundToPhaseDiagrams(compound, Prototype);
        }
        if (e.length == 2) {
            addBinaryCompoundToPhaseDiagrams(compound, Prototype);
        } else if (e.length == 3) {
            addTernaryCompoundToPhaseDiagram(compound, Prototype);
        }
    }

    private void addUnaryCompoundToPhaseDiagrams(CompositionEntry compound, String Prototype) {
        int e = compound.getElements()[0];
        for (Map.Entry<int[], List<int[]>> entry : PhaseDiagram.entrySet()) {
            int[] id = entry.getKey();
            List<int[]> phaseDiagram = entry.getValue();
            if (id[0] == e) {
                addCompoundToPhaseDiagram(phaseDiagram, new double[]{1.0, 0, 0}, Prototype);
            } else if (id[1] == e) {
                addCompoundToPhaseDiagram(phaseDiagram, new double[]{0, 1.0, 0}, Prototype);
            } else if (id[2] == e) {
                addCompoundToPhaseDiagram(phaseDiagram, new double[]{0, 0, 1.0}, Prototype);
            }
        }
    }

    /**
     * Add a binary compound to any phase diagram with both elements
     * @param compound Compound to be added
     * @param Prototype Name of prototype for this compound
     */
    private void addBinaryCompoundToPhaseDiagrams(CompositionEntry compound, String Prototype) {
        int[] e = compound.getElements();
        double[] f = compound.getFractions();
        for (Map.Entry<int[], List<int[]>> entry : PhaseDiagram.entrySet()) {
            int[] id = entry.getKey();
            List<int[]> phaseDiagram = entry.getValue();
            if (id[0] == e[0]) {
                if (id[1] == e[1]) {
                    addCompoundToPhaseDiagram(phaseDiagram, new double[]{f[0], f[1], 0}, Prototype);
                }
                if (id[2] == e[1]) {
                    addCompoundToPhaseDiagram(phaseDiagram, new double[]{f[0], 0, f[1]}, Prototype);
                }
            } else if (id[1] == e[0]) {
                if (id[0] == e[1]) {
                    addCompoundToPhaseDiagram(phaseDiagram, new double[]{f[1], f[0], 0}, Prototype);
                }
                if (id[2] == e[1]) {
                    addCompoundToPhaseDiagram(phaseDiagram, new double[]{0, f[0], f[1]}, Prototype);
                }
            } else if (id[2] == e[0]) {
                if (id[0] == e[1]) {
                    addCompoundToPhaseDiagram(phaseDiagram, new double[]{f[1], 0, f[0]}, Prototype);
                }
                if (id[1] == e[1]) {
                    addCompoundToPhaseDiagram(phaseDiagram, new double[]{0, f[1], f[0]}, Prototype);
                }
            }
        }
    }

    /**
     * Add a ternary compound to each phase diagram that contains all three elements
     * @param compound Compound to add to diagram
     * @param Prototype
     */
    private void addTernaryCompoundToPhaseDiagram(CompositionEntry compound, String Prototype) {
        int[] e = compound.getElements();
        double[] f = compound.getFractions();
        addCompoundToPhaseDiagram(PhaseDiagram.get(new int[]{e[0], e[1], e[2]}), f, Prototype); // ABC
        addCompoundToPhaseDiagram(PhaseDiagram.get(new int[]{e[0], e[2], e[1]}), new double[]{f[0], f[2], f[1]}, Prototype); // ACB
        addCompoundToPhaseDiagram(PhaseDiagram.get(new int[]{e[1], e[0], e[2]}), new double[]{f[1], f[0], f[2]}, Prototype); // BAC
        addCompoundToPhaseDiagram(PhaseDiagram.get(new int[]{e[1], e[2], e[0]}), new double[]{f[1], f[2], f[0]}, Prototype); // BCA
        addCompoundToPhaseDiagram(PhaseDiagram.get(new int[]{e[2], e[1], e[0]}), new double[]{f[2], f[1], f[0]}, Prototype); // CBA
        addCompoundToPhaseDiagram(PhaseDiagram.get(new int[]{e[2], e[0], e[1]}), new double[]{f[2], f[0], f[1]}, Prototype); // CBA
    }

    /**
     * Adds a compound based on its composition and structure type to a specific diagram. First finds
     *  the corresponding composition bin, then adds the prototype to the list of known prototypes. Once
     *  this is complete, stores index of bin and prototype within that bin to the phaseDiagram.
     * @param phaseDiagram List of compounds in the diagram. For eacn entry, x[0] is the corresponding
     *  entry in {@link #CommonCompositions}
     * @param composition Composition of compound
     * @param prototype Name of prototype
     */
    private void addCompoundToPhaseDiagram(List<int[]> phaseDiagram, double[] composition, String prototype) {
        // --> Find the composition bin closest to this composition
        double closestDist = 1e100;
        int compositionIndex = -1;
        for (int i = 0; i < CommonCompositions.size(); i++) {
            double[] thisComposition = CommonCompositions.get(i).getKey();
            double totalDist = Math.abs(thisComposition[0] - composition[0]) + Math.abs(thisComposition[1] - composition[1]) + Math.abs(thisComposition[2] - composition[2]);
            if (totalDist < closestDist) {
                closestDist = totalDist;
                compositionIndex = i;
                if (totalDist < MinDistance) {
                    break;
                }
            }
        }
        // --> For that bin, see if this compound is in it
        int prototypeIndex = CommonCompositions.get(compositionIndex).getValue().indexOf(prototype);
        if (prototypeIndex == -1) {
            prototypeIndex = CommonCompositions.get(compositionIndex).getValue().size();
            CommonCompositions.get(compositionIndex).getValue().add(prototype);
        }
        // --> Store compound info in phase diagram
        phaseDiagram.add(new int[]{compositionIndex, prototypeIndex});
    }

    @Override
    protected int[] getCompoundVector(int[] sites) {
        List<int[]> phaseDiagram = PhaseDiagram.get(sites);
        int[] output = new int[CommonCompositions.size()];
        for (int[] compound : phaseDiagram) {
            output[compound[0]] = compound[1] + 1;
        }
        return output;
    }

    /**
     * Increment a counter that iterates through all combinations of elements in a phase diagram.
     * <br>For a ternary system, it goes A->B->C for unary, AB->AC->BC for binary, ABC for ternary
     * <br>For a quaternary, it goes A->B->C for unary, AB->AC->AD->BC->BD->CD for binary, and so on...
     * @param counter Counter to be incremented
     * @return Whether it iterator is done
     */
    protected boolean incrementCounter(int[] counter) {
        if (counter.length == 1) {
            counter[0]++;
            return counter[0] == NComponents;
        } else {
            int[] subCounter = Arrays.copyOfRange(counter, 1, counter.length);
            boolean isDone = incrementCounter(subCounter);
            if (isDone) {
                counter[0]++;
                for (int i = 1; i < counter.length; i++) {
                    counter[i] = counter[i - 1] + 1;
                    isDone = counter[i] == NComponents;
                }
            } else {
                System.arraycopy(subCounter, 0, counter, 1, subCounter.length);
            }
            return isDone;
        }
    }
}
