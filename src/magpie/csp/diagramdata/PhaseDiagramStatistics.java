
package magpie.csp.diagramdata;

import magpie.data.materials.CompositionEntry;
import magpie.data.materials.PrototypeEntry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import magpie.data.materials.*;
import magpie.data.materials.util.LookupData;
import magpie.utility.DistinctPermutationGenerator;
import org.apache.commons.lang3.tuple.*;

/**
 * Holds what compounds exist in phase diagrams. Can be used to calculate 
 *  the probability of certain compound having certain characteristics depending
 *  on what elements are at specific compositions in its corresponding phase diagram.
 *  Based on work by <a href="http://www.nature.com/nmat/journal/v5/n8/full/nmat1691.html">Fisher <i>et al</i></a>.
 * 
 * <p><b><u>Practical Guide:</u></b>
 * 
 * <p>Disregarding actually informing you what is being computed. Use this class by:
 * 
 * <ol>
 * <li>For a list of compounds, calculate whether your desired condition is true for each entry
 * <li>Train the cumulant functions by calling {@linkplain #getCumulants(magpie.data.oqmd.PrototypeDataset, boolean[]) }
 * <li>Use those functions predictively by calling {@linkplain #evaluateProbability(magpie.data.oqmd.PrototypeEntry, double[][], double) }
 * </ol>
 * 
 * <p><b><u>Theory Behind this Method:</u></b>
 * 
 * <p>The simple goal of this method is to calculate correlations between two events,
 * like a phase diagram having a B2 compound and one of its constituents being Fe.
 * This is accomplished by representing each phase diagram by the following variable vector:
 *          <center>C<sub>1</sub>, ..., C<sub>N</sub>, E<sub>1</sub>, ..., E<sub>N</sub></center>
 * where C<sub>i</sub> is the identity of a certain (numbered) prototype compound existing near composition bin i and
 * E<sub>j</sub> is the identity of element j in the phase diagram.
 * 
 * <p>With this description, one can calculate the probability of some condition being 
 * true for a compound in a certain phase diagram by calculating:
 * <center>P(condition | C<sub>1</sub> = a<sub>1</sub> &cap; ... &cap; E<sub>N</sub> = Z<sub>N</sub>)</center>
 * where a<sub>i</sub> is the identity of the structure of the compound that exists at composition
 * C<sub>i</sub> of the phase diagram. Similarly, Z<sub>i</sub> is the atomic number 
 * of the i-th constituent of the phase diagram. 
 * 
 * <p>In order make this computable, <a href="http://www.nature.com/nmat/journal/v5/n8/full/nmat1691.html">Fisher <i>et al</i></a>
 * used the following approximation (known as the Morita cumulant expansion):
 * <center>P(a &cap; b &cap; c &cap; ...) = 1 / Z * P(a) * P(b) * [...] * g(a,b) * g(a,c) * [...]</center>
 * where g(a,b) = P(a &cap; b) / P(a) / P(b). 
 * 
 * <p>Applied to the above condition probability formula, this allows the following simplification:
 * <center>P(condition | C<sub>1</sub> = a<sub>1</sub> &cap; ... &cap; E<sub>N</sub> = Z<sub>N</sub>) = P(condition &cap; C<sub>1</sub> = a<sub>1</sub> &cap; ...) / P(condition) / P(C<sub>1</sub> = a<sub>1</sub> &cap; ...)
 * <br>&asymp; P(condition) * g(condition, C<sub>1</sub> = a<sub>1</sub>) * [...]</center>
 * 
 * <p>All of the probabilities necessary to calculate are relatively straightforward to compute.
 * The only other important thing to note is a slight adjustment to the calculated probabilities that
 * ensures that P(x &cap; y) is always non-zero, but is always some small positive value. This 
 * (very clever) modification allows for P(condition | ...) to be non-zero when 
 * the condition and only one of the variables have not yet been observed to exist
 * together before. Otherwise, the corresponding g(x,y) will be zero and the calculated
 * probability would be zero - even if all other variables have favorable correlations.
 * 
 * <center>g(condition, x==y) = P(condition &cap; x) / P(stable) / P(x==y)
 * <br>P(stable) is: ([# condition == true] + 1 / [# training examples]) / [# examples]
 * <br>P(x==y) is: ([# times condition x==y in all phase diagrams] + 1 / [# of possible values for x]) / [# of phase diagrams]
 * <br>P(stable &cap; x) is: ([# times both were true] + 1 / [# possible values] / [# examples]) / [# of examples]</center>
 * 
 * <p><b><u>Implementation Guide:</u></b>
 * 
 * <p>Every operation necessary for this class except three are implemented in the base class.
 * In order to make a functioning version, one needs to implement:
 * 
 * <ol>
 * <li>{@linkplain #processCompounds(java.util.Map) } - Given a map of compound compositions
 * and a name of their prototype, save the compound's location in a phase diagram
 * and the prototype as a possible candidate structure at a certain composition.
 * <li>{@linkplain #getCompoundVector(int[]) } - Given the identity of an element
 * on each site of a crystal, return a list of the identity of each structure at
 * each of the {@linkplain #CommonCompositions} (0 for no compound).
 * <li>{@linkplain #calculateStructureProbabilities() } - Calculate the probability 
 * of each prototype existing at each composition in {@linkplain #CommonCompositions}. Note
 * the revised method for calculating probabilities (see "Theory Behind this Method").
 * </ol>
 *  
 * @author Logan Ward
 */
