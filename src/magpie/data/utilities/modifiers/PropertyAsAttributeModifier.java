/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.utilities.modifiers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.user.CommandHandler;

/**
 * Use a measured property of an entry as an attribute. Requires that dataset extends
 *  {@linkplain MultiPropertyDataset}. Entries without a measured value for desired
 *  property will be removed from dataset.
 * 
 * <usage><p><b>Usage</b>: &lt;properties...>
 * <br><pr><i>properties...</i>: Names of properties to add as attributes (can be several)</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PropertyAsAttributeModifier extends BaseDatasetModifier {
    /** Properties to add as attributes */
    String[] PropertiesToAdd = null;

    @Override
    public void setOptions(List Options) throws Exception {
        if (Options.isEmpty()) throw new Exception(printUsage());
        setPropertiesToAdd(CommandHandler.convertCommandToString(Options));
    }

    @Override
    public String printUsage() {
        System.err.println("WARNING: This class has not been tested yet. LW 4Apr14");
        return "Usage: <Properties to add...>";
    }
    
    /**
     * Define which properties to use as attributes
     * @param PropertiesToAdd Properties to use as attributes
     */
    public void setPropertiesToAdd(String[] PropertiesToAdd) {
        this.PropertiesToAdd = Arrays.copyOf(PropertiesToAdd, PropertiesToAdd.length);
    }

    @Override
    protected void modifyDataset(Dataset Data) {
        if (! (Data instanceof MultiPropertyDataset))
            throw new Error("Data does not extend MultiPropertyDataset");
        MultiPropertyDataset Ptr = (MultiPropertyDataset) Data;
        
        // For each property, add as attribute 
        for (String Property : PropertiesToAdd) {
            int ind = Ptr.getPropertyIndex(Property);
            if (ind == -1)
                throw new Error("Dataset does not contain property: " + Property);
            
            // Step 1: Add attribute to dataset
            double[] value = Ptr.getMeasuredPropertyArray(ind);
            Ptr.addAttribute(Property, value);
            
            // Step 2: Remove entries without measured property (value == NaN)
            ind = Ptr.NAttributes() - 1; // Knowing it is the last one
            Iterator<BaseEntry> iter = Data.getEntries().iterator();
            while (iter.hasNext()) {
                BaseEntry E = iter.next();
                if (Double.isNaN(E.getAttribute(ind)))
                    iter.remove();
            }
        }
    }
    
    
    
    
    
}
