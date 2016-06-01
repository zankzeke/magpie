package magpie.attributes.generators.crystal;

import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.utility.MathUtils;
import org.apache.commons.math3.stat.StatUtils;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute attributes based on the effective coordination number. The effective 
 * coordination number can be thought of as a face-size-weighted coordination number.
 * It is computed by the formula
 * 
 * <center><i>N<sub>eff</sub></i> = 1 / sum[(<i>f<sub>i</sub></i>
 * / <i>SA</i>)<sup>2</sup>]</center>
 * 
 * where <i>f<sub>i</sub></i> is the area of face <i>i</i> and SA is the surface
 * area of the entire cell. 
 * 
 * <p>The effective coordination number has major benefit: stability against the
 * additional of a very small face. Small perturbations in atomic positions
 * can break symmetry in a crystal, and lead to the introduction of small faces.
 * The conventional coordination number treats all faces equally, so the coordination
 * number changes even when one of these small faces is added. 
 * 
 * <p>One approach in the literature is to first apply a screen on small
 * faces (e.g., remove any smaller than 1% of the total face area), which still
 * runs into problems with discontinuity for larger displacements.
 * 
 * <p>Our approach is differentiable with respect to the additional of a small face 
 * (ask Logan if you want the math), and also captures another interesting effect
 * small coordination numbers for Voronoi cells with a dispersity in face sizes.
 * For example, BCC has 14 faces on its voronoi cell. 8 large faces, and 6 small ones.
 * Our effective face size identifies a face size of closer to 8, the commonly-accepted
 * value of the BCC coordination number, than 14 reported by the conventional measure.
 * Additional, for systems with equal-sized faces (e.g., FCC), this measure
 * agrees exactly with conventional reports.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class EffectiveCoordinationNumberAttributeGenerator extends BaseAttributeGenerator {

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
        newAttrs.add("mean_EffectiveCoordination");
        newAttrs.add("var_EffectiveCoordination");
        newAttrs.add("min_EffectiveCoordination");
        newAttrs.add("max_EffectiveCoordination");
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
            double[] N_eff = voro.getEffectiveCoordinationNumbers();
            temp[pos++] = StatUtils.mean(N_eff);
            temp[pos++] = MathUtils.meanAbsoluteDeviation(N_eff, temp[0]);
            temp[pos++] = StatUtils.min(N_eff);
			temp[pos++] = StatUtils.max(N_eff);
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(4) Mean, variance, minimum, and maximum effective coordination number,"
                + " which is defined as the inverse of the sum of the squares over the"
                + " fraction of the total surface area of each face.";
        
        return output;
    }
    
}
