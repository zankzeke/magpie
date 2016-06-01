package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Select attributes that have no NaN values.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class RemoveNaNAttributeSelector extends BaseAttributeSelector {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new IllegalArgumentException(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    protected List<Integer> train_protected(Dataset data) {
        List<Integer> output = new ArrayList<>(data.NAttributes());
        
        for (int a=0; a<data.NAttributes(); a++) {
            // See if any entries have a NaN or infinite value
            boolean allGood = true;
            for (BaseEntry entry : data.getEntries()) {
                double x = entry.getAttribute(a);
                if (Double.isInfinite(x) || Double.isNaN(x)) {
                    allGood = false;
                    break;
                }
            }
            
            // If so, add it to the "selected" list
            if (allGood) {
                output.add(a);
            }
        }
        
        return output;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        return "Selects only attributes that have no NaN values";
    }
}
