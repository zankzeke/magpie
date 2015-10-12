package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute attributes based on chemical ordering of structure. Determines
 * average Warren-Cowley ordering parameter for the bond network defined 
 * by the Voronoi tessellation of a structure.
 * 
 * <p>For each atom in the structure, the average Warren-Cowley ordering parameter
 * is determined by computing the average magnitude of ordering parameter for 
 * each type for all atoms in a structure. The ordering parameter is 0 for 
 * a perfectly-random distribution, so this average represents an average 
 * degree of "ordering" in the structure. This attribute is computed for 
 * several nearest-neighbor shells (1st, 2nd, and 3rd by default).
 * 
 * <usage><p><b>Usage</b>: &lt;shells...&gt;
 * <br><pr><i>shells</i>: List of which nearest-neighbor shells to compute the 
 * mean WC parameter for (default: 1 2 3).</usage>
 * 
 * 
 * @author Logan Ward
 */
public class ChemicalOrderingAttributeGenerator extends BaseAttributeGenerator {
    /** Shells to compute the WC attribute for */
    final private Set<Integer> Shells = new TreeSet<>();

    /**
     * Create a default attribute generator. Will compute WC parameters for 
     * the first, second and third nearest-neighbor shells.
     */
    public ChemicalOrderingAttributeGenerator() {
        Shells.add(1); Shells.add(2); Shells.add(3);
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        Set<Integer> shells = new TreeSet<>();
        try {
            for (Object option : Options) {
                Integer shell = Integer.parseInt(option.toString());
                shells.add(shell);
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set settings
        setShells(shells);
    }

    @Override
    public String printUsage() {
        return "Usage: <shells...>";
    }
    
    /**
     * Define which nearest-neighbor shells to consider when generating attributes.
     * @param shells List of shell indices
     */
    public void setShells(Collection<Integer> shells) {
        Shells.clear();
        Shells.addAll(shells);
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check to make sure dataset hold crystal structures
        if (! (data instanceof CrystalStructureDataset)) {
            throw new Exception("Dataset doesn't contain crystal structures");
        }
        
        // Create attribute names
        List<String> newAttrs = new ArrayList<>();
        for (Integer shell : Shells) {
            newAttrs.add("mean_WCMagnitude_Shell" + shell);
        }
        data.addAttributes(newAttrs);
        
        // Compute attributes
        double[] temp = new double[newAttrs.size()];
        for (BaseEntry ptr : data.getEntries()) {
            // Get the Voronoi tessellation
            AtomicStructureEntry entry = (AtomicStructureEntry) ptr;
            VoronoiCellBasedAnalysis voro;
            try {
                voro = entry.computeVoronoiTessellation();
            } catch (Exception e) {
                Arrays.fill(temp, Double.NaN); // If tessellation fails
                entry.addAttributes(temp);
                continue;
            }
            
            // Compute the attributes
            int pos = 0;
            for (Integer shell : Shells) {
                temp[pos++] = voro.warrenCowleyOrderingMagnituide(shell);
            }
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(" + Shells.size() + ") Mean magnitude of Warren-Cowley "
                + "ordering parameter for the following nearest-neighbor shells:";
        for (Integer shell : Shells) {
            output += " " + shell;
        }
        
        return output;
    }
    
}
