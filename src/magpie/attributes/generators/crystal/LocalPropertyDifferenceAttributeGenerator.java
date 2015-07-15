package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute attributes based on the difference in elemental properties between
 * neighboring atoms. For an atom, its "local property difference" is computed by:
 * 
 * <br>\(\frac{\sum_{n}{f_{n} * \left|p_{atom}-p_{n}\right|}}{\sum_{n}{f_n}}\)
 * 
 * <p>where \(f_n\) is the area of the face associated with neighbor n,
 * \(p_{atom}\) the the elemental property of the central atom, and 
 * \(p_n\) is the elemental property of the neighbor atom.
 * 
 * <p>This parameter is computed for all elemental properties stored in 
 * {@linkplain CrystalStructureDataset#ElementalProperties}.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class LocalPropertyDifferenceAttributeGenerator extends BaseAttributeGenerator {
    /** Elemental properties used to generate attributes */
    private List<String> ElementalProperties = null;
    
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
        CrystalStructureDataset dPtr = (CrystalStructureDataset) data;
        
        // Create attribute names
        ElementalProperties = dPtr.getElementalProperties();
        List<String> newAttr = new ArrayList<>();
        for (String prop : ElementalProperties) {
            newAttr.add("mean_NeighDiff_" + prop);
            newAttr.add("var_NeighDiff_" + prop);
            newAttr.add("min_NeighDiff_" + prop);
            newAttr.add("max_NeighDiff_" + prop);
        }
        data.addAttributes(newAttr);
        
        // Compute attributes
        double[] temp = new double[newAttr.size()];
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
            
            // Get the elements corresponding to each type
            int[] elemIndex = new int[entry.getStructure().nTypes()];
            for (int i=0; i<elemIndex.length; i++) {
                elemIndex[i] = ArrayUtils.indexOf(LookupData.ElementNames, 
                        entry.getStructure().getTypeName(i));
            }
            double[] propValues = new double[elemIndex.length];
            
            // Loop through each elemental property
            for (String prop : ElementalProperties) {
                // Get properties for elements in this structure
                double[] lookupTable;
                try {
                    lookupTable = dPtr.getPropertyLookupTable(prop);
                } catch (Exception e) {
                    throw new Error(e);
                }
                for (int i=0; i<propValues.length; i++) {
                    propValues[i] = lookupTable[elemIndex[i]];
                }
                
                // Compute the neighbor differences for each atom
                double[] neighDiff;
                try {
                    neighDiff = voro.neighborPropertyDifferences(propValues);
                } catch (Exception e) {
                    throw new Error(e);
                }
                temp[pos++] = StatUtils.mean(neighDiff);
                double[] meanDeviation = neighDiff.clone();
                for (int i=0; i<meanDeviation.length; i++) {
                    meanDeviation[i] = Math.abs(meanDeviation[i] 
                            - temp[pos - 1]);
                }
                temp[pos++] = StatUtils.mean(meanDeviation);
                temp[pos++] = StatUtils.min(neighDiff);
                temp[pos++] = StatUtils.max(neighDiff);
            }
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }
    
    

    @Override
    public String printDescription(boolean htmlFormat) {
            String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Print out number of attributes
        output += " (" + (ElementalProperties.size() * 4) + ") ";
        
        // Print out description
        output += "Mean, maximum, minium, and mean absolute deviation in the "
                + "difference between the elemental properties between an atom "
                + "and its nearest neighbors "
                + "for " + ElementalProperties.size() + " elemental properties:\n";
        
        // Print out elemental properties
        if (htmlFormat) {
            output += "<br>";
        }
        boolean started = false;
        for (String prop : ElementalProperties) {
            if (started) {
                output += ", ";
            }
            output += prop;
            started = true;
        }
        
        return output;
    }
    
    
}
