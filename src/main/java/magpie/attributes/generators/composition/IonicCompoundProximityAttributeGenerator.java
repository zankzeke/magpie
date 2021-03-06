package magpie.attributes.generators.composition;

import java.util.ArrayList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.filters.CompositionDistanceFilter;
import magpie.utility.tools.IonicCompoundFinder;

/**
 * Generate attributes based on the distance of a composition from compositions
 * that can form, charge-neutral ionic compounds.
 * 
 * <p>This generator only computes a single attribute: the L<sub>1</sub> distance
 * between the composition of an entry and the nearest ionic compound (determined
 * using {@linkplain IonicCompoundFinder}. For compound where it is not possible 
 * to form ionic compound (e.g., only metallic elements), the entry is assigned
 * arbitrarily large distance (equal to the number of elements in the alloy).
 * 
 * <p>The one adjustable parameter in this calculation is the maximum number
 * of atoms per formula unit used when looking for ionic compounds. For binary
 * compounds, the maximum conceivable number of elements in a formula unit is
 * for a compound with a 9+ and a 5- species, which has 14 atoms in the formula unit. 
 * Consequently, we recommend using 14 or larger for this parameter.
 * 
 * <usage><p><b>Usage</b>: &lt;formula unit size&gt; 
 * <pr><br><i>formula unit size</i>: Maximum number of formula unit size</usage>
 * 
 * @author Logan Ward
 * @see IonicCompoundFinder
 * @see CompositionDistanceFilter#computeDistance(CompositionEntry, CompositionEntry, int)
 */
public class IonicCompoundProximityAttributeGenerator extends 
        BaseAttributeGenerator {
    /** Maximum number of atoms per formula unit */
    private int MaxFormulaUnit = 14;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // Get maximum formula unit size
        int maxForm;
        try {
            maxForm = Integer.parseInt(Options.get(0).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setMaxFormulaUnit(maxForm);
        
        // Make sure there aren't any unwanted options
        if (Options.size() > 1) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <formula unit size>";
    }

    /**
     * Set the maximum formula unit size when searching for ionic compounds.
     * @param size Maximum size
     * @see IonicCompoundFinder
     */
    public void setMaxFormulaUnit(int size) {
        this.MaxFormulaUnit = size;
    }

    @Override
    public void addAttributes(Dataset dataPtr) throws Exception {
        // Make sure that data is a composition dataset
        if (! (dataPtr instanceof CompositionDataset)) {
            throw new IllegalArgumentException("data must be a CompositionDataset");
        }
        
        // Get a CompositionDataset pointer
        CompositionDataset data = (CompositionDataset) dataPtr;
        
        // Generate attribute names
        List<String> newNames = new ArrayList<>();
        newNames.add("IonicCompoundDistance_MaxSize" + MaxFormulaUnit);
        data.addAttributes(newNames);
        
        // Get ionic compound finder
        IonicCompoundFinder finder = new IonicCompoundFinder();
        finder.setLookupPath(data.getDataDirectory());
        finder.setMaxFormulaUnitSize(MaxFormulaUnit);
        
        // Loop through each entry
        double[] newAttrs = new double[newNames.size()];
        for (BaseEntry entryPtr : data.getEntries()) {
            // Set the maximum distance to be equal to the number of elements
            //  The maximum possible L_1 distance for an N element system is N
            CompositionEntry entry = (CompositionEntry) entryPtr;
            finder.setMaximumDistance(entry.getElements().length);

            // If the material only has 1 element, set attribute to be 1
            if (entry.getElements().length == 1) {
                newAttrs[0] = 1;           
            } else {  
                // Get the list of all ionic compounds in the system
                finder.setNominalComposition(entry);
                List<CompositionEntry> ionicCompounds = finder.findAllCompounds();

                // Find the distance to the closest one. If no other compounds,
                //  set distance to be maximum possible
                if (ionicCompounds.isEmpty()) {
                    newAttrs[0] = entry.getElements().length;
                } else {
                    newAttrs[0] = CompositionDistanceFilter.computeDistance(
                            entry, 
                            ionicCompounds.get(0),
                            1);
                }
            }
            
            entry.addAttributes(newAttrs);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(1) Distance of a composition to the nearest ionic compound"
                + " with up to " + MaxFormulaUnit + " atoms in the formula unit.";
        
        return output;
    }
}
