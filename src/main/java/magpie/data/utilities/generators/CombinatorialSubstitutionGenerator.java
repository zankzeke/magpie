package magpie.data.utilities.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.util.LookupData;
import magpie.utility.CartesianSumGenerator;
import magpie.utility.MathUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;
import vassal.data.Cell;

/**
 * Generate new crystalline compound by substituting elements onto all sites 
 * of a known prototype. 
 * 
 * <usage><p><b>Usage</b>: [-voro] -style [all|permutations|combinations]
 * [-ignore &lt;elems to ignore&gt;] $&lt;prototypes&gt; &lt;elements...&gt;
 * <br><pr><i>-voro</i>: Compute Voronoi tessellation of the prototype before
 * creating derivatives.
 * <br><pr><i>-style</i>: How to choose new combinations of elements. "all"
 * all permutations with replacements, "permutations" all permutations without
 * replacement, "combinations" all combinations without replacement
 * <br><pr><i>elems to ignore</i>: List of elements that should not be replaced in 
 * the prototype (e.g., use this to generate all ABO<sub>3</sub> compounds)
 * <br><pr><i>prototypes</i>: {@linkplain CrystalStructureDataset} containing
 * prototype structures (as entries).
 * <br><pr><i>elements</i>: List of elements to substitute</usage>
 * @author Logan Ward
 */
public class CombinatorialSubstitutionGenerator extends BaseEntryGenerator {
    /** List of elements to substitute */
    final protected Set<String> Elements = new TreeSet<>();
    /** List of elements ignore during substitution */
    final protected Set<String> ElementsToIgnore = new TreeSet<>();
    /** List of prototype structures */
    final protected List<CrystalStructureEntry> Prototypes = new ArrayList<>();
    /** Types of enumeration */
    public enum EnumerationType {
        /** All permutations, with replacement */ ALL_POSSIBLE,
        /** All permutations, without replacement */ PERMUTATIONS,
        /** All combinations, without replacement */ COMBINATIONS
    }
    /** Enumeration type for generating new compounds */
    protected EnumerationType EnumerationStyle = EnumerationType.ALL_POSSIBLE;
    /** Whether to compute the Voronoi tessellation before generation */
    protected boolean ComputeVoronoi = false;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        boolean voro = false;
        Set<String> elems = new TreeSet<>();
        Set<String> elemsIgnore = new TreeSet<>();
        EnumerationType enumStyle;
        int pos = 0;
        
