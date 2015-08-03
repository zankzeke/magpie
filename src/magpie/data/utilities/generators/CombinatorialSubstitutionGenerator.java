package magpie.data.utilities.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.util.LookupData;
import magpie.utility.CartesianSumGenerator;
import magpie.utility.NDGridIterator;
import org.apache.commons.lang3.ArrayUtils;
import vassal.data.Cell;

/**
 * Generate new crystalline compound by substituting elements onto all sites 
 * of a known prototype. 
 * 
 * <usage><p><b>Usage</b>: [-voro] $&lt;prototypes&gt; &lt;elements...&gt;
 * <br><pr><i>-voro</i>: Compute Voronoi tessellation of the prototype before
 * creating derivatives.
 * <br><pr><i>prototypes</i>: {@linkplain CrystalStructureDataset} containing
 * prototype structures (as entries).
 * <br><pr><i>elements</i>: List of elements to substitute</usage>
 * @author Logan Ward
 */
public class CombinatorialSubstitutionGenerator extends BaseEntryGenerator {
    /** List of elements to use (id is Z-1) */
    final protected Set<Integer> Elements = new TreeSet<>();
    /** List of prototype structures */
    final protected List<AtomicStructureEntry> Prototypes = new ArrayList<>();
    /** Whether to compute the Voronoi tessellation before generation */
    protected boolean ComputeVoronoi = false;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        boolean voro = false;
        Set<String> elems = new TreeSet<>();
        int pos = 0;
        
        try {
            // See if the user wants to compute tessellation first
            if (Options.get(pos).toString().toLowerCase().startsWith("-voro")) {
                voro = true;
                pos++;
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
        setElementsByAbbreviation(elems);
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Set list of elements to use for substitutions.
     * @param elements List of element IDs (Z-1)
     */
    public void setElements(Collection<Integer> elements) {
        Elements.clear();
        Elements.addAll(elements);
    }
    
    /**
     * Set list of elements to use for substitutions
     * @param elements List of elements by abbreviations
     * @throws java.lang.Exception If element not found
     */
    public void setElementsByAbbreviation(Collection<String> elements) throws Exception {
        // Lookup IDs
        Set<Integer> elemIDs = new TreeSet<>();
        for (String abbr : elements) {
            int elem = ArrayUtils.indexOf(LookupData.ElementNames, abbr);
            if (elem == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Exception("No such element: " + abbr);
            }
            elemIDs.add(elem);
        }
        
        setElements(elemIDs);
    }
    
    /**
     * Define the list of prototype structures to use to create entries.
     * @param prototypes List containing prototype structures
     */
    public void setPrototypes(List<AtomicStructureEntry> prototypes) {
        Prototypes.clear();
        Prototypes.addAll(prototypes);
    }
    
    /**
     * Define the list of prototype structures used to create entries.
     * @param data Dataset containing list of prototype structures
     */
    public void setPrototypes(CrystalStructureDataset data) {
        List<AtomicStructureEntry> prot = new ArrayList<>(data.NEntries());
        for (BaseEntry e : data.getEntries()) {
            prot.add((AtomicStructureEntry) e);
        }
        setPrototypes(prot);
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
            Iterator<AtomicStructureEntry> iter = Prototypes.iterator();
            while (iter.hasNext()) {
                try {
                    iter.next().computeVoronoiTessellation();
                } catch (Exception e) {
                    iter.remove(); // If tessellation fails, delete this structure
                }
            }
        }
        
        // Get iterator over prototypes
        final Iterator<AtomicStructureEntry> protIter = Prototypes.iterator();
        
        // Initialize iterator element combinations(use entry 0 in Prototypes list)
        final Iterator<List<Integer>> elemIterInitial =
                getReplacementIterator(Prototypes.get(0));
        
        return new Iterator<BaseEntry>() {
            Iterator<List<Integer>> elemIter = elemIterInitial;
            AtomicStructureEntry curProt = protIter.next();

            @Override
            public boolean hasNext() {
                return protIter.hasNext() || elemIter.hasNext();
            }

            @Override
            public BaseEntry next() {
                List<Integer> newElems;
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
                for (int i = 0; i < curStrc.nTypes(); i++) {
                    changes.put(curStrc.getTypeName(i),
                            LookupData.ElementNames[newElems.get(i)]);
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
    protected Iterator<List<Integer>> getReplacementIterator(
            AtomicStructureEntry entry) {
        // Get stacked list of all possible elements for each site
        Cell strc = entry.getStructure();
        List<Collection<Integer>> stack = new ArrayList<>(strc.nTypes());
        for (int i=0; i<strc.nTypes(); i++) {
            stack.add(Elements);
        }
        
        // Make the sum generator
        CartesianSumGenerator<Integer> gen = new CartesianSumGenerator<>(stack);
        return gen.iterator();
    }
    
}