public abstract class PhaseDiagramStatistics implements Serializable {
    /** Minimum Manhattan distance between compositions to be considered equal */
    protected final double MinDistance = 1e-5;
    /** Order of phase diagram */
    protected int NComponents;
    /** Probability that each element appears in a phase diagram */
    protected double ElementProbability;
    /** Number of possible elements */
    protected int NElements;
    /** List of common compositions and the names of prototypes found at that composition.
     * Compositions with identical stoichiometry (i.e. AB and BA) share the same list.
     */
    protected List<Pair<double[], List<String>>> CommonCompositions;
    /** For each composition, probability of each structure existing in a phase diagram. [][0] is the no-structure * condition. */
    protected double[][] StructureProbability;
    /** Names of each element */
    protected String[] ElementNames;
    /**
     * Used to sort {@linkplain #CommonCompositions}. Only sorts by the composition, never
     *  the list of prototype names.
     */
    protected final Comparator<Pair<double[], List<String>>> CompositionComparator = new Comparator<Pair<double[], List<String>>>() {
        @Override
        public int compare(Pair<double[], List<String>> o1, Pair<double[], List<String>> o2) {
            double[] comp1 = o1.getKey();
            double[] comp2 = o2.getKey();
            int c = Integer.compare(comp1.length, comp2.length);
            if (c != 0) {
                return c;
            }
            for (int i = 0; i < comp1.length; i++) {
                c = Double.compare(comp1[i], comp2[i]);
                if (c != 0) {
                    return c;
                }
            }
            return 0;
        }
    };
    /** Number compounds stored by this object */
    protected int NCompounds;

    /**
     * Create a blank instance of this class.
     */
    protected PhaseDiagramStatistics() {};
    
    /**
     * Get number of compounds stored by this object.
     * @return Number of distinct compounds stored by this object.
     */
    public int NCompounds() {
        return NCompounds;
    }
    
    /**
     * Get the order of the phase diagram (number of elements).
     * @return Number of elements in each phase diagram
     */
    public int NComponents() {
        return NComponents;
    }

    /**
     * Determine whether two compositions are equal (within {@linkplain #MinDistance}). Uses
     * the Manhattan distance.
     * @param a Composition #1
     * @param b Composition #2
     * @return Whether they are closer than a minimum tolerance
     */
    protected boolean compositionsAreEqual(double[] a, double[] b) {
        if (a.length != b.length) {
            return false;
        }
        double totalDistance = 0;
        for (int i = 0; i < a.length; i++) {
            totalDistance += Math.abs(a[i] - b[i]);
            if (totalDistance > MinDistance) {
                return false;
            }
        }
        return true;
    }

    /**
     * For a certain prototype, get a vector that describes the characteristics of its phase diagram.
     * This variable vector has the following entries:
     * <center>S<sub>1</sub>, [...,] S<sub>N</sub>, E<sub>1</sub>, [...,] E<sub>C</sub></center>
     * where S<sub>i</sub> is the identity of the structure in composition bin i (0 for no compound) and E<sub>j</sub>
     * is the identity of element on site j.
     *
     * @param compound Compound of interest
     * @return Vector describing phase diagram
     */
    public int[] getVariableVector(PrototypeEntry compound) {
        // ---> Get the identity of elements on each site
        int[] elems = new int[compound.NSites()];
        for (int i = 0; i < elems.length; i++) {
            int[] siteElems = compound.getSiteComposition(i).getElements();
            if (siteElems.length != 1) {
                throw new Error("More than one element on each site is not supported.");
            }
            elems[i] = siteElems[0];
        }
        // ---> Get the identity of compounds from corresponding the phase diagram
        int[] compoundList = getCompoundVector(elems);
        // ---> Make it into the variable vector
        int[] output = new int[compoundList.length + elems.length];
        System.arraycopy(compoundList, 0, output, 0, compoundList.length);
        System.arraycopy(elems, 0, output, compoundList.length, elems.length);
        return output;
    }

