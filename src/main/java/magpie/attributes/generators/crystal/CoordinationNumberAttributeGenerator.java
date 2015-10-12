package magpie.attributes.generators.crystal;

import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute attributes based on the coordination number. Uses the Voronoi 
 * tessellation to define the coordination network.
 * 
 * <p>DEV NOTE (LW 15Jul15): Could benefit from adding a face size cutoff, where
 * atoms are only defined as coordinated if the face between them is larger than
 * a certain fraction of the surface area of both cells. Otherwise faces on the 
 * cells that are only present to numerical issues will be counted as neighbors.
 * Metallic glass community commonly removes any faces smaller than 1% of the 
 * total surface area of a cell.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class CoordinationNumberAttributeGenerator extends BaseAttributeGenerator {

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
        newAttrs.add("mean_Coordination");
        newAttrs.add("var_Coordination");
        newAttrs.add("min_Coordination");
        newAttrs.add("max_Coordination");
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
            temp[pos++] = voro.faceCountAverage();
            temp[pos++] = voro.faceCountVariance();
            temp[pos++] = voro.faceCountMinimum();
			temp[pos++] = voro.faceCountMaximum();
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(4) Mean, variance, minimum, and maximum coordination number";
        
        return output;
    }
    
}
