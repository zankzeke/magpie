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
import magpie.data.utilities.splitters.BaseDatasetSplitter;
import magpie.user.CommandHandler;

/**
 * Changes the class variable of an entry to be what partition a {@linkplain BaseDatasetSplitter}
 *  assigns it to.
 * 
 * <usage><p><b>Usage</b>: &lt;split method> [&lt;split options...>]
 * <br><pr><i>split method</i>: Name of {@linkplain BaseDatasetSplitter} used to partition data.
 * ("?" for available options).
 * <br><pr><i>split options...</i>: Any options for the splitter</usage>
 * @author Logan Ward
 */
public class PartitionToClassModifier extends BaseDatasetModifier {
    /** Splitter used to partition data */
    private BaseDatasetSplitter splitter;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String splitMethod;
        List<Object> splitOptions;
        if (Options.isEmpty()) 
            throw new Exception(printUsage());
        splitMethod = Options.get(0).toString();
        if (splitMethod.contains("?")) {
            System.out.println(CommandHandler.printImplmentingClasses(BaseDatasetSplitter.class, false));
            return;
        }
        splitOptions = Options.subList(1, Options.size());
        setSplitter((BaseDatasetSplitter) CommandHandler.instantiateClass(
                "data.utilities.splitters." + splitMethod, splitOptions));
    }

    @Override
    public String printUsage() {
        return "Usage: <split method> [<split options...>]";
    }
    
    /**
     * Set the splitter used to generate new class variables. Must already be trained
     * @param splitter Desired splitter
     */
    public void setSplitter(BaseDatasetSplitter splitter) {
        this.splitter = splitter;
    }

    @Override
    protected void modifyDataset(Dataset Data) {
        // Generate labels for all entries
        int[] newClass = splitter.label(Data);
        // Assign entries to new class
        for (int i=0; i<Data.NEntries(); i++) {
            BaseEntry Entry = Data.getEntry(i);
            double value = (double) newClass[i];
            if (Entry instanceof MultiPropertyEntry) {
                // Add class as a new property
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
                Ptr.addProperty("Split-" + Ptr.getTargetPropertyName());
                Ptr.setTargetProperty("Split-" + Ptr.getTargetPropertyName());
            }
        }
        
        // Define new class names
        int nClasses = -1;
        for (int cls : newClass) if (cls > nClasses) nClasses = cls;
        nClasses++;
        String[] ClassNames = new String[nClasses];
        for (int i=0; i<ClassNames.length; i++) 
            ClassNames[i] = "Split" + i;
        Data.setClassNames(ClassNames);
    }
}
