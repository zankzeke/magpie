package magpie.csp;

import magpie.data.materials.CompositionEntry;
import magpie.data.materials.PrototypeEntry;
import magpie.csp.diagramdata.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.*;
import magpie.data.materials.util.PrototypeSiteInformation;
import magpie.data.utilities.filters.ContainsElementFilter;
import magpie.models.classification.BaseClassifier;
import magpie.utility.interfaces.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Predict what structure is likely to form at a specific composition from 
 *  a list of possible candidates.
 * 
 * <p>
 * <b><u>Usage Guide</u></b>
 * <p>To predict the probability that a crystal structure will form:
 * <ol>
 * <li>Create an instance of one of the implementations of this class,
 * and set any appropriate options.
 * <li>Import a list of all known compounds, and their prototypes using either the
 * "prototypes" command in the text interface, or {@linkplain #importKnownCompounds(java.lang.String)}
 * <li>Supply the composition of the compound of interest. Use the "predict" command
 * in the text interface, or {@linkplain #predictStructure(java.lang.String) } in
 * the Java interface.
 * </ol>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>exclude &lt;elements...&gt;</b> - Remove entries containing certain elements 
 * from the list of known compounds
 * <br><pr><i>elements</i>: List of elements abbreviations</command>
 * 
 * <command><p><b>get model = $&ltmodel&gt;</b> - Get the classifier used to predict last crystal structure</command>
 * 
 * <command><p><b>predict &lt;composition> [&lt;# to show>]</b> - Predict what 
 *  structural prototypes are most likely for a certain compound
 * <br><pr><i>composition</i>: Composition of compound of interest
 * <br><pr><i># to show</i>: Number of the top candidates to print (default = 10)</command>
 * 
 * <command><p><b>prototypes &lt;filename></b> - Import list of known prototypes
 * <br><pr><i>filename</i>: Path to file containing composition of known compounds, and name of their prototype structure</command>
 * 
 * <command><p><b>examples &lt;composition&gt;</b> -  Get a list of other
 * possible prototypes for a certain composition and all known examples for
 * each prototype
 * <br><pr><i>composition</i>: Composition in question</command>
 * 
 * <command><p><b>validate &lt;ncomp> [&lt;folds>]</b> - Evaluate performance of CSP algorithm 
 * using cross-validation.
 * <br><pr><i>ncomp</i>: Only attempt to predict structures of compounds with this number of constituents
 * <br><pr><i>folds</i>: Number of folds in which to split known compounds. Can use "loocv" to perform
 * leave-one-out cross-validation (default = 20)</command>
 * 
 * <p><b><u>Implemented Print Commands:</u></b>
 * 
 * <print><p><b>stats</b> - Print out the number of predictions used to generate
 * performance statistics.</print>
 * 
 * <print><p><b>stats list-length [&lt;min prob&gt;] [&lt;max length&gt;]</b> - 
 *  Print out the minimum number of prototypes that need to be calculated for 
 *  a certain prediction success probability
 * <br><pr><i>min prob</i>: Minimum success probability to print (default = 0.7)
 * <br><pr><i>max length</i>: Maximum length list to print (default = 20)</print>
 * 
 * <p><b><u>Implementation Guide:</u></b>
 * 
 * <p>In order to create your own crystal structure predictor, you just need to implement 
 *  a single operation: {@linkplain #makeClassifier(magpie.csp.PhaseDiagramStatistics, magpie.data.oqmd.PrototypeDataset) }.
 *  This operation generates a classifier that will predict the probability that a certain 
 *  crystal structure will form given composition. Composition is supplied as a {@linkplain PrototypeEntry}
 *  where the least-prevalent element is on "A" site, the second least is on the "B", and so on. 
 *  Any sites with equal fractions are treated as equal (which is a key feature of {@linkplain PrototypeEntry}).
 * 
 * @author Logan Ward
 */
public abstract class CSPEngine implements Commandable, Printable, Options {
    /**
     * List of known compounds (used when making predictions)
     */
    protected Map<CompositionEntry, String> KnownCompounds;
    /**
     * Statistics about phase diagrams with certain number of constituents. Holds
     *  information about the structure of compounds that forms at each composition
     */
    protected Map<Integer, PhaseDiagramStatistics> DiagramStatistics = new TreeMap<>();
    /**
     * Number of elements in the last entry evaluated in this class. Used to determine 
     * whether we classifier needs to be reset.
     */
    protected int LastNComponents = -1;
    /**
     * Matched composition bin of last entry evaluated.
     */
    protected int LastCompositionBin = -1;
    /**
     * Classifier used to predict prototype given composition.
     */
    private BaseClassifier Classifier = null;
    /**
     * Holds statistics about CSP algorithm performance.
     */
    protected CSPPerformanceStats PerformanceStats = new CSPPerformanceStats();
    

    /**
     * Gather a list of known compounds an their prototypes. Format:
     * <p>
     * &lt;compound #1 composition>[tab]&lt;compound #1 prototype>
     * <br>&lt;compound #2 composition>[tab]&lt;compound #2 prototype>
     * <br>[...]
     *
     * @param filename Path to compound data file
     */
    public void importKnownCompounds(String filename) {
        KnownCompounds = new TreeMap<>();
        BufferedReader is;
        try {
            is = Files.newBufferedReader(Paths.get(filename), Charset.forName("US-ASCII"));
        } catch (IOException e) {
            throw new Error(e);
        }
        // Read in each compound
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
            String[] words = Line.split("\\s+");
			try {
				CompositionEntry entry = new CompositionEntry(words[0]);
				KnownCompounds.put(entry, words[1]);
			} catch (Exception e) {
				System.err.println("Problem parsing entry: " + words[0]);
			}
        } while (true);
    }

    /**
     * Define the composition and prototype of already-known compounds.
     * 
     * @param KnownCompounds Map of a compound's composition to the name of its prototype
     */
    public void setKnownCompounds(Map<CompositionEntry, String> KnownCompounds) {
        this.KnownCompounds = KnownCompounds;
    }
    
    /**
     * Given a list of elements, remove all entries that contain those elements
     * from the list of known compounds. 
     * @param ElementList List of elements to be removed
     */
    public void removeKnownCompoundsContainingElements(List<String> ElementList) {
        // Generate filter
        ContainsElementFilter filter = new ContainsElementFilter();
        filter.setElementList(ElementList.toArray(new String[0]));
        
        // Remove entries that fail
        Iterator<CompositionEntry> iter = KnownCompounds.keySet().iterator();
        while (iter.hasNext()) {
            CompositionEntry e = iter.next();
            if (filter.entryContainsElement(e)) {
                iter.remove();
            }
        }
    }
    
    /**
     * Get a list of possible structure types for a structure, given composition.
     * @param composition Composition of structure in question
     * @return List of names of possible prototypes
     * @throws java.lang.Exception
     */
    public List<String> getPossibleStructures(String composition) 
            throws Exception {
        return getPossibleStructures(new CompositionEntry(composition));
    }
    
    /**
     * Get a list of possible structure types for a structure, given composition.
     * @param composition Composition of structure in question
     * @return List of names of possible prototypes
     */
    public List<String> getPossibleStructures(CompositionEntry composition) {
        // Check if phase-diagram stats need to be computed
        int nComp = composition.getElements().length;
        PhaseDiagramStatistics statistics = DiagramStatistics.get(nComp);
        if (statistics == null) {
            // It has not been calculated yet
            statistics = new OnTheFlyPhaseDiagramStatistics();
            int[] NBins = new int[]{30, 120, 180, 240};
            statistics.importKnownCompounds(KnownCompounds, nComp, NBins);
            DiagramStatistics.put(nComp, statistics);
        }
        
        // Get the nearset composition bin
        int[] elems = composition.getElements();
        double[] fracs = composition.getFractions();
        orderComposition(elems, fracs);
        int compositionBin = statistics.getClosestBin(fracs);
        List<String> knownPrototypes = statistics.getPrototypeNames(fracs);
        return knownPrototypes;
    }
    
    /**
     * Get a training set of all examples of all possible crystal structures
     * at a certain composition
     * @param composition Composition of interest
     * @return Training set
     */
    public PrototypeDataset getTrainingSet(CompositionEntry composition) {
        // Get the possible prototypes
        List<String> knownPrototypes = getPossibleStructures(composition);
        
        // Get the composition bin for this entry
        int[] elems = composition.getElements();
        double[] fracs = composition.getFractions();
        orderComposition(elems, fracs);
        PhaseDiagramStatistics statistics = DiagramStatistics.get(elems.length);
        int compositionBin = statistics.getClosestBin(fracs);
        
        // Get siteInfo for this entry
        PrototypeSiteInformation siteInfo = makeSiteInfo(fracs);
        
        // Now, create a PrototypeDataset out of all known examples with same stoichiometry
        PrototypeDataset trainData = new PrototypeDataset();
        trainData.setClassNames(knownPrototypes.toArray(new String[0]));
        trainData.setSiteInfo(siteInfo);
        for (Map.Entry<CompositionEntry, String> entry : KnownCompounds.entrySet()) {
            // Skip entries where number of elem != nComp
            if (entry.getKey().getElements().length != elems.length) {
                continue;
            }
            // See if it maps to the same composition bin
            elems = entry.getKey().getElements();
            fracs = entry.getKey().getFractions();
            orderComposition(elems, fracs);
            if (compositionBin != statistics.getClosestBin(fracs)) {
                continue;
            }
            // If it fits both criteria, add it to the dataset
            PrototypeEntry newEntry = new PrototypeEntry(siteInfo);
            fillPrototypeEntry(newEntry, elems);
            int prototypeID = knownPrototypes.indexOf(entry.getValue());
            if (prototypeID == -1) {
                throw new Error("Unknown prototype structure: " + entry.getKey() + " = " + entry.getValue());
            }
            newEntry.setMeasuredClass((double) prototypeID);
            trainData.addEntry(newEntry);
        }
        return trainData;
    }

    /**
     * Predict what structure is most likely at a certain composition
     *
     * @param Composition Composition to be evaluated
     * @return List of names of prototypes and their probabilities
	 * @throws Exception If Composition does not parse correctly
     */
    public List<Pair<String, Double>> predictStructure(String Composition) throws Exception {
		CompositionEntry comp = new CompositionEntry(Composition);
        return predictStructure(comp);
    }

    /**
     * Predict what structure is most likely at a certain composition
     *
     * @param composition Composition to be evaluated
     * @return List of names of prototypes and their probabilities
     */
    public List<Pair<String, Double>> predictStructure(CompositionEntry composition) {
        // --> Get lookup data
        int nComp = composition.getElements().length;
        int[] elems = composition.getElements();
        double[] fracs = composition.getFractions();
        
        // --> Get list of possible structures
        List<String> knownPrototypes = getPossibleStructures(composition);
        
        // --> Get phase diagram statistics
        PhaseDiagramStatistics statistics = DiagramStatistics.get(nComp);
        
        // --> Whether to rebuild classifier
        boolean toRebuid = false;
        orderComposition(elems, fracs);
        int compositionBin = statistics.getClosestBin(fracs);
        if (nComp != LastNComponents || compositionBin != LastCompositionBin) {
            toRebuid = true;
            LastNComponents = nComp;
            LastCompositionBin = compositionBin;
        }
        
        // --> Generate entry to be predicted
        PrototypeSiteInformation siteInfo = makeSiteInfo(fracs);
        PrototypeEntry entryToPredict = new PrototypeEntry(siteInfo);
        fillPrototypeEntry(entryToPredict, elems);
        
        // --> If this new entry does not have the same stoichiometry as the last entry evaluated, 
        //     train a new classifier
        if (toRebuid) {
            // Get training set
            PrototypeDataset trainData = getTrainingSet(composition);
            
            // Train a CumulantExpansion model on this dataset
            Classifier = makeClassifier(statistics, trainData);
        }
        
        // --> Use it to predict probability that a certain structure will appear
        double[] probs = getProbabilities(Classifier, knownPrototypes, siteInfo, entryToPredict);
        
        // --> Store results, and sort such that the most likely class is first
        List<Pair<String, Double>> output = new LinkedList<>();
        for (int i = 0; i < probs.length; i++) {
            output.add(new ImmutablePair<>(knownPrototypes.get(i), probs[i]));
        }
        Collections.sort(output, new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return Double.compare(o2.getRight(), o1.getRight());
            }
        });
        return output;
    }

    /**
     * Generate {@linkplain PrototypeSiteInformation} appropriate for a certain 
     * crystal prototype. Assumes any sites with the same fraction are equivalent
     * @param fracs Fractions of elements on each site. Sorted in ascending order
     * @return Appropriate {@linkplain PrototypeSiteInformation}
     */
    protected PrototypeSiteInformation makeSiteInfo(double[] fracs) {
        PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
        siteInfo.addSite(fracs[0], false, new LinkedList<Integer>());
        for (int i = 1; i < fracs.length; i++) {
            List<Integer> equivSites = new LinkedList<>();
            if (fracs[i] == fracs[i - 1]) {
                equivSites.add(i - 1);
            }
            siteInfo.addSite(fracs[i], false, equivSites);
        }
        return siteInfo;
    }

    /**
     * Calculate probability that a compound will form as each of the known prototypes.
     * @param classifier Model trained to predict which prototype will form, given composition.
     * @param knownPrototypes List of known prototypes
     * @param siteInfo Information about the prototypes
     * @param entryToPredict Entry to predict
     * @return List of probabilities (that add to 1) in same order as knownPrototypes
     */
    protected double[] getProbabilities(BaseClassifier classifier, List<String> knownPrototypes,
            PrototypeSiteInformation siteInfo, PrototypeEntry entryToPredict) {
        PrototypeDataset runData = new PrototypeDataset();
        runData.setClassNames(knownPrototypes.toArray(new String[0]));
        runData.setSiteInfo(siteInfo);
        runData.addEntry(entryToPredict);
        classifier.run(runData);
        double[] probs = runData.getEntry(0).getClassProbilities();
        return probs;
    }

    /**
     * Given the dataset of training examples, make a classifier to predict the probability that a prototype
     *  will form at a certain composition.
     * @param statistics Statistics about all known phase diagrams (could be used predictively)
     * @param trainData Training data, where each entry maps
     * @return Classifier fitting the specifications above
     */
    protected abstract BaseClassifier makeClassifier(PhaseDiagramStatistics statistics, PrototypeDataset trainData);

    /**
     * Adjust a PrototypeEntry so that each site is occupied by a specific
     * element
     *
     * @param entry Entry to be modified
     * @param siteIdentity Identity of element present on each site.
     */
    protected void fillPrototypeEntry(PrototypeEntry entry, int[] siteIdentity) {
        if (entry.NSites() != siteIdentity.length) {
            throw new Error("Entry must have the same number of sites as the number of elements in siteIdentity");
        }
        for (int i = 0; i < siteIdentity.length; i++) {
            entry.setSiteComposition(i, new CompositionEntry(new int[]{siteIdentity[i]}, new double[]{1.0}));
        }
    }

    /**
     * Ensure that a composition is sorted such that elements are listed in
     * ascending order by atomic fraction
     *
     * @param elems Elements present in sample
     * @param frac Fractions of each element present
     */
    protected void orderComposition(int[] elems, double[] frac) {
        if (elems.length < 2) {
            return;
        }
        boolean notDone = true;
        double tempF;
        int tempE;
        while (notDone) {
            notDone = false;
            for (int i = 1; i < elems.length; i++) {
                if (frac[i] < frac[i - 1]) {
                    notDone = true;
                    tempE = elems[i];
                    elems[i] = elems[i - 1];
                    elems[i - 1] = tempE;
                    tempF = frac[i];
                    frac[i] = frac[i - 1];
                    frac[i - 1] = tempF;
                }
            }
        }
    }
    
    /**
     * Validates predictive ability of this model. Works by segmenting list of known compounds with
     *  a certain number of constituents into <code>folds</code> different subsets. Then, the CSP 
     *  algorithm is used to predict which compound will form for each compound in one of subsets given 
     *  all of the known compounds in the remaining <code>folds - 1</code>. This process is then repeated 
     *  for each other subset. Any compound that has a different number of compounds is always kept
     * 
     * <p>Note: If a compound in the testing set has a prototype that is not present in the remaining
     * subsets, no attempt is made to predict its structure. The number of times this occurs will be 
     * recorded.
     * 
     * @param nComp Only attempt to predict structures of compounds with these many elements
     * @param folds Number of folds to use (if &le;0, preform leave-one-out cross-validation)
     */
    public void crossvalidate(int nComp, int folds) {
        PerformanceStats.clear();
        Comparator<Map.Entry<CompositionEntry,String>> compComparer =
                new Comparator<Map.Entry<CompositionEntry,String>>() {
            @Override
            public int compare(Map.Entry<CompositionEntry,String> eo1, 
                    Map.Entry<CompositionEntry,String> eo2) {
                CompositionEntry o1 = eo1.getKey(), o2 = eo2.getKey();
                double[] f1 = o1.getFractions(), f2 = o2.getFractions();
                Arrays.sort(f1); Arrays.sort(f2);
                int c;
                for (int i=0; i<f1.length; i++) {
                    c = Double.compare(f1[i], f2[i]);
                    if (c != 0) return c;
                }
                int[] e1 = o1.getElements(), e2 = o2.getElements();
                f1 = o2.getFractions(); f2 = o2.getFractions();
                for (int i=0; i<f1.length; i++) {
                    c = Integer.compare(e1[i], e2[i]);
                    if (c == 0) {
                        c = Double.compare(f1[i], f2[i]);
                        if (c != 0) {
                            return c;
                        }
                    } else {
                        return c;
                    }
                }
                return eo2.getValue().compareTo(eo1.getValue());
            }
        };
        
        // --> Split data into subsets
        List<Map.Entry<CompositionEntry,String>> fullSet = new LinkedList<>(),
                alwaysKept = new LinkedList<>();
        for (Map.Entry<CompositionEntry,String> entry : KnownCompounds.entrySet()) {
            if (entry.getKey().getElements().length == nComp) {
                fullSet.add(entry);
            } else {
                alwaysKept.add(entry);
            }
        }
        // Check if user requests LOOCV
        if (folds <= 0) {
            folds = fullSet.size();
        }
        Collections.shuffle(fullSet);
        List<List<Map.Entry<CompositionEntry,String>>> subSets = new ArrayList<>(folds);
        for (int i=0; i<folds; i++) 
            subSets.add(new LinkedList<Map.Entry<CompositionEntry, String>>());
        for (int i=0; i<fullSet.size(); i++) {
            subSets.get(i % folds).add(fullSet.get(i));
        }
        
        // --> Run prediction for each subset
        for (int s=0; s<folds; s++) {
			DiagramStatistics.clear();
            // Assemble training and test sets
            List<Map.Entry<CompositionEntry,String>> testSet = subSets.get(s);
            KnownCompounds.clear();
            for (Map.Entry<CompositionEntry,String> entry : alwaysKept) {
                KnownCompounds.put(entry.getKey(), entry.getValue());
            }
            for (int i=0; i<folds; i++) {
                if (s != i) {
                    for (Map.Entry<CompositionEntry,String> entry : subSets.get(i)) {
                        KnownCompounds.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            
            Collections.sort(testSet, compComparer);
            // Attempt to predict structure for everything in test set
            for (Map.Entry<CompositionEntry,String> entry : testSet) {
                // Ensure that its prototype is in the training set
                if (! KnownCompounds.values().contains(entry.getValue())) {
					continue;
				}
                // Predict the prototype
                List<Pair<String,Double>> predictions = predictStructure(entry.getKey());
                PerformanceStats.addResult(entry.getValue(), predictions);
            }
        }
    }

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("For now, you need to supply a command name");
        }
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "examples" : {
                if (Command.size() != 2) {
                    throw new Exception("Usage: examples <composition>");
                }
                String comp = Command.get(1).toString();
                PrototypeDataset prot = getTrainingSet(new CompositionEntry(comp));
                CompositionDataset data = new CompositionDataset();
                data.setClassNames(prot.getClassNames());
                for (BaseEntry ptr : prot.getEntries()) {
                    PrototypeEntry entry = (PrototypeEntry) ptr;
                    data.addEntry(entry);
                }
                System.out.format("\tGarthered %d training examples.\n", data.NEntries());
                return data;
            } 
            case "exclude": {
                List<String> elems = new ArrayList<>(Command.size() - 1);
                for (Object obj : Command.subList(1, Command.size())) {
                    elems.add(obj.toString());
                }
                removeKnownCompoundsContainingElements(elems);
                return null;
            }
            case "get":
                if (Command.size() != 2) {
                    throw new Exception("Usage: get <component>");
                }
                return getComponent(Command.get(1).toString());
            case "predict":
                {
                    String compostion;
                    int toPrint;
                    try {
                        compostion = Command.get(1).toString();
                        toPrint = 10;
                        if (Command.size() == 3) {
                            toPrint = Integer.parseInt(Command.get(2).toString());
                        }
                    } catch (Exception e) {
                        throw new Exception("Usage: predict <composition>");
                    }
                    List<Pair<String, Double>> results = predictStructure(compostion);
                    System.out.println("\nProbable structures for " + compostion);
                    for (int i = 0; i < Math.min(toPrint, results.size()); i++) {
                        System.out.format("\t%25s\t%.2f%%\n", results.get(i).getKey(), results.get(i).getValue() * 100.0);
                    }
                }
                break;
            case "prototypes":
                {
                    if (Command.size() != 2) {
                        throw new Exception("Usage: prototypes <filename>");
                    }
                    String filename = Command.get(1).toString();
                    importKnownCompounds(filename);
                    System.out.println("\tImported " + KnownCompounds.size() + " compounds from " + filename);
                }
                break;
            case "validate": {
                int NFolds = 20, NComp;
                try {
                    NComp = Integer.parseInt(Command.get(1).toString());
                    if (Command.size() > 2) {
                        String temp = Command.get(2).toString().toLowerCase();
                        if (temp.startsWith("loocv")) {
                            NFolds = 0;
                        } else {
                            NFolds = Integer.parseInt(Command.get(2).toString());
                        }
                    }
                } catch (Exception e) {
                    throw new Exception("Usage: validate <ncomp> [<folds = 20>]");
                }
                crossvalidate(NComp, NFolds);
                if (NFolds > 0) {
                    System.out.println("\tRan " + NFolds + "-fold cross-validation.");
                } else {
                    System.out.println("\tRan leave-one-out cross-validation.");
                }
            } break;
            default:
                throw new Exception("CSP Command not recognized: " + Action);
        }
        return null;
    }

    @Override
    public String about() {
        return "CSP Engine based on " + KnownCompounds.size() + " known compounds";
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) {
            return about();
        }
        String Action = Command.get(0).toLowerCase();
        switch (Action) {
            case "stats":
                if (PerformanceStats.NResults() == 0) {
                    throw new Exception("CSP algorithm not yet validated.");
                }
                return PerformanceStats.printCommand(Command.subList(1, Command.size()));
            default:
                throw new Exception("CSPEngine print command not recognzied: " + Action);
        }
    }
    
    /**
     * Get a clone of a specific component of this CSP engine.
     * @param Name Name of component
     * @return Clone if that component
     * @throws Exception For various reasons (see text)
     */
    protected Object getComponent(String Name) throws Exception {
        switch (Name.toLowerCase()) {
            case "model": return Classifier.clone();
            default: throw new Exception("CSPEngine does not contain a " + Name);
        }
    }
}