    /**
     * Calculate the probability of each compound and element being in a phase
     *  diagram. It only needs to be done once.
     */
    protected void calculateProbabilities() {
        // Elements is real easy - same for every element
        ElementProbability = (1.0 + 1.0 / NElements) / (double) NElements;
        calculateStructureProbabilities();
    }

    /**
     * Import all known compounds into phase diagram object
     * @param Filename Path to a file containing all known compounds
     * @param DiagramOrder Number of constituents in phase diagram (2 for binary, etc)
     * @param DesiredNBins Number of composition bins for each number of compounds. Should
     * be an array where x[i] is the number of desired composition bins for compositions
     * with i+1 components.
     */
    public void importKnownCompounds(String Filename, int DiagramOrder, int[] DesiredNBins) {
        // ---> Import the file containing prototype names
        Map<CompositionEntry, String> compounds = importCompoundDataset(Filename);
        importKnownCompounds(compounds, DiagramOrder, DesiredNBins);
    }

    /**
     * Import list of known compounds into phase diagram object
     * @param compounds Map of composition to prototype name
     * @param DiagramOrder Number of constituents in phase diagram (2 for binary, etc)
     * @param DesiredNBins Number of composition bins for each number of compounds. Should
     * be an array where x[i] is the number of desired composition bins for compositions
     * with i+1 components.
     * @param DesiredNBins Desired number of composition bins
     */
    public void importKnownCompounds(Map<CompositionEntry, String> compounds, int DiagramOrder, int[] DesiredNBins) {
        // --> Get all compounds relevant for this diagram
        Map<CompositionEntry,String> compoundList = new TreeMap<>();
        NComponents = DiagramOrder;
        NElements = compounds.keySet().iterator().next().getElementNameList().length;
        Iterator<Map.Entry<CompositionEntry,String>> iter = compounds.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<CompositionEntry,String> entry = iter.next();
            if (entry.getKey().getElements().length <= NComponents) 
                compoundList.put(entry.getKey(), entry.getValue());
        }
        NCompounds = compoundList.size();
        
        // ---> Count the number of times each stoichiometry occurs at each composition
        getCommonCompositions(compoundList, DesiredNBins);
        Collections.sort(CommonCompositions, CompositionComparator);
        
