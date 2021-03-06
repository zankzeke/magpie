
package magpie.csp.diagramdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.utility.DistinctPermutationGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Calculate phase diagrams as requested. Stores a list of known compounds in 
 *  such a way that pertinent compounds can be retrieved quickly.
 * @author Logan Ward
 */
public class OnTheFlyPhaseDiagramStatistics extends PhaseDiagramStatistics {
    /** Lookup tables for elements compound with 1, 2, 3, ... components. Each
     * element of this list contains a sorted map that links each known compound's
     * composition (rounded to exactly match a composition from the {@linkplain #CommonCompositions} 
     * list to the index of the prototype structure of that compound. Uses a sorted
     * map so that one can lookup all compounds in a particular system quickly.
     */
    private List<SortedMap<Pair<int[],double[]>, Integer>> CompoundLookup;
    /**
     * Copy of {@linkplain #CommonCompositions} used to enable faster lookup from
     * {@linkplain #getCompositionIndex(double[])}.
     */
    private List<double[]> CommonCompositionCopy;
    /**
     * Comparator for {@linkplain #CommonCompositionCopy}. Must be keep up-to-date with
     *  {@linkplain #CompositionComparator}.
     */
    private final Comparator<double[]> CompositionComparatorCopy;
    /**
     * Stores the possible choices of elements when running {@linkplain #getCompoundVector(int[]) }.
     *   Cached to avoid re-generating it
     */
    private final List<List<int[]>> ElemChoices = new ArrayList<>(6);

    public OnTheFlyPhaseDiagramStatistics() {
        this.CompositionComparatorCopy = new Comparator<double[]>() {
            @Override
            public int compare(double[] comp1, double[] comp2) {
                int c;
                for (int i = 0; i < comp1.length; i++) {
                    c = Double.compare(comp1[i], comp2[i]);
                    if (c != 0) {
                        return c;
                    }
                }
                return 0;
            }
        };
    }

    @Override
    protected void processCompounds(Map<CompositionEntry, String> compounds) {
        // --> Copy the common compositions, for fast lookup
        CommonCompositionCopy = new ArrayList<>(CommonCompositions.size());
        for (Pair<double[], List<String>> pair : CommonCompositions) {
                CommonCompositionCopy.add(pair.getKey());
        }

        // --> Create an appropriate sorted map
        Comparator compoundSorter = new Comparator<Pair<int[], double[]>>() {
            @Override
            public int compare(Pair<int[],double[]> A, Pair<int[],double[]> B) {
                // First, sort by element list
                int c;
                for (int i=0; i<A.getKey().length; i++) {
                    c = Integer.compare(A.getKey()[i],B.getKey()[i]);
                    if (c != 0) return c;
                }
                // Next, sort by fractions
                for (int i=0; i<A.getKey().length; i++) {
                    c = Double.compare(A.getValue()[i],B.getValue()[i]);
                    if (c != 0) return c;
                }
                return 0;
            }
        };

        // --> Create lookup tables for unary, binary, ..., NComponent-ary compounds
        CompoundLookup = new ArrayList<>(NComponents);
        for (int i=1; i<=NComponents; i++) {
            CompoundLookup.add(new TreeMap<Pair<int[],double[]>, Integer>(compoundSorter));
        }
        
        // --> Add each compound to the appropriate lookup table
        for (Map.Entry<CompositionEntry, String> entry : compounds.entrySet()) {
            // Sort the elements in appropriate order
            CompositionEntry composition = entry.getKey();
            int[] elems = composition.getElements();
            double[] fracs = composition.getFractions();

            // Skip if more components than NComponents
            if (elems.length > NComponents) continue;

            // Sort such that elements are in ascending order
            orderComposition(elems, fracs);

            // Round composition such that it is *exactly* equal to the nearest bin
            //  in CommonCompositions. Also, match the prototype with that bin
            String prototype = entry.getValue();
            int prototypeID = roundCompositionAndMatchStructure(fracs, prototype);

            // Add to the appropriate map
            CompoundLookup.get(elems.length - 1).put(new ImmutablePair<>(elems, fracs), prototypeID);
        }
    }