        try {
            // See if the user wants to compute tessellation first
            if (Options.get(pos).toString().toLowerCase().startsWith("-voro")) {
                voro = true;
                pos++;
            } 
            
            // Get style of permutations
            if (! Options.get(pos).toString().equalsIgnoreCase("-style")) {
                throw new Exception();
            }
            switch(Options.get(++pos).toString().toLowerCase()) {
                case "all": enumStyle = EnumerationType.ALL_POSSIBLE; break;
                case "permutations": enumStyle = EnumerationType.PERMUTATIONS; break;
                case "combinations": enumStyle = EnumerationType.COMBINATIONS; break;
                default: throw new Exception();
            }
            pos++;
            
            // See if the user wants to exclude anything
            if (Options.get(pos).toString().toLowerCase().startsWith("-ignore")) {
                pos++;
                while (Options.get(pos) instanceof String) {
                    elemsIgnore.add(Options.get(pos++).toString());
                }
            }

            // Get the list of prototypes
            CrystalStructureDataset data = (CrystalStructureDataset) Options.get(pos++);
            setPrototypes(data);
            
            // Get the list of elements
            for (int o=pos; o<Options.size(); o++) {
                elems.add(Options.get(o).toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set settings
        setComputeVoronoi(voro);
        setEnumerationStyle(enumStyle);
        setElementsToSubstitute(elems);
        setElementsToIgnore(elemsIgnore);
    }

    @Override
    public String printUsage() {
        return "Usage: [-voro] -style [all|permutations|combinations] [-ignore <elements to ignore...>] $<prototypes> <elements...>";
    }

    /**
     * Set list of elements to use for substitutions.
     * @param elements List of element abbreviations
     */
    public void setElementsToSubstitute(Collection<String> elements) {
        checkElements(elements);
        Elements.clear();
        Elements.addAll(elements);
    }

    /**
     * Given list of element abbreviations, make sure all are known elements
     * @param elements List of element abbreviation
     * @throws IllegalArgumentException If an element is not of a known type
     */
    protected void checkElements(Collection<String> elements) throws 
            IllegalArgumentException {
        for (String abbr : elements) {
            int elem = ArrayUtils.indexOf(LookupData.ElementNames, abbr);
            if (elem == ArrayUtils.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("No such element: " + abbr);
            }
        }
    }
    
    /**
     * Set list of elements that, if present, will not be replaced
     * @param elements Collection of element abbreviations
     */
    public void setElementsToIgnore(Collection<String> elements) {
        checkElements(elements);
        ElementsToIgnore.clear();
        ElementsToIgnore.addAll(elements);
    }
    
    /**
     * Define the list of prototype structures to use to create entries.
     * @param prototypes List containing prototype structures
     */
    public void setPrototypes(List<CrystalStructureEntry> prototypes) {
        Prototypes.clear();
        Prototypes.addAll(prototypes);
    }
    
    /**
     * Define the list of prototype structures used to create entries.
     * @param data Dataset containing list of prototype structures
     */
    public void setPrototypes(CrystalStructureDataset data) {
        List<CrystalStructureEntry> prot = new ArrayList<>(data.NEntries());
        for (BaseEntry e : data.getEntries()) {
            prot.add((CrystalStructureEntry) e);
        }
        setPrototypes(prot);
    }
    
    /**
     * Set method used to enumerate different sets of elements
     * @param type Enumeration style
     */
    public void setEnumerationStyle(EnumerationType type) {
        EnumerationStyle = type;
    }
    
    /**
     * Set whether to compute the Voronoi tessellation before generating entries.
     * Allows for faster attribute generation with {@linkplain CrystalStructureDataset}.
     * @param input Desired setting.
     */
    public void setComputeVoronoi(boolean input) {
        ComputeVoronoi = input;
    }
   
    @Override
    public Iterator<BaseEntry> iterator() {
        // If desired compute the Voronoi tessellation of the prototypes
        if (ComputeVoronoi) {
            Iterator<CrystalStructureEntry> iter = Prototypes.iterator();
            while (iter.hasNext()) {
                try {
                    iter.next().computeVoronoiTessellation();
                } catch (Exception e) {
                    iter.remove(); // If tessellation fails, delete this structure
                }
            }
        }
        
        // Get iterator over prototypes
        final Iterator<CrystalStructureEntry> protIter = Prototypes.iterator();
        
        // Initialize iterator element combinations(use entry 0 in Prototypes list)
        final Iterator<List<String>> elemIterInitial =
                getReplacementIterator(Prototypes.get(0));
        
        return new Iterator<BaseEntry>() {
            Iterator<List<String>> elemIter = elemIterInitial;
            CrystalStructureEntry curProt = protIter.next();

            @Override
            public boolean hasNext() {
                return protIter.hasNext() || elemIter.hasNext();
            }

            @Override
            public BaseEntry next() {
                List<String> newElems;
                // If there is another set of elements available
                if (elemIter.hasNext()) {
                    // Get the new element identities
                    newElems = elemIter.next();
                    
                    
                } else {
                    // Get a new prototype
                    curProt = protIter.next();
                    
                    // Restart the element iterator
                    elemIter = getReplacementIterator(curProt);
                    
                    // Get the first set of replacements
                    newElems = elemIter.next();
                }
            
                // Get the map of swaps to make
                Map<String, String> changes = new HashMap<>();
                Cell curStrc = curProt.getStructure();
                int pos=0;
                for (int type = 0; type < curStrc.nTypes(); type++) {
                    if (! ElementsToIgnore.contains(curStrc.getTypeName(type))) {
                        changes.put(curStrc.getTypeName(type),
                                newElems.get(pos++));
                    }
                }

                // Make the new entry
                try {
                    return curProt.replaceElements(changes);
                } catch (Exception e) {
                    // We should never get here. replaceElements only crashes
                    //  if element names fail to parse, which have already 
                    //  been checked.
                    throw new Error(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported."); 
            }
        };
    }
    
    /**
     * Get an iterator over all possible combinations of elements on each site 
     * of this structure
     * @param entry Entry to make the iterator for
     * @return Iterator over all combinations
     */
    protected Iterator<List<String>> getReplacementIterator(
            CrystalStructureEntry entry) {
        // Get stacked list of all possible elements for each site
        Cell strc = entry.getStructure();
        List<Collection<String>> stack = new ArrayList<>(strc.nTypes());
        for (int i=0; i<strc.nTypes(); i++) {
            if (! ElementsToIgnore.contains(strc.getTypeName(i))) {
                stack.add(Elements);
            }
        }
        
        // Generate all combinations
        switch (EnumerationStyle) {
            case ALL_POSSIBLE: {
                // Make the sum generator
                CartesianSumGenerator<String> gen = new CartesianSumGenerator<>(stack);
                return gen.iterator();
            } 
            case PERMUTATIONS: case COMBINATIONS: {
                // Create a list of element names
                final List<String> elems = new ArrayList<>(Elements);
                
                // Generate all permutations
                int[] comb = new int[stack.size()];
                Arrays.fill(comb, elems.size());
                
                // Create iterator over combations
                final Iterator<int[]> combIter = EnumerationStyle == EnumerationType.COMBINATIONS ?
                        CombinatoricsUtils.combinationsIterator(elems.size(), stack.size())
                        : MathUtils.permutationIterator(elems.size(), stack.size());
                
                // Wrapper that transforms int[] to List<String>
                return new Iterator<List<String>>() {

                    @Override
                    public boolean hasNext() {
                        return combIter.hasNext();
                    }

                    @Override
                    public List<String> next() {
                        int[] nextInts = combIter.next();
                        List<String> output = new ArrayList<>(nextInts.length);
                        for (int elem : nextInts) {
                            output.add(elems.get(elem));
                        }
                        return output;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException(); 
                    }
                };
            }
            default: 
                throw new RuntimeException("Enumeration style not supported: " + EnumerationStyle.name());
        }
    }
    
}