        // --> For each compound, add it to all appropriate phase diagrams
        processCompounds(compoundList);
        // Calculate probabilities
        calculateProbabilities();
    }

    /**
     * Calculate the probability of each prototype appearing in a phase diagram.
     */
    protected abstract void calculateStructureProbabilities();

    /**
     * Load in file containing composition and prototypes of all known compounds.
     * @param filename Path to data file
     * @return Map of composition to structural prototype
     */
    protected Map<CompositionEntry, String> importCompoundDataset(String filename) {
        Map<CompositionEntry, String> compoundList = new TreeMap<>();
        BufferedReader is;
        try {
            is = Files.newBufferedReader(Paths.get(filename), Charset.forName("US-ASCII"));
        } catch (IOException e) {
            throw new Error(e);
        }
        // Read in each compound
        ElementNames = LookupData.ElementNames;
        NElements = LookupData.SortingOrder.length;
        do {
            String Line;
            try {
                Line = is.readLine();
            } catch (IOException e) {
                throw new Error(e);
            }
            if (Line == null) {
                break;
            }
            String[] words = Line.split("\t");
			try {
				CompositionEntry entry = new CompositionEntry(words[0]);
				compoundList.put(entry, words[1]);
			} catch (Exception e) {
				System.err.println("Problem parsing entry: " + words[0]);
			}
        } while (true);
        return compoundList;
    }

    /**
     * @return Number of different compositions bins
     */
    public int NCompositions() {
        return CommonCompositions.size();
    }

    /**
     * Determine most common compositions from all structures. Does so by first finding most common stoichiometry
     *  and then storing all permutations (AB, BA, ABC, BAC, ...).
     * @param Compounds Map of compound compositions and prototype names
     * @param DesiredNBins Number of composition bins to have for each number of constituents.
     * x[0] is for binaries, x[1] for ternaries, and so on...
     */
    protected void getCommonCompositions(Map<CompositionEntry, String> Compounds, int[] DesiredNBins) {
		if (DesiredNBins.length < NComponents - 1) {
			throw new Error("Too few compositions defined.");
		}
        // --> Find the number of times each composition appears
        List<List<Pair<double[], Integer>>> Histograms = new ArrayList<>(NComponents - 1);
        for (int i=0; i<NComponents-1; i++) Histograms.add(new LinkedList<Pair<double[], Integer>>());
        for (Map.Entry<CompositionEntry, String> entry : Compounds.entrySet()) {
            // Get the stoichiometry of this entry
            CompositionEntry newCompound = entry.getKey();
            double[] newComposition = newCompound.getFractions();
            // Skip elemental compounds, or those with more elements than we are interested in
            if (newComposition.length == 1 || newComposition.length > NComponents) continue;
            Arrays.sort(newComposition);
            // If not already known, add to list
            List<Pair<double[], Integer>> relevantList = Histograms.get(newComposition.length - 2);
            boolean wasFound = false;
            for (Pair<double[], Integer> known : relevantList) {
                // Check whether this stoichiometry has been found
                if (compositionsAreEqual(newComposition, known.getKey())) {
                    wasFound = true;
                    known.setValue(known.getValue() + 1);
                    break;
                }
            }
            if (!wasFound) {
                relevantList.add(new MutablePair<>(newComposition, 1));
            }
        }
        // --> Find the most common entries, add an appropriate number to the composition list
        CommonCompositions = new LinkedList<>();
        Comparator<Pair<double[], Integer>> ranker = new Comparator<Pair<double[], Integer>>() {
            @Override
            public int compare(Pair<double[], Integer> o1, Pair<double[], Integer> o2) {
                return -1 * o1.getValue().compareTo(o2.getValue());
            }
        }; // Sorts such that the most frequent are at the top
        // Unary
        addCompositionBin(new double[]{1}, new LinkedList<String>());
        // Binary +
        int elemCount = 1;
        for (List<Pair<double[],Integer>> histogram : Histograms) {
            Collections.sort(histogram, ranker);
            elemCount++;
            int startCount = CommonCompositions.size();
            Iterator<Pair<double[],Integer>> iter = histogram.iterator();
            // Add until we have either exceed the target number of bins, or run out of common compositions
            while (CommonCompositions.size() - startCount < DesiredNBins[elemCount - 2]
                    && iter.hasNext()) {
                addCompositionBin(iter.next().getKey(), new LinkedList<String>());
            }
        }
    }

    /**
     * Adds all permutations (i.e. ABC, BAC, ...) to
     * @param x Stoichiometry to be added (must be sorted)
     * @param Prototypes List that holds names of all prototypes with this stoichiometry (does not have to contain anything yet)
     */
    protected void addCompositionBin(double[] x, List<String> Prototypes) {
        // Ensure that if any entries are closer than a certain tolerance, they are made
        //  precisely equal
        for (int i=1; i<x.length - 1; i++) {
            if (x[i] - x[i-1] < MinDistance) x[i] = x[i-1];
        }
        
        // Extend the length of the list to NComp, if necessary
        double[] xCopy;
        if (x.length < NComponents) {
            xCopy = new double[NComponents];
            System.arraycopy(x, 0, xCopy, 0, x.length);
        } else {
            xCopy = x;
        }
        // Generate all distinct duplicates
        Set<double[]> images = DistinctPermutationGenerator.generatePermutations(xCopy);
        for (double[] image : images) {
            CommonCompositions.add(new ImmutablePair<>(image, Prototypes));
        }
    }

    /**
     * For a certain training set, calculate the cumulant function between a condition
     *  being true and each variable being any allowed condition. Each variable,
     *  and their possible values are described in {@linkplain #getVariableVector(magpie.data.oqmd.PrototypeEntry) }
     * @param trainingData Data containing examples on which to train cumulant functions
     * @param hasCondition For each entry in the training set, whether whatever condition is true
     * @return x[i][j] is the cumulant function: g(condition == True, variable_i == j)
     */
    public double[][] getCumulants(PrototypeDataset trainingData, boolean[] hasCondition) {
		// --> Generate list of all prototypes and their equivalent structures
		List<Pair<PrototypeEntry,Boolean>> trainingExamples = new LinkedList<>();
		for (int i=0; i<trainingData.NEntries(); i++) {
			PrototypeEntry E = trainingData.getEntry(i);
			for (PrototypeEntry equiv : E.getEquivalentPrototypes()) {
				trainingExamples.add(new ImmutablePair<>(equiv, hasCondition[i]));
			}
		}
        // --> Determine probably of condition being true
        int hitCount = 0;
        for (boolean ex : hasCondition) if (ex) hitCount++;
        double conditionProbability = ((double) hitCount + 1.0 / trainingData.NEntries()) / (double) hasCondition.length;
        // --> Prepare arrays to calculate how many times a condition is a certain value
        // is true and the entry has class==0
        int[][] bothTrue = new int[CommonCompositions.size() + NComponents][];
        for (int i = 0; i < CommonCompositions.size(); i++) {
            bothTrue[i] = new int[CommonCompositions.get(i).getRight().size() + 1];
        }
        for (int i = 0; i < NComponents; i++) {
            bothTrue[CommonCompositions.size() + i] = new int[NElements];
        }
        // ---> For each compound where class == 0, mark values of each variable
		for (Pair<PrototypeEntry,Boolean> ex : trainingExamples) {
            if (ex.getValue()) {
                int[] variableVector = getVariableVector(ex.getKey());
                for (int j = 0; j < variableVector.length; j++) {
                    bothTrue[j][variableVector[j]]++;
                }
            }
        }
        // --> Use results to calculate cumulants
        double[][] cumulant = new double[bothTrue.length][];
        for (int v = 0; v < cumulant.length; v++) {
            cumulant[v] = new double[bothTrue[v].length];
            // For all variables corresponding to the existance of a structure at a certain composition
            for (int c = 0; c < cumulant[v].length; c++) {
                // First, calculate probability of both being true (with additional factor to ensure it is never 0 - Fischer said so)
                cumulant[v][c] = ((double) bothTrue[v][c] + 1.0 / bothTrue[v].length / trainingExamples.size()) / trainingExamples.size();
                // Second, calcualte cumulant (ratio of joint / product of individual probabilities)
                if (v < CommonCompositions.size()) {
                    cumulant[v][c] /= StructureProbability[v][c] * conditionProbability;
                } else {
                    cumulant[v][c] /= ElementProbability * conditionProbability;
                }
            }
        }
        return cumulant;
    }

    /**
     * Given a prototype entry, generate its probability of having a certain condition. For 
	 *  prototypes that have symmetrically-equivalent sites, this operation will calcualate 
	 *  the maximum probability for all equivalent arrangements.
     * @param entry Entry to evaluate
     * @param cumulant Cumulant function values generated by {@linkplain #getCumulants(magpie.data.oqmd.PrototypeDataset, boolean[]) }
     * @param conditionProb Probability that a random entry has the condition
     * @return Probability of this particular entry having a condition
     */
    public double evaluateProbability(PrototypeEntry entry, double[][] cumulant, double conditionProb) {
		double bestProb = Double.NEGATIVE_INFINITY;
		for (PrototypeEntry equiv : entry.getEquivalentPrototypes()) {
			double prob = conditionProb;
			int[] variableVector = getVariableVector(equiv);
			for (int i = 0; i < variableVector.length; i++) {
				prob *= cumulant[i][variableVector[i]];
			}
			if (prob > bestProb) bestProb = prob;
		}
		return bestProb;
    }

    /**
     * Given a list of elements on each site of a structure, determine which prototypes
     *  exist on each site of the corresponding phase diagram.
     * @param sites Which elements are on each site
     * @return x[i] is the index of the prototype structure of the compound existing
     * at composition i in {@linkplain #CommonCompositions} (0 for no compound)
     */
    protected abstract int[] getCompoundVector(int[] sites);

    /**
     * Given a list of compounds, mark which phase diagrams they appear in. Also, 
     *  keep a list of the names of prototype compounds that appear
     * @param compounds Map of phase diagrams to be added
     */
    protected abstract void processCompounds(Map<CompositionEntry, String> compounds);

    /**
     * Find the composition bin in {@linkplain #CommonCompositions}) closest to
     *  a certain composition
     * @param fracs Composition of a compound. Defined by the fraction of each element present
     * @return Index of bin closest to this composition
     */
    public int getClosestBin(double[] fracs) {
        double closestDist = 1.0E100;
        int compositionIndex = -1;
        for (int bin = 0; bin < CommonCompositions.size(); bin++) {
            double[] binComposition = CommonCompositions.get(bin).getKey();
            double totalDist = 0.0;
            for (int e = 0; e < fracs.length; e++) {
                totalDist += Math.abs(binComposition[e] - fracs[e]);
            }
            if (totalDist < closestDist) {
                closestDist = totalDist;
                compositionIndex = bin;
                if (totalDist < MinDistance) {
                    break;
                }
            }
        }
        return compositionIndex;
    }

    /**
     * Extend the length of a composition by padding the end with zeros.
     * @param frac Fraction to be extend
     * @return frac extended with 0s
     */
    protected double[] extendComposition(double[] frac) {
        // If length is smaller than NComponents, pad it with zeros
        double[] key;
        if (frac.length == NComponents) {
            // Should never be >
            key = frac.clone();
        } else if (frac.length < NComponents) {
            key = new double[NComponents];
            System.arraycopy(frac, 0, key, 0, frac.length);
        } else {
            throw new Error("Implementation Error: Compound has more elements than this phase diagram keeps track of");
        }
        return key;
    }
    
    /**
     * Print out values of each cumulant and their names. Prints only the ones with
     *  the strongest effect on the calculated probability. This is measured by |log(cumulant)|
     * 
     * @param cumulants Cumulants to be printed
     * @param toPrint Number of the strongest cumulants to print. -1 for all of them 
     * @return List containing the top toPrint cumulants
     */
    public String printCumulants(double[][] cumulants, int toPrint) {
        // --> Rank cumulants by power
        int nCumulants = 0;
        for (double[] x : cumulants) nCumulants += x.length;
        List<Pair<int[],Double>> cumulantList = new ArrayList<>(nCumulants);
        for (int c=0; c<cumulants.length; c++) {
            for (int v=0; v<cumulants[c].length; v++) {
                cumulantList.add(new ImmutablePair<>(new int[]{c,v},
                        Math.abs(Math.log(cumulants[c][v]))));
            }
        }
        Comparator<Pair<int[],Double>> comparer = new Comparator<Pair<int[], Double>>() {
            @Override
            public int compare(Pair<int[], Double> o1, Pair<int[], Double> o2) {
                return Double.compare(o2.getValue(), o1.getValue());
            }
        };
        Collections.sort(cumulantList, comparer);
        
        // --> Print out list of the most powerful
        String output = "";
        for (int i=0; i!=toPrint && i<nCumulants; i++) {
            int[] id = cumulantList.get(i).getKey();
            // Get name of this condition
            String name;
            if (id[0] < CommonCompositions.size()) {
                // The cumulant corresponds to a particular compound (at a certain compositions)
                double[] comp = CommonCompositions.get(id[0]).getKey();
                name = "";
                for (int c=0; c<NComponents; c++) {
                    if (comp[c] > 0) {
                        name += String.format("%c%.2f", (char) (65 + c), comp[c] * 100);
                    }
                }
                name += id[1] == 0 ? "-" + "NoCompound" : 
                        "-" + CommonCompositions.get(id[0]).getValue().get(id[1]-1);
            } else {
                // The cumulant corresponds to an element on a particular site
                int site = id[0] - CommonCompositions.size();
                String Element = ElementNames[id[1]];
                name = Character.toString((char) (65 + site)) + "=" + Element;
            }
            // Print out name and cumulant
            output += String.format("%s\t%.4e\n", name, cumulants[id[0]][id[1]]);
        }
        return output;
    }
    
    /**
     * Round a compound such that it's composition exactly equals that of a common
     *   composition, and return the index of that composition
     * @param frac Atomic fractions of elements in the compound (will be rounded
     *   to match that of the closest bin)
     * @return Index of compound prototype in the list prototypes known at the nearest
     *  composition
     */
    public int roundComposition(double[] frac) {
        double[] key = extendComposition(frac);
        
        // --> Find the composition bin closest to the key
        int compositionIndex = getClosestBin(key);
        
        // --> Round "frac" so that it precisely matches the bin
        key = CommonCompositions.get(compositionIndex).getLeft();
        System.arraycopy(key, 0, frac, 0, frac.length);
        for (int i=frac.length + 1; i<key.length; i++) {
            if (key[i] > MinDistance) {
                throw new Error("Implemenation Error: " + frac.length + "-component compound " +
                        " matched to a " + key.length + "-component composition");
            }
        }
        
        return compositionIndex;
    }
    
    /**
     * Get list of all known prototype structures at a composition.
     * @param fracs Fraction of elements in 
     * @return List of prototype names at the closest bin to composition
     */
    public List<String> getPrototypeNames(double[] fracs) {
        int id = getClosestBin(fracs);
        return new LinkedList<>(CommonCompositions.get(id).getValue());
    }
}
