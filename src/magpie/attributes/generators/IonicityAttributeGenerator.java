package magpie.attributes.generators;

import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.utility.tools.OxidationStateGuesser;

/**
 * Generate attributes based on the "ioncity" of a compound.
 * 
 * <p>Creates attributes based on whether it is possible to form a charge-neutral
 * ionic compound, and two measures based a simple measure of "bond ionicity" (see
 * <a href="http://www.wiley.com/WileyCDA/WileyTitle/productCd-EHEP002505.html">
 * W.D. Callister's text</a>):
 * 
 * <center><math>I(x,y) = 1 - exp(-0.25 * (&Chi;(x) - &Chi;(y))<sup>2</sup>)</math></center>
 * 
 * <ol>
 * <li>Maximum ionic character: Max I(x,y) between any two constituents
 * <li>Mean ionic character: sum(x<sub>i</sub> * x<sub>j</sub> * I(i,j)) where
 * x<sub>i</sub> is the fraction of element i.
 * </ol>
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class IonicityAttributeGenerator extends BaseAttributeGenerator {

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
        
        // Get lookup table
		double[] en;
        en = ptr.getPropertyLookupTable("Electronegativity");
        
        // Add attribute names
        List<String> newNames = new LinkedList<>();
        newNames.add("CanFormIonic");
        newNames.add("MaxIonicChar");
        newNames.add("MeanIonicChar");
        ptr.addAttributes(newNames);
        
        // Make the oxidation state guesser
        OxidationStateGuesser guesser = new OxidationStateGuesser();
        guesser.setElectronegativity(en);
        guesser.setOxidationStates(ptr.getOxidationStates());
        
        // Compute new attributes
        double[] toAdd = new double[newNames.size()];
        for (int i = 0; i < ptr.NEntries(); i++) {
            // Get the entry
            CompositionEntry entry = ptr.getEntry(i);
            
            // Can it form an ionic compound?
            toAdd[0] = guesser.getPossibleStates(entry).isEmpty() ? 0 : 1;
            
            // Get maximum ionic character
            toAdd[1] = 1 - Math.exp(-0.25 * Math.pow(entry.getMaxDifference(en), 2.0));
            
            // Get mean ionic character
            int[] elem = entry.getElements();
            double[] frac = entry.getFractions();
            toAdd[2] = 0.0;
            for (int j = 0; j < elem.length; j++) {
                for (int k = 0; k < elem.length; k++) {
                    toAdd[2] += frac[j] * frac[k] * (1 - Math.exp(-0.25
                            * Math.pow(en[elem[j]] - en[elem[k]], 2.0)));
                }
            }
            
            // Add in the attributes
            entry.addAttributes(toAdd);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + ":";
        
        // Print out description
        output += " (3) Whether a charge-neutral, ionic compound can be formed, "
                + "maximum bond ionicity, and mean bond ionicity";
        
        return output;
    }
    
}
