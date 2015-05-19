package magpie.attributes.generators;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Attributes suitable for modeling ionic compounds. Based on work by Ann Deml:
 * 
 * <p><center>Deml, <i>et al.</i> (?). Journal. Volume (Year), Page
 * 
 * <p>These attributes are based on the formal charges of 
 * 
 * <p>Currently implemented attributes:
 * <ol>
 * <li>
 * @author Logan Ward
 */
public class IonicCompoundAttributeGenerator extends BaseAttributeGenerator {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addAttributes(Dataset data) {
        if (! (data instanceof CompositionDataset)) {
            throw new Error("Data must implement composition dataset");
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Compute formal charges of each element in a compound.
     * @param data Dataset storing oxidation state information
     * @param entry Composition to be evaluated
     * @return List of formal charges (same order as entry.getElements()). Null
     * if a charge-balanced ionic compound can't be formed.
     */
    static public int[] getFormalCharges(CompositionDataset data, CompositionEntry entry) {
        // Get list of elements and compositions
        int[] elems = entry.getElements();
        double[] fracs = entry.getFractions();
        
        // Trivial case: Elemental compounds
        if (elems.length == 1) {
            return null;
        }
        
        // Get the charge states
        double[][] allKnownStates = data.getOxidationStates();
        double[][] possibleStates = new double[elems.length][];
        int nMultiOx = 0;
        for (int e=0; e<elems.length; e++) {
            possibleStates[e] = allKnownStates[elems[e]];
            if (possibleStates[e].length == 0) {
                return null; // Nobel gass
            } else if (possibleStates[e].length > 1) {
                nMultiOx++;
            }
        }
        
        int[] states = new int[elems.length];
        // Case #1: All elements only have one charge state
        if (nMultiOx == 0) {
            double charge=0;
            // Assign the charge state to each 
            for (int e=0; e<elems.length; e++) {
                states[e] = (int) Math.round(possibleStates[e][0]);
                charge += states[e] * fracs[e];
            }
            
            // Check if it is charge balance
            return Math.abs(charge) < 1e-6 ? states : null;
        } else if (nMultiOx > 2) {
            return null;
        }
        
        // 
        return null;
    }
    
}
