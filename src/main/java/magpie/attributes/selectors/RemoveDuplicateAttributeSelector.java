package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Select only attributes that have unique values. Finds attributes that have 
 * the same value for each entry, and selects one of those two attributes.
 * 
 * <usage><p><b>Usage</b>: [-tolerance &lt;tol&gt;]
 * <pr><br><i>tol</i>: Tolerance of declaring two variables equivalent. Default = 1e-12</usage>
 * 
 * @author Logan Ward
 */
public class RemoveDuplicateAttributeSelector extends BaseAttributeSelector {
    /** Maximum absolute difference at which to declare two floats equal */
    protected double Tolerance = 1e-12;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // Default: No options
        if (Options.isEmpty()) return;
        
        // Other, tolernace
        if (Options.size() != 2) throw new Exception(printUsage());
        double tol;
        try {
            if (! Options.get(0).toString().equalsIgnoreCase("-tolerance")) {
                throw new Exception();
            }
            tol = Double.parseDouble(Options.get(1).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set the tolerance
        setTolerance(tol);
    }

    @Override
    public String printUsage() {
        return "Usage: [-tolerance <tol>]";
    }

    /**
     * Define the maximum difference at which two numbers are considered equal.
     * @param tol Desired tolerance (Default = 1e-6)
     */
    public void setTolerance(double tol) {
        this.Tolerance = tol;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        return "Eliminates attributes that always have the same values";
    }

    @Override
    protected List<Integer> train_protected(Dataset data) {
        List<Integer> output = new ArrayList<>(data.NAttributes() / 4);
        
        // Loop through all attributes 
        for (int at=0; at<data.NAttributes(); at++) {
            // Check whether this attribute is a duplicate of previously-selected attribute
            boolean duplicateFound = false;
            for (Integer prevAttr : output) {
                boolean pairAreDuplicates = true;
                for (BaseEntry entry : data.getEntries()) {
                    // Get this attribute
                    double x = entry.getAttribute(at);
                    if (Double.isNaN(x)) continue;
                    
                    // Get previous attribute
                    double y = entry.getAttribute(prevAttr);
                    if (Double.isNaN(y)) continue;
                    
                    // Check if they are equal
                    if (Math.abs(x-y) > Tolerance) {
                        pairAreDuplicates = false;
                        break;
                    }
                }
                
                // If they are duplicates, stop the search
                if (pairAreDuplicates) {
                    duplicateFound = true;
                    break;
                }
            }
            
            // If it is not a duplicate, add to output list
            if (! duplicateFound) {
                output.add(at);
            }
        }
        
        return output;
    }
    
}
