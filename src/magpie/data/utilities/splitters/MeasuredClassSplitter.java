/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.splitters;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Splits based on measured class (Data must have multiple classes for this to be usable).
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class MeasuredClassSplitter extends BaseDatasetSplitter {

    @Override
    public void setOptions(List Options) throws Exception {
        /* Nothing to do */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    @Override
    public int[] label(Dataset D) {
        int[] output = new int[D.NEntries()];
        if (D.NClasses() == 1)
            Arrays.fill(output, 1);
        else {
            Iterator<BaseEntry> iter = D.getEntries().iterator();
            int i=0;
            while (iter.hasNext()) {
                output[i] = (int) iter.next().getMeasuredClass();
                i++;
            }
        }
        return output;
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Do not need to train anything
    }
}
