package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.utility.MathUtils;
import org.apache.commons.math3.stat.StatUtils;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute attributes based on heterogeneity in structure. Measures variance in 
 * bond lengths (both for a single atom and between different atoms) and atomic volumes.
 * Also considers the number of unique coordination polyhedron shapes.
 * 
 * <p>Bond lengths, atomic volumes, and coordination polyhedra are based on the 
 * Voronoi tessellation of the structure.
 * 
 * <p>Current attributes:
 * <ol>
 * <li>Mean absolute deviation in average bond length for each atom, normalized
 * by mean for all atoms
 * <li>Minimum in average bond length, normalized by mean for all atoms
 * <li>Maximum in average bond length, normalized by mean for all atoms
 * <li>Mean bond length variance between bonds across all atom
 * <li>Mean absolute deviation in bond length variance
 * <li>Minimum bond length variance
 * <li>Maximum bond length variance
 * <li>Mean absolute deviation in atomic volume, normalized by mean atomic volume
 * <li>Number of unique coordination polyhedron shapes
 * </ol>
 * 
 * <p>Here, bond length variation for a single atom is defined as:
 * 
 * <p>\(\hat{l}=\langle l_i - \bar{l} \rangle\)
 * 
 * <p>where \(l_i\) is the distance between an atom and one of its neighbors.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class StructuralHeterogeneityAttributeGenerator extends BaseAttributeGenerator {
    
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
        newAttrs.add("var_MeanBondLength");
        newAttrs.add("min_MeanBondLength");
        newAttrs.add("max_MeanBondLength");
        newAttrs.add("mean_BondLengthVariation");
        newAttrs.add("var_BondLengthVariation");
        newAttrs.add("min_BondLengthVariation");
        newAttrs.add("max_BondLengthVariation");
        newAttrs.add("var_CellVolume");
        newAttrs.add("UniquePolyhedronShapes");
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
            
            // Bond length attributes
            //    Variation between cells
            double[] meanBondLengths = voro.meanBondLengths();
            double lengthScale = StatUtils.mean(meanBondLengths);
            for (int i=0; i<meanBondLengths.length; i++) {
                meanBondLengths[i] /= lengthScale; // Normalize bond lengths
            }
            temp[pos++] = MathUtils.meanAbsoluteDeviation(meanBondLengths);
            temp[pos++] = StatUtils.min(meanBondLengths);
            temp[pos++] = StatUtils.max(meanBondLengths);
            
            //     Variation within a single cell
            meanBondLengths = voro.meanBondLengths(); // Recompute bond lengths
            double[] bondLengthVariation = voro.bondLengthVariance(meanBondLengths);
            for (int i=0; i<bondLengthVariation.length; i++) {
                // Normalize bond length variation by mean bond length of each cell
                bondLengthVariation[i] /= meanBondLengths[i];
            }
            temp[pos++] = StatUtils.mean(bondLengthVariation);
            temp[pos++] = MathUtils.meanAbsoluteDeviation(bondLengthVariation);
            temp[pos++] = StatUtils.min(bondLengthVariation);
            temp[pos++] = StatUtils.max(bondLengthVariation);
            
            // Cell volume / shape attributes
            temp[pos++] = voro.volumeVariance() * 
                    entry.getStructure().nAtoms() / entry.getStructure().volume();
			temp[pos++] = (double) voro.getUniquePolyhedronShapes().size();
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(9) Measures of structural heterogeneity: Variation in "
                + "average bond length for atoms, variation in bond distance "
                + "between neighbors of an atom, varatioin in atomic volume, "
                + "and number of unique coordination polyhedron shapes.";
        
        return output;
    }
}
