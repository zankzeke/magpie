/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.utilities.filters;

import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;

/**
 * Filter entries based on a measured property. Requires the data to extend 
 *  {@linkplain MultiPropertyDataset}. 
 * 
 * <usage><p><b>Usage</b>: &lt;Target Property> &lt;Criteria> &lt;Threshold>
 * <br><pr><i>Target Property</i>: Property on which data is filtered
 * <br><pr><i>Criteria</i>: Comparison operator used to filter data. Can be: &lt;, &le;, >, &ge;, =, and &ne;
 * <br><pr><i>Threshold</i>: Value to which property is compared</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PropertyFilter extends AttributeFilter {
    
    @Override
    public String printUsage() {
        return "Usage: <Target Property> <Criteria> <Threshold>";
    }

    @Override
    protected boolean[] label(Dataset D) {
        if (! (D instanceof MultiPropertyDataset)) {
            throw new Error("Data does not extend MultiPropertyDataset");
        }
        MultiPropertyDataset Ptr = (MultiPropertyDataset) D;
       double[] property = Ptr.getMeasuredPropertyArray(TargetAttribute);
       return testCriteria(property);
    }
}
