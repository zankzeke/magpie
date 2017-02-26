
package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;

/**
 * Filter entries based on a measured or predicted property. Requires the data to extend 
 *  {@linkplain MultiPropertyDataset}. 
 * 
 * <usage><p><b>Usage</b>: &lt;measured|predicted&gt; &lt;Target Property> &lt;Criteria> &lt;Threshold>
 * <br><pr><i>measured|predicted</i>: Whether to use the measured or predicted value
 * <br><pr><i>Target Property</i>: Property on which data is filtered
 * <br><pr><i>Criteria</i>: Comparison operator used to filter data. Can be: &lt;, &le;, >, &ge;, =, and &ne;
 * <br><pr><i>Threshold</i>: Value to which property is compared</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PropertyFilter extends AttributeFilter {
    /** Whether to use measured or predicted property values */
    protected boolean UseMeasured = true;
    
    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        try {
            String word = OptionsObj.get(0).toString().toLowerCase();
            if (word.startsWith("me")) {
                UseMeasured = true;
            } else if (word.startsWith("pr")) {
                UseMeasured = false;
            } else {
                throw new Exception();
            }
            super.setOptions(OptionsObj.subList(1, OptionsObj.size())); 
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }
    
    @Override
    public String printUsage() {
        return "Usage: <Measured|Predicted> <Target Property> <Criteria> <Threshold>";
    }

    @Override
    public boolean[] label(Dataset D) {
        if (!(D instanceof MultiPropertyDataset)) {
            throw new Error("Data does not extend MultiPropertyDataset");
        }
        MultiPropertyDataset Ptr = (MultiPropertyDataset) D;
        double[] property = UseMeasured ? 
                Ptr.getMeasuredPropertyArray(TargetAttribute)
                : Ptr.getPredictedPropertyArray(TargetAttribute);
        
        return testCriteria(property);
    }
}
