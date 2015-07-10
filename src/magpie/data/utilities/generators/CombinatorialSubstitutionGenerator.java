package magpie.data.utilities.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.util.LookupData;
import magpie.utility.CartesianSumGenerator;
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
    public List<BaseEntry> generateEntries() {
        // If desired, compute the Voronoi tessellations
        if (ComputeVoronoi) {
            Iterator<AtomicStructureEntry> iter = Prototypes.iterator();
            while (iter.hasNext()) {
                AtomicStructureEntry e = iter.next();
                try {
                    e.computeVoronoiTessellation();
                } catch (Exception ex) {
                    iter.remove(); // If the tessellation fails
                }
            }
        }
        
        // For each entry, compute the the expansions
        List<BaseEntry> output = new ArrayList<>();
        for (AtomicStructureEntry prototype : Prototypes) {
            // Get the current list of elements
            Cell strc = prototype.getStructure();
            List<String> curTypes = new ArrayList<>(strc.nTypes());
            for (int i=0; i<strc.nTypes(); i++) {
                curTypes.add(strc.getTypeName(i));
            }
            
            // Prepare combinatorial generator
            List<Collection<Integer>> stackedSets = new ArrayList<>(curTypes.size());
            for (int i=0; i<strc.nTypes(); i++) {
                stackedSets.add(Elements);
            }
            CartesianSumGenerator<Integer> combGen = new CartesianSumGenerator<>(stackedSets);
            
            // Create all combinations
            Map<String,String> changes = new TreeMap<>();
            for (List<Integer> comb : combGen) {
                // Mark the changes to be made
                for (int i=0; i<curTypes.size(); i++) {
                    changes.put(curTypes.get(i), LookupData.ElementNames[comb.get(i)]);
                }
                
                // Generate a new entry based on the these changes
                AtomicStructureEntry newEntry;
                try {
                    newEntry = prototype.replaceElements(changes);
                } catch (Exception ex) {
                    continue; // I doubt this will ever happen. This operation
                    // only throws exceptions if the elements fair to parse,
                    // which are checked well before this stage.
                }
                
                // Add it to the output
                output.add(prototype);
            }
        }
        
        return output;
    }
    
}
