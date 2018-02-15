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
import magpie.data.materials.CrystalStructureEntry;
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
 * <p>There are two options for computing order parameters: Weighted and unweighted.
 * The former is computed by weighing the contribution of each neighboring atom
 * by the fraction of surface area corresponding to boundaries between that atom
 * and the central atom. The former considers all neighbors weighted equally, which
 * means they are very sensitive to the introduction of small faces due to numerical
 * problems inherent to the Voronoi tessellation. Full details is available in 
 * the Vassal documentation for 
 * {@linkplain VoronoiCellBasedAnalysis#getNeighborOrderingParameters(int, boolean) }.
 * 
 * <usage><p><b>Usage</b>: [-unweighted] &lt;shells...&gt;
 * <br><pr><i>-unweighted</i>: Do not weigh WC parameters with face areas.
 * <br><pr><i>shells</i>: List of which nearest-neighbor shells to compute the 
 * mean WC parameter for (default: 1 2 3).</usage>
 * 
 * 
 * @author Logan Ward
 */
public class ChemicalOrderingAttributeGenerator extends BaseAttributeGenerator {
    /** Shells to compute the WC attribute for */
    final private Set<Integer> Shells = new TreeSet<>();
    /** Whether to compute weighted WC ordering parameters. */
    private boolean Weighted = true;

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
        boolean weighted = true;
        try {
            // Check if first tag is "-unweighted"
            if (Options.get(0).toString().equalsIgnoreCase("-unweighted")) {
                weighted = false;
            }
            
            // Get shell 
            for (Object option : Options.subList(weighted ? 0 : 1, Options.size())) {
                Integer shell = Integer.parseInt(option.toString());
                shells.add(shell);
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set settings
        setWeighted(weighted);
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

    /**
     * Set whether to consider face sizes when computing ordering parameters.
     * @param weighted Whether to weigh using face sizes
     */
    public void setWeighted(boolean weighted) {
        this.Weighted = weighted;
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
            newAttrs.add("mean_WCMagnitude" + (Weighted ? "" : "_unweighted_") + "_Shell" + shell);
        }
        data.addAttributes(newAttrs);
        
        // Compute attributes
        double[] temp = new double[newAttrs.size()];
        for (BaseEntry ptr : data.getEntries()) {
            // Get the Voronoi tessellation
            CrystalStructureEntry entry = (CrystalStructureEntry) ptr;
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
                temp[pos++] = voro.warrenCowleyOrderingMagnituide(shell, Weighted);
            }
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(" + Shells.size() + ") Mean magnitude of the";
        if (Weighted) {
            output += " face-area-weighted";
        }
        output += " Warren-Cowley ordering parameters" 
                + "for the following nearest-neighbor shells:";
        for (Integer shell : Shells) {
            output += " " + shell;
        }
        
        return output;
    }
    
}
