package magpie.attributes.generators.element;

import java.util.ArrayList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.ElementDataset;
import magpie.data.materials.ElementEntry;

/**
 * Compute attributes based on elemental properties. Uses list of properties
 * from {@linkplain ElementDataset#getElementalProperties() }.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * @author Logan Ward
 */
public class ElementalPropertyAttributeGenerator extends BaseAttributeGenerator {
    /** List of element properties to use as attributes */
    private List<String> ElementalProperties = new ArrayList<>();

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
    public void addAttributes(Dataset ptr) throws Exception {
        if (! (ptr instanceof ElementDataset)) {
            throw new Exception("Data must be an ElementDataset");
        }
        ElementDataset data = (ElementDataset) ptr;
        
        // Get elemental properties
        ElementalProperties = data.getElementalProperties();
        
        // Add attribute names
        data.addAttributes(ElementalProperties);
        
        // Create attributes
        double[] newAttr = new double[ElementalProperties.size()];
        for (BaseEntry ePtr : data.getEntries()) {
            ElementEntry entry = (ElementEntry) ePtr;
            
            // Compute attributes
            int pos = 0;
            for (String prop : ElementalProperties) {
                // Get lookup table
                double[] lookup = data.getLookupTable(prop);
                
                // Get attribute
                newAttr[pos++] = entry.getLookupValue(lookup);
            }
            
            // Add them to entry
            entry.addAttributes(newAttr);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(" + ElementalProperties.size() + ")";
        output += " Elemental properties:\n";
        if (htmlFormat) {
            output += "</br>";
        }
        
        // List properties
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
