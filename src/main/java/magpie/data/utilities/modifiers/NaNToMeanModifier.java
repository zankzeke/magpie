package magpie.data.utilities.modifiers;

import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Replace any attributes that are NaN or infinite with the mean value 
 * for that attribute.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * @author Logan Ward
 */
public class NaNToMeanModifier extends BaseDatasetModifier {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No Options*";
    }

    @Override
    protected void modifyDataset(Dataset Data) {
        List<BaseEntry> hasNaN = new LinkedList<>();
        SummaryStatistics statComputer = new SummaryStatistics();
        for (int at=0; at<Data.NAttributes(); at++) {
            // Find all entries that have an NaN, average those that don't
            hasNaN.clear();
            statComputer.clear();
            for (BaseEntry entry : Data.getEntries()) {
                double x = entry.getAttribute(at);
                if (Double.isInfinite(x) || Double.isNaN(x)) {
                    hasNaN.add(entry);
                } else {
                    statComputer.addValue(x);
                }
            }
            
            // Set the value of the bad entries to the mean value
            for (BaseEntry entry : hasNaN) {
                entry.setAttribute(at, statComputer.getMean());
            }
        }
    }
}