    /**
     * Round a compound such that its composition exactly equals that of a common
     *   composition (from {@linkplain #CommonCompositions}. Store the name of its prototype in that composition's list (if
     *   not already there), return the index of the prototype in that list
     * @param frac Atomic fractions of elements in the compound (will be rounded
     *   to match that of the closest bin)
     * @param prototype Name of the prototype structure for this compound
     * @return Index of compound prototype in the list prototypes known at the nearest
     *  composition
     */
    private int roundCompositionAndMatchStructure(double[] frac, String prototype) {
        double[] key = createLookupKey(frac);
        
        // --> Find the composition bin closest to the key
        int compositionIndex = getClosestBin(key);
        key = CommonCompositions.get(compositionIndex).getLeft();
        
        // --> For that bin, see if it contains the name of this compound's prototype
        int prototypeIndex = CommonCompositions.get(compositionIndex).getValue().indexOf(prototype);
        if (prototypeIndex == -1) {
            prototypeIndex = CommonCompositions.get(compositionIndex).getValue().size();
            CommonCompositions.get(compositionIndex).getValue().add(prototype);
        }
        
        // --> Round "frac" so that it precisely matches the bin
        System.arraycopy(key, 1, frac, 0, frac.length);

        return prototypeIndex;
    }

    @Override
    protected int[] getCompoundVector(int[] sites) {
        // --> Initialize output vector (should be all zeros)
        int[] output = new int[CommonCompositions.size()];
        
        // --> For each number of constituents, get known compounds
        double[] key = new double[sites.length + 1];
        for (int nc=1; nc <= NComponents; nc++) {
            int[] elemList = new int[nc];
            int[] matchOrder = new int[nc];
            
            // Generate possible choices for this element
            if (ElemChoices.size() < nc) {
                // Only needs to be done once. Creates a list of lists with all constituent unary, binary, ... systems
                //  Ex: For a quaternary ABCD, ElemChoices contains [A, B, C, D], [AB, ...], ... , [ABCD]
                int[] elemChoice = new int[nc];
                for (int i=1; i<nc; i++) elemChoice[i] = i;
                List<int[]> temp = new LinkedList<>();
                boolean isDone = false;
                while (!isDone) {
                    temp.add(elemChoice.clone());
                    isDone = incrementCounter(elemChoice);
                }
                ElemChoices.add(temp);
            }
            
            // For each permutation of nc elements from sites
            for (int[] elemChoice : ElemChoices.get(nc-1)) {
                // Get compounds with these elements
                for (int i=0; i<elemChoice.length; i++) elemList[i] = sites[elemChoice[i]];
                Arrays.sort(elemList);
                SortedMap<Pair<int[], double[]>, Integer> compounds = 
                        getCompoundsInSystem(elemList);

                // Determine how the order of elements in these compounds matches
                //  up with the requested phase diagram (defined by the order of sites)
                for (int i=0; i<elemList.length; i++) {
                    matchOrder[i] = ArrayUtils.indexOf(sites, elemList[i]);
                }
                // For each of those compounds, find the corresponding composition
                //  in CommonComposition and set the value of the appropriate 
                //  index in the compound vector to the proper value depending on
                //  the type of prototype of the compound
                for (Map.Entry<Pair<int[], double[]>, Integer> compound : compounds.entrySet()) {
                    // Get the composition of this compound
                    double[] unorderedFrac = compound.getKey().getValue();

                    // Create the lookup key
                    Arrays.fill(key, 0.0);
                    key[0] = compound.getKey().getKey().length;
                    for (int i=0; i<nc; i++)
                        key[matchOrder[i]+1] = unorderedFrac[i];
                    int compositionID = getCompositionIndex(key);
                    if (compositionID < 0) {
                        throw new RuntimeException("Implementation Error: Somehow, the composition corresponding to a compound was not found!");
                    }

                    // Mark the appropriate point in the output as the prototype ID
                    //  of the corresponding compound
                    output[compositionID] = compound.getValue() + 1;
                }
            }
        }
        // --> We're done here
        return output;
    }

    /**
     * Look up a composition in {@linkplain #CommonCompositions}. 
     * <br>WARNING: Only use this if fracs is exactly (or real close) to an entry. Will
     * NOT find the composition that is closest to the supplied composition.
     * @param fracs Composition to look up
     * @return Index of composition in {@linkplain #CommonCompositions}
     */
    protected int getCompositionIndex(double[] fracs) {
        return Collections.binarySearch(CommonCompositionCopy, 
                fracs, CompositionComparatorCopy);
    }
    
