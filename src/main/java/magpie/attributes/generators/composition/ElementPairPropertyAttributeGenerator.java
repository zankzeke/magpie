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
import org.apache.commons.math3.stat.StatUtils;

/**
 * Attributes based on properties of constituent binary systems. Computes the minimum,
 * maximum, and range of all pairs in the material, and the weighted mean and variance
 * of all pairs. Pairs are weighted by the product of the fractions of each element.
 * So, the weighted mean can be expressed as:
 * 
 * <center><math>sum<sub>i,j</sub>[ x<sub>i</sub>*x<sub>j</sub>*f(i,j) ] / 
 * sum<sub>i,j</sub>[ x<sub>i</sub>*x<sub>j</sub> ]</center>
 * 
 * where x<sub>j</sub> is the fraction of element j, and f(i,j) is the property
 * of the pair of i and j. Variance is defined as the mean absolute deviation
 * from the mean over all pairs, and is weighted using a similar scheme.
 * 
 * <p>If an entry only has one element. The value of NaN is used for all attributes
 * 
 * <usage><p><b>Usage</b>: *No Options*</usage>
 * @author Logan Ward
 */
public class ElementPairPropertyAttributeGenerator extends BaseAttributeGenerator {
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
        return "Usage: *No Options*";
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check if this is an composition dataset
        if (! (data instanceof CompositionDataset)) {
            throw new Exception("Data isn't a CompositionDataset");
        }
        CompositionDataset ptr = (CompositionDataset) data;
        
        // Create attribute names
        ElementalProperties = ptr.getElementPairProperties();
        List<String> newNames = new ArrayList<>(ElementalProperties.size());
        for (String prop : ElementalProperties) {
            newNames.add("binary_max_" + prop);
            newNames.add("binary_min_" + prop);
            newNames.add("binary_range_" + prop);
            newNames.add("binary_mean_" + prop);
            newNames.add("binary_variance_" + prop);
        }
        ptr.addAttributes(newNames);
        
        // Compute the attributes
        double[] newAttrs = new double[newNames.size()];
        for (BaseEntry entryPtr : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) entryPtr;
            int[] elems = entry.getElements();
            double[] fracs = entry.getFractions();
            
            // Check if this is a pure compound
            if (fracs.length == 1) {
                Arrays.fill(newAttrs, Double.NaN);
                entry.addAttributes(newAttrs);
                continue;
            }
            
            // Get the weights for each pair
            double[] pairWeights = new double[(elems.length - 1) * elems.length / 2];
            int pos = 0;
            for (int i=0; i<fracs.length; i++) {
                for (int j=0; j<i; j++) {
                    pairWeights[pos++] = fracs[i] * fracs[j];
                }
            }
            double totalSum = StatUtils.sum(pairWeights);
            for (int v=0; v<pairWeights.length; v++) {
                pairWeights[v] /= totalSum;
            }
            
            // Loop through each property
            double[] pairValues = new double[pairWeights.length];
            int attrPos = 0;
            for (String prop : ElementalProperties) {
                // Get lookup table
                double[][] table = ptr.getPairPropertyLookupTable(prop);
                
                // Get the binary values
                pos=0;
                for (int i=0; i<fracs.length; i++) {
                    for (int j=0; j<i; j++) {
                        pairValues[pos++] = 
                                LookupData.readPairTable(table, elems[i], elems[j]);
                    }
                }
                
                // Compute the attributes
                newAttrs[attrPos++] = StatUtils.max(pairValues);
                newAttrs[attrPos++] = StatUtils.min(pairValues);
                newAttrs[attrPos++] = StatUtils.max(pairValues) - StatUtils.min(pairValues);
                double mean = 0;
                for (int i=0; i<pairValues.length; i++) {
                    mean += pairWeights[i] * pairValues[i];
                }
                newAttrs[attrPos++] = mean;
                double mad = 0;
                for (int i=0; i<pairValues.length; i++) {
                    mad += pairWeights[i] * Math.abs(pairValues[i] - mean);
                }
                newAttrs[attrPos++] = mad;
            }
            
            entry.addAttributes(newAttrs);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Print out number of attributes
        output += " (" + (ElementalProperties.size() * 5) + ") ";
        
        // Print out description
        output += "Properites based on statistics of the properties of pairs "
                + "of elements in a material. Minimum, maximum, range, weighted-mean,"
                + "and weighted mean absolute deviation"
                + " of " + ElementalProperties.size() + " element pair properties:\n";
        
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
