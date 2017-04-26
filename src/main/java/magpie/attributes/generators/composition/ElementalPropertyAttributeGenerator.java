package magpie.attributes.generators.composition;

import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

import java.util.*;

/**
 * Generate attributes based on elemental property statistics. Computes the mean,
 * maximum, minimum, range, mode, and mean absolute deviation of all elemental 
 * properties stored in {@linkplain CompositionDataset#ElementalProperties}.
 *
 * <p>
 *     If the value of an elemental property is missing for <i>any</i> element in a composition, the value of all attributes
 *     for that composition will be set to Missing (i.e., NaN).
 * </p>
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
            throw new IllegalArgumentException(printUsage());
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
        int attributesPerProperty = 6;
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
        assert newNames.size() == attributesPerProperty * ElementalProperties.size();

        // Generate attributes for each entry
        Map<String,Set<String>> missingData = new TreeMap<>();
        double[] toAdd = new double[newNames.size()];
        for (int e = 0; e < ptr.NEntries(); e++) {
            CompositionEntry entry = ptr.getEntry(e);
            int count = 0;

            // Generate data for each property
            for (String prop : ElementalProperties) {
                // Get the lookup table for this property
				double[] lookup = ptr.getPropertyLookupTable(prop);

                // Check if any required lookup-data is missing;
                int[] elems = entry.getElements();
                boolean hasMissing = false;
                for (int i = 0; i < elems.length; i++) {
                    if (Double.isNaN(lookup[elems[i]])) {
                        hasMissing = true;
                        if (missingData.containsKey(prop)) {
                            missingData.get(prop).add(ptr.ElementNames[elems[i]]);
                        } else {
                            Set<String> temp = new TreeSet<>();
                            temp.add(ptr.ElementNames[elems[i]]);
                            missingData.put(prop, temp);
                        }
                    }
                }

                // If there is missing data, all attributes should be
                if (hasMissing) {
                    Arrays.fill(toAdd, count, count + attributesPerProperty, Double.NaN);
                    count += attributesPerProperty;
                } else {
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
            }

            // Add attributes to entry
            entry.addAttributes(toAdd);
        }

        // Print out warning of which properties have missing data
        if (missingData.size() > 0) {
            System.err.println("WARNING: There are " + missingData.size()
                    + " elmental properties with missing values:");
            int i = 0;
            for (Map.Entry<String,Set<String>> entry : missingData.entrySet()) {
                String prop = entry.getKey();
                System.err.print("\t" + prop + ":");
                for (String elem : entry.getValue()) {
                    System.err.print(" " + elem);
                }
                System.err.println();
            }
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        StringBuilder output = new StringBuilder(getClass().getName() + (htmlFormat ? " " : ": "));
        
        // Print out number of attributes
        output.append(" (").append(ElementalProperties.size() * 6).append(") ");
        
        // Print out description
        output.append("Minimum, mean, maximum, mode, range, and mean absolute deviation" + " of ")
                .append(ElementalProperties.size()).append(" elemental properties:\n");
        
        // Print out elemental properties
        if (htmlFormat) {
            output.append("<br>");
        }
        boolean started = false;
        for (String prop : ElementalProperties) {
            if (started) {
                output.append(", ");
            }
            output.append(prop);
            started = true;
        }

        return output.toString();
    }
    
}