    /**
     * Get all compounds within a certain system. Since we know the sorting order of
     *   {@linkplain #CompoundLookup}, this can be accomplished rather quickly.
     * @param elemList List of elements that define this system. Will be sorted
     *  so that elemList is in the same order as the key of all entries in the 
     *  output map. (Those are probably sorted by atomic number)
     * @return Map of composition to prototype index for all compounds in that system
     */
    private SortedMap<Pair<int[], double[]>, Integer> getCompoundsInSystem(int[] elemList) {
        // --> Get all compounds with elemList.length # of constituents
        SortedMap<Pair<int[], double[]>, Integer> allCompounds = 
                CompoundLookup.get(elemList.length - 1);
        // --> Get all entries in this system
        int[] elemListStart = elemList.clone();
        int[] elemListEnd = elemList.clone(); elemListEnd[elemListEnd.length - 1]++;
        return allCompounds.subMap(new ImmutablePair<>(elemListStart, new double[elemList.length]),
                new ImmutablePair<>(elemListEnd, new double[elemList.length]));
    }

    @Override
    protected void calculateStructureProbabilities() {
        // Initialize tally arrays. This stores in how many phase diagrams each prototype appears
        long[][] prototypeHitCount = new long[CommonCompositions.size()][];
        for (int i = 0; i < CommonCompositions.size(); i++) {
            prototypeHitCount[i] = new long[CommonCompositions.get(i).getRight().size()];
        }

        // --> For each known compound, increment the corresponding hit count
        //  for its prototype ID in *all* corresponding compositions
        // Example: A binary compound AlNu appears in (113-2) other ternary, such as Al-Ni-He
        //   And, for each ternary there are 6 permutations
        int[] permutation = new int[NComponents]; // Generate all possible arrangements of elements
        for (int i=1; i<NComponents; i++) permutation[i] = i;
        Set<int[]> permutations = DistinctPermutationGenerator.generatePermutations(permutation);
        double[] temp = new double[NComponents+1];
        for (SortedMap<Pair<int[],double[]>,Integer> compounds : CompoundLookup) {
            for (Map.Entry<Pair<int[], double[]>, Integer> compound : compounds.entrySet()) {
                Pair<int[], double[]> comp = compound.getKey();
                
                // For compounds with less than NComponents constituents,
                //  how many different values can the other constituents take on;
                long nDiagrams = 1;
                for (int i=comp.getLeft().length; i<NComponents; i++) {
                    nDiagrams *= (long) (LookupData.ElementNames.length - i);
                }
                nDiagrams /= CombinatoricsUtils.factorial(NComponents - comp.getLeft().length);
                
                // For each arrangement (add compounds)
                Integer prototypeID = compound.getValue();
                double[] key = createLookupKey(comp.getRight());
                for (int[] p : permutations) {
                    temp[0] = key[0];
                    for (int i=0; i<NComponents; i++) temp[i+1] = key[p[i]+1];
                    int compositionID = getClosestBin(temp);
                    prototypeHitCount[compositionID][prototypeID] += nDiagrams;
                }
            }
        }
        
        // --> Calculate corresponding probability of seeing each prototype (or nothing) at each composition
        long nPhaseDiagrams = 1;
        for (int i=0; i < NComponents; i++) {
            nPhaseDiagrams *= (long) (LookupData.ElementNames.length - i);
        }
        // Assemble arrays
        StructureProbability = new double[prototypeHitCount.length][];
        for (int i = 0; i < CommonCompositions.size(); i++) {
            StructureProbability[i] = new double[prototypeHitCount[i].length + 1];
            long timesNothingSeed = nPhaseDiagrams;
            for (int j = 0; j < prototypeHitCount[i].length; j++) {
                timesNothingSeed -= prototypeHitCount[i][j];
                StructureProbability[i][j+1] = ((double) prototypeHitCount[i][j] + 1.0 / StructureProbability[i].length) / (double) nPhaseDiagrams;
            }
            StructureProbability[i][0] = ((double) timesNothingSeed + 1.0 / StructureProbability[i].length) / (double) nPhaseDiagrams;
        }
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
