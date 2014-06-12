/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.csp.diagramdata;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import magpie.data.materials.CompositionEntry;
import magpie.utility.DistinctPermutationGenerator;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Kept only as backup. {@linkplain OnTheFlyPhaseDiagramStatistics} is much faster. 
 * This one is not even fully functional. Still slightly off from {@linkplain TernaryPhaseDiagramStatistics}, 
 * the gold standard and horribly slow.
 *
 * @author Logan Ward
 * @deprecated 
 */
public class PrecomputedPhaseDiagramStatistics extends PhaseDiagramStatistics {
    /**
     * For each phase diagram, store list of known compounds and their prototype. Elements in the phase diagram are they key, which are mapped to two integers. x[0] is the index of
     * {@link #CommonCompositions} corresponding to the composition of a compound in this diagram, x[1] is the index of the prototype of this compound in the list of prototypes
     * stored for that composition.
     */
    private SortedMap<int[], List<int[]>> PhaseDiagram;
    
    /**
     * Precomputed permutations for list of a certain number of entries.
     * <br>0 -> 0; 1 -> 01, 10; 2 -> 012, 021, 021, ...
     */
    private List<Set<int[]>> permutations;

    @Override
    protected void processCompounds(Map<CompositionEntry, String> compounds) {
        // --> Precompute permuations
        permutations = new LinkedList<>();
        for (int l=1; l<=NComponents; l++) {
            int[] temp = new int[l];
            for (int i=1; i<l; i++) temp[i] = i;
            permutations.add(DistinctPermutationGenerator.generatePermutations(temp));
        }
        
        // --> Create the PhaseDiagram map
        Comparator diagramComparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                int[] listA = (int[]) o1;
                int[] listB = (int[]) o2;
                int c;
                
                // Find the minimum value in each list
                int minA = listA[0], minB = listB[0];
                for (int i=1; i<listA.length; i++) {
                    if (listA[i] < minA) minA = listA[i];
                    if (listB[i] < minB) minB = listB[i];
                }
                
                // Sort by it first
                c = Integer.compare(minA, minB);
                if (c != 0) return c;
                
                // Next, sort lexigraphically
                for (int i=0; i<listA.length; i++) {
                    c = Integer.compare(listA[i], listB[i]);
                    if (c != 0) {
                        return c;
                    }
                }
                return 0;
            }
        };
        PhaseDiagram = new TreeMap<>(diagramComparator);
        // --> Add all combinations of all elements
        int[] phaseDiagram = new int[NComponents];
        while (true) {
            // Add to list
            boolean anyEqual = false;
            for (int i=0; i<NComponents; i++) {
                for (int j=i+1; j<NComponents; j++) {
                    if (phaseDiagram[i] == phaseDiagram[j]) {
                        anyEqual = true; break;
                    }
                }
                if (anyEqual) break;
            }
            if (! anyEqual)
                PhaseDiagram.put(phaseDiagram.clone(), new LinkedList<int[]>());
            // Increment
            boolean allDone = true;
            for (int i=0; i<NComponents; i++) {
                phaseDiagram[i]++;
                if (phaseDiagram[i] == NElements) {
                    phaseDiagram[i] = 0;
                } else {
                    allDone = false;
                    break;
                }
            }
            if (allDone) break;
        }
        
        // Add all compounds to all pertainent phase diagrams
        for (Map.Entry<CompositionEntry, String> entry : compounds.entrySet()) {
            CompositionEntry compositionEntry = entry.getKey();
            String prototype = entry.getValue();
            addCompoundToPhaseDiagrams(compositionEntry, prototype);
        }
    }

    /**
     * Calculate the probability of each prototype appearing in a phase diagram
     */
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
     *
     * @param compound Compound to be added
     */
    private void addCompoundToPhaseDiagrams(CompositionEntry compound, String Prototype) {
        // --> Get composition
        int[] elems = compound.getElements();
        double[] fracs = compound.getFractions();
        
        if (elems.length > NComponents) { // No matching diagram, do nothing
        } else if (elems.length == NComponents) {
            // --> Special case: If have the same number of elements in diagram as 
            //     in each phase diagram, can look up the diagrams directly 
            int[] components = new int[NComponents];
            double[] composition = new double[NComponents];
            for (int[] order : permutations.get(NComponents - 1)) {
                for (int i=0; i<NComponents; i++) {
                    components[i] = elems[order[i]];
                    composition[i] = fracs[order[i]];
                }
                List<int[]> diagram = PhaseDiagram.get(components);
                addCompoundToPhaseDiagram(diagram, composition, Prototype);
            }
        } else {
            // --> General Case: Have to add the compound to all phase diagrams that
            //     contain this compound (can be many)
            
            // --> Get only phase diagrams that are sure to carry this element
            // Since the list is sorted first by the minimum Z of any element in the diagram,
            //  no phase diagram after one containing all of the maximum Z in this 
            //  compound + 1 will contain any of its constituents
            int[] startKey = new int[NComponents];
            int maxZ = elems[0];
            for (int i=1; i<elems.length; i++) if (elems[i]<maxZ) maxZ = elems[i];
            int[] endKey = new int[NComponents]; Arrays.fill(endKey, maxZ+1);
            Map<int[],List<int[]>> relevantDiagrams = PhaseDiagram.subMap(startKey, endKey);
            
            // --> For each arrangment of each entry
            for (int[] order : permutations.get(elems.length - 1)) {
                double[] composition = new double[NComponents];
                int hits = 0;
                // --> For each relevant diagram
                for (Map.Entry<int[], List<int[]>> diagram : relevantDiagrams.entrySet()) {
                    // Map the elements in this digram to elements in the phase diagram
                    int[] components = diagram.getKey();
                    Arrays.fill(composition, 0.0);
                    boolean allFound = true;
                    for (int i=0; i<elems.length; i++) {
                        int index = ArrayUtils.indexOf(components, elems[order[i]]);
                        if (index == -1) {
                            allFound = false;
                            break;
                        } else {
                            composition[i] = fracs[order[i]];
                        }
                    }
                    if (!allFound) continue; // If every element in the list is not containeed
                    hits++;
                    // If that diagram works, add the compound
                    addCompoundToPhaseDiagram(diagram.getValue(), composition, Prototype);
                }
                hits=0;
            }
        }
    }

    /**
     * Adds a compound based on its composition and structure type to a specific diagram. First finds the corresponding composition bin, then adds the prototype to the list of
     * known prototypes. Once this is complete, stores index of bin and prototype within that bin to the phaseDiagram.
     *
     * @param phaseDiagram List of compounds in the diagram. For eacn entry, x[0] is the corresponding entry in {@link #CommonCompositions}
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

