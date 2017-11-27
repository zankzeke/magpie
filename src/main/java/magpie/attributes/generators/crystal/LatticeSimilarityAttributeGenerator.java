package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute similarity of structure to several simple lattices. Determined by 
 * comparing the shape of each coordination polyhedron in the structure (as determined
 * using a Voronoi tessellation) to those in a reference lattice.
 * 
 * <p>Similarity is computed by summing the difference in the number of faces
 * with each number of edges between a certain Voronoi cell and that of the reference 
 * lattice. This difference is then normalized by the number of faces in the 
 * reference lattice, and averaged over all atoms to produce a "similarity index." 
 * In this form, structures based on the reference lattice have a match of 0, which becomes 
 * larger with increase dissimilarity.
 * 
 * <p>For now we consider the BCC, FCC (which has the same coordination polyhedron
 * shape as HCP), and SC lattices.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class LatticeSimilarityAttributeGenerator extends BaseAttributeGenerator {
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check to make sure dataset hold crystal structures
        if (! (data instanceof CrystalStructureDataset)) {
            throw new Exception("Dataset doesn't contain crystal structures");
        }
        
        // Create attribute names
        List<String> newAttrs = new ArrayList<>();
        newAttrs.add("dissimilarity_FCC");
        newAttrs.add("dissimilarity_BCC");
        newAttrs.add("dissimilarity_SC");
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
            temp[pos++] = voro.meanFCCDissimilarity();
            temp[pos++] = voro.meanFCCDissimilarity();
            temp[pos++] = voro.meanSCDissimilarity();
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(3) Average similarity of each coordination polyhedron "
                + "to the BCC, FCC, and SC lattices.";
        
        return output;
    }
    
}
