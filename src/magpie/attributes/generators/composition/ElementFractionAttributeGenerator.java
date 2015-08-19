package magpie.attributes.generators.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;

/**
 * Stores the fraction of each element as attributes.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class ElementFractionAttributeGenerator extends BaseAttributeGenerator {
    
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
        // Check if this is an composition dataset
        if (! (data instanceof CompositionDataset)) {
            throw new Exception("Data isn't a CompositionDataset");
        }
        CompositionDataset ptr = (CompositionDataset) data;
        
        // Create attribute names
        List<String> newAttrNames = new ArrayList<>();
        for (String elem : LookupData.ElementNames) {
            newAttrNames.add("X_" + elem);
        }
        data.addAttributes(newAttrNames);
        
        // Compute attributes
        double[] newAttrs = new double[newAttrNames.size()];
        for (BaseEntry ePtr : data.getEntries()) {
            Arrays.fill(newAttrs, 0); // Mark everything as zero
            
            // Save the element fractions
            CompositionEntry entry = (CompositionEntry) ePtr;
            int[] elems = entry.getElements();
            double[] fracs = entry.getFractions();
            for (int i=0; i<elems.length; i++) {
                newAttrs[elems[i]] = fracs[i];
            }
            
            // Store attributes
            entry.addAttributes(newAttrs);
        }
    }
    
    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Print out number of attributes
        output += " (" + LookupData.ElementNames.length + ") ";
        
        output += " Fraction of each element";
        
        return output;
    }
    
}
