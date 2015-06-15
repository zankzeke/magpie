package magpie.attributes.generators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Generate attributes based on elemental property statistics. Computes the mean,
 * maximum, minimum, range, mode, and mean absolute deviation of all elemental 
 * properties stored in {@linkplain CompositionDataset#ElementalProperties}. 
 * 
 * <usage><p><b>Usage</b>: No options.</usage>
 * 
 * @author Logan Ward
 */
public class ElementalPropertyAttributeGenerator extends BaseAttributeGenerator {
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
        // Check if this is an composition dataset
        if (! (data instanceof CompositionDataset)) {
            throw new Exception("Data isn't a CompositionDataset");
        }
        CompositionDataset ptr = (CompositionDataset) data;
        
        // Create attribute names
        ElementalProperties = ptr.getElementalProperties();
        List<String> newNames = new ArrayList<>(ElementalProperties.size());
        for (String prop : ElementalProperties) {
            newNames.add("mean_" + prop);
            newNames.add("maxdiff_" + prop);
            newNames.add("dev_" + prop);
            newNames.add("max_" + prop);
            newNames.add("min_" + prop);
            newNames.add("most_" + prop);
        }
        ptr.addAttributes(newNames);

        // Generate attributes for each entry
        List<String> MissingData = new ArrayList<>();
        double[] toAdd = new double[newNames.size()];
        for (int e = 0; e < ptr.NEntries(); e++) {
            CompositionEntry entry = ptr.getEntry(e);
            int count = 0;

            // Generate data for each property
            for (String prop : ElementalProperties) {
                // Get the lookup table for this property
				double[] lookup = ptr.getPropertyLookupTable(prop);

                // Check if any required lookup data is missing;
                int[] elems = entry.getElements();
                for (int i = 0; i < elems.length; i++) {
                    if (Double.isNaN(lookup[elems[i]])) {
                        MissingData.add(ptr.ElementNames[elems[i]] + ":" + prop);
                    }
                }

                // Calculate the mean
                double mean = entry.getMean(lookup);
                toAdd[count++] = mean;

                // Calculate the maximum diff
                toAdd[count++] = entry.getMaxDifference(lookup);
                // Calculate the mean deviation
                toAdd[count++] = entry.getAverageDeviation(lookup, mean);
                toAdd[count++] = entry.getMaximum(lookup);
                toAdd[count++] = entry.getMinimum(lookup);
                toAdd[count++] = entry.getMost(lookup);
            }

            // Add attributes to entry
            entry.addAttributes(toAdd);
        }

        // Print out warning of which properties have missing (see def of 
        //    OffendingProperties)
        if (MissingData.size() > 0) {
            System.err.println("WARNING: There are " + MissingData.size()
                    + " missing elmental properties:");
            int i = 0;
            Iterator<String> iter = MissingData.iterator();
            while (iter.hasNext()) {
                System.err.format("%32s", iter.next());
                if (i % 2 == 1) {
                    System.err.println();
                }
                i++;
            }
            if (i % 2 == 1) {
                System.err.println();
            }
            System.err.println();
            System.err.flush();
        }
    }

    @Override
    public String printDescription(boolean htmlDescription) {
        String output = getClass().getName() + ":";
        
        // Print out number of attributes
        output += " (" + (ElementalProperties.size() * 6) + ") ";
        
        // Print out description
        output += "Minimum, mean, maximum, mode, range, and mean absolute deviation"
                + " of " + ElementalProperties.size() + " elemental properties:\n";
        
        // Print out elemental properties
        if (htmlDescription) {
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
