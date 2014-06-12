/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.rankers;

import java.util.List;
import magpie.data.BaseEntry;

/**
 * EntryRanker that ranks entries based on class variable.
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class SimpleEntryRanker extends EntryRanker {

    @Override
    public void setOptions(List Options) throws Exception {
        /** No options to set */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    
    
    @Override 
    public double objectiveFunction(BaseEntry Entry) {
            if (UseMeasured) return Entry.getMeasuredClass();
            else return Entry.getPredictedClass();
    }
}
