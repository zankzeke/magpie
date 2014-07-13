/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;

/**
 * Takes a Dataset with a single class and a continuous class variable
 * and change it into a dataset with two classes. Any entry with a measured class of 0 is assigned
 * to a new class of 0, all others are placed in class 1.
 *
 * <p>For MultiPropertyDatasets, adds a new property and sets it as the target class.</p>
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 * @see MultiPropertyDataset
 */
public class NonZeroClassModifier extends BaseDatasetModifier {
    
    @Override
    protected void modifyDataset(Dataset Data) {
        // Add property to each entry
        for (BaseEntry Entry : Data.getEntries()) {
            double value = Entry.getMeasuredClass() != 0 ? 1 : 0;
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
                Ptr.addProperty("NonZero" + Ptr.getTargetPropertyName());
                Ptr.setTargetProperty("NonZero" + Ptr.getTargetPropertyName(), true);
            }
        }
        
        // Define new class names
        String[] ClassNames = new String[]{"Zero", "NonZero"};
        Data.setClassNames(ClassNames);
    }

    @Override
    public void setOptions(List Options) throws Exception {
        /* Nothing to do, ignore all input */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    
}
