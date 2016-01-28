package magpie.attributes.generators;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;

/**
 * Use measured property of entry as an attribute. Requires dataset to extend
 * {@linkplain MultiPropertyDataset}. 
 * 
 * <p>Entries in dataset must contain a measurement for this property, otherwise
 * they are assigned {@linkplain Double#NaN} as the attribute value. If the dataset
 * does not contain the property as any of the options, {@linkplain #addAttributes(magpie.data.Dataset) }
 * will throw an exception.
 * 
 * <usage><p><b>Usage</b>: &lt;properties...&gt; 
 * <br><pr><i>properties</i>: List of properties to use as attributes.</usage>
 * 
 * @author Logan Ward
 */
public class PropertyAsAttributeGenerator extends BaseAttributeGenerator {
    /** List of properties to use as attributes. */
    protected Set<String> Properties = new TreeSet<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.isEmpty()) {
            throw new Exception(printUsage());
        }
        
        clearProperties();
        for (Object opt : Options) {
            addProperty(opt.toString());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <properties...>";
    }
    
    /**
     * Clear the list of properties being used as attributes.
     */
    public void clearProperties() {
        Properties.clear();
    }
    
    /**
     * Add property to list of properties used as attributes.
     * @param prop Property to use as attributes
     */
    public void addProperty(String prop) {
        Properties.add(prop);
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check whether dataset is a multiproperty dataset
        if (! (data instanceof MultiPropertyDataset)) {
            throw new Exception("Dataset must be a multi-property dataset");
        }
        MultiPropertyDataset dataPtr = (MultiPropertyDataset) data;
        
        // Loop through properties
        for (String prop : Properties) {
            // Get the data
            double[] values = dataPtr.getMeasuredPropertyArray(prop);
            
            // Add as attribute
            data.addAttribute(prop, values);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += " (" + Properties.size() + ") Measured properties of each entry:\n";
        
        if (htmlFormat) {
            output += "</br>";
        }
        
        // Loop over properties
        Iterator<String> iter = Properties.iterator();
        output += iter.next();
        while (iter.hasNext()) {
            output += ", " + iter.next();
        }
        
        return output;
    }
}
