package magpie.attributes.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Generate attributes based on stoichiometry of material. Includes attributes
 * that are only based on the fractions of elements, but not what those elements
 * actually are. 
 * 
 * <p>Currently includes:
 * <ol>
 * <li>Number of components
 * <li>L<sub>p</sub> norms of the fraction vector.
 * </ol>
 * 
 * <usage><p><b>Usage</b>: &lt;p norms...&gt;
 * <br><pr><i>p norms</i>: Which p norms to compute (e.g., 2). </usage>
 * @author Logan Ward
 */
public class StoichiometricAttributeGenerator extends BaseAttributeGenerator {
    /** List of p norms to compute */
    protected Set<Integer> PNorms = new TreeSet<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        clearPNorms();
        for (Object obj : Options) {
            int p;
            try {
                p = Integer.parseInt(obj.toString());
            } catch (NumberFormatException e) {
                throw new Exception(printUsage());
            }
            addPNorm(p);
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <p norms...>";
    }
    
    /**
     * Clear out the list of p norms to be computed
     */
    public void clearPNorms() {
        PNorms.clear();
    }
    
    /**
     * Add a p norm to be computed
     * @param norm Desired norm
     * @throws Exception 
     */
    public void addPNorm(int norm) throws Exception {
        if (norm == 0) {
            return; // Already included by default
        } else if (norm == 1) {
            throw new Exception("L1 norm is always 1. Useless as attribute");
        } 
        PNorms.add(norm);
    }
    
    /**
     * Add several norms to be computed
     * @param norms Collection of norms
     * @throws Exception 
     */
    public void addPNorms(Collection<Integer> norms) throws Exception {
        for (Integer n : norms) {
            addPNorm(n);
        }
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Make sure this is a composition dataset
        if (! (data instanceof CompositionDataset)) {
            throw new Exception("Must be a CompositionDataset");
        }
        
        // Add in attribute names
        List<String> newNames = new ArrayList<>(1 + PNorms.size());
        newNames.add("NComp");
        for (int p : PNorms) {
            newNames.add("Comp_L" + p + "Norm");
        }
        data.addAttributes(newNames);
        
        // Compute attributes
        double[] attr = new double[newNames.size()];
        for (BaseEntry ptr : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) ptr;
            double[] fracs = entry.getFractions();
            
            // Number of components
            attr[0] = 0;
            for (double f : fracs) {
                if (f > 0) {
                    attr[0]++;
                }
            }
            
            // Lp norms
            int count = 1;
            for (int p : PNorms) {
                double temp = 0.0;
                for (double f : fracs) {
                    temp += Math.pow(f, p);
                }
                attr[count++] = Math.pow(temp, 1.0 / p);
            }
            
            // Add attributes
            entry.addAttributes(attr);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + ": ";
        
        // Print description
        output += "(" + (PNorms.size() + 1) + ")"
                + " Number of components, p = {";
        boolean started = false;
        for (int p : PNorms) {
            if (started) {
                output += ",";
            }
            output += p;
            started = true;
        }
        output += "} norms of the fraction vector";
        
        return output;
    }
    
}
