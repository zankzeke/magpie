package magpie.attributes.generators.composition;

import java.util.ArrayList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Generates several attributes first demonstrated by <a href="http://journals.aps.org/prb/abstract/10.1103/PhysRevB.89.094104">
 * Meredig & Agrawal <i>et al.</i></a>. This class is meant to be used in
 * conjunction with {@linkplain ElementFractionAttributeGenerator} and 
 * {@linkplain ValenceShellAttributeGenerator}.
 * 
 * <p>To match the attributes from the Meredig & Agrawal <i>et al.</i> paper, Magpie input file:
 * <div style="margin-left: 25px; font-family:monospace">
 *     data attributes generators clear
 *     <br>data attributes generators add composition.ElementFractionAttributeGenerator
 *     <br>data attributes generators add composition.MeredigAttributeGenerator
 *     <br>data attributes generators add composition.ValenceShellAttributeGenerator
 * </div>
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class MeredigAttributeGenerator extends BaseAttributeGenerator {
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
        List<String> newNames = new ArrayList<>();
        newNames.add("mean_AtomicWeight");
        newNames.add("mean_Column");
        newNames.add("mean_Row");
        newNames.add("maxdiff_AtomicNumber");
        newNames.add("mean_AtomicNumber");
        newNames.add("maxdiff_CovalentRadius");
        newNames.add("mean_CovalentRadius");
        newNames.add("maxdiff_Electronegativity");
        newNames.add("mean_Electronegativity");
        newNames.add("mean_NsValence");
        newNames.add("mean_NpValence");
        newNames.add("mean_NdValence");
        newNames.add("mean_NfValence");
        ptr.addAttributes(newNames);
        
        // Get lookup tables
        double[] mass = ptr.getPropertyLookupTable("AtomicWeight");
        double[] column = ptr.getPropertyLookupTable("Column");
        double[] row = ptr.getPropertyLookupTable("Row");
        double[] number = ptr.getPropertyLookupTable("Number");
        double[] radius = ptr.getPropertyLookupTable("CovalentRadius");
        double[] en = ptr.getPropertyLookupTable("Electronegativity");
        double[] s = ptr.getPropertyLookupTable("NsValence");
        double[] p = ptr.getPropertyLookupTable("NpValence");
        double[] d = ptr.getPropertyLookupTable("NdValence");
        double[] f = ptr.getPropertyLookupTable("NfValence");
        
        // Compute attributes 
        double[] newAttrs = new double[newNames.size()];
        for (BaseEntry ePtr : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) ePtr;
            
            // Compute attributes
            int pos = 0;
            newAttrs[pos++] = entry.getMean(mass);
            newAttrs[pos++] = entry.getMean(column);
            newAttrs[pos++] = entry.getMean(row);
            newAttrs[pos++] = entry.getMaxDifference(number);
            newAttrs[pos++] = entry.getMean(number);
            newAttrs[pos++] = entry.getMaxDifference(radius);
            newAttrs[pos++] = entry.getMean(radius);
            newAttrs[pos++] = entry.getMaxDifference(en);
            newAttrs[pos++] = entry.getMean(en);
            newAttrs[pos++] = entry.getMean(s);
            newAttrs[pos++] = entry.getMean(p);
            newAttrs[pos++] = entry.getMean(d);
            newAttrs[pos++] = entry.getMean(f);
            
            // Add to entry
            entry.addAttributes(newAttrs);
        }
    }
    
    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Print description
        output += "(9) Attributes used in ";
        if (htmlFormat) {
            output += "<a href=\"http://journals.aps.org/prb/abstract/10.1103/PhysRevB.89.094104\">";
        }
        output += "Meredig and Agrawal " + (htmlFormat ? "<i>et al.</i>" : "et al.") + " (2014)";
        output += "\n" + (htmlFormat ? "</br>" : "");
        if (htmlFormat) {
            output += "</a>";
        }
        output += "Mean atomic mass, atomic number, electronegativity, radius,"
                + " row and column on periodic table, and number of s/p/d/f valence"
                + " electrons. Range in atomic number,"
                + " radius, and electronegativity";
        
        return output;
    }
}
