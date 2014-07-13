/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;

/**
 * Modify dataset so that it now contains two classes: class variable was negative, and not. 
 * 
 * <usage><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class ClassIsNegativeModifier extends BaseDatasetModifier {
   @Override
    protected void modifyDataset(Dataset Data) {
        // Add property to each entry
        for (BaseEntry Entry : Data.getEntries()) {
            double value = Entry.getMeasuredClass() < 0 ? 0 : 1;
            if (Entry instanceof MultiPropertyEntry) {
                // Add property
                MultiPropertyEntry Ptr = (MultiPropertyEntry) Entry;
                if (Ptr.getTargetProperty() != -1)
                    Ptr.addProperty(value);
                else
                    Entry.setMeasuredClass(value);
            } else
                Entry.setMeasuredClass(value);
        }
		
        // Add property to dataset if MultiPropertyDataset
        if (Data instanceof MultiPropertyDataset) {
            MultiPropertyDataset Ptr = (MultiPropertyDataset) Data;
            if (Ptr.getTargetPropertyIndex() != -1) {
                Ptr.addProperty("Negative" + Ptr.getTargetPropertyName());
                Ptr.setTargetProperty("Negative" + Ptr.getTargetPropertyName(), true);
            }
        }
        
        // Define new class names
        String[] ClassNames = new String[]{"Negative", "Positive"};
        Data.setClassNames(ClassNames);
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() > 0) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    
}
