package magpie.data.utilities.modifiers.duplicates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Resolve entries by averaging the duplicates. For continuous properties or classes,
 * the average of all entries is taken. For discrete properties, the class with the 
 * lowest index out of all classes with most examples is selected.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class AveragingDuplicateResolver extends BaseDuplicateResolver {

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
    protected BaseEntry resolveDuplicates(Dataset data, List<BaseEntry> entries) {
        if (entries.get(0) instanceof MultiPropertyEntry) {
            return resolveMultipropertyEntry(data, entries);
        } else {
            return resolveBaseEntry(data, entries);
        }
    }
    
    /**
     * Average base entries.
     * @param data Dataset containing these entries
     * @param entries List of classes that are BaseEntry objects
     * @return A single selection
     */
    static protected BaseEntry resolveBaseEntry(Dataset data, List<BaseEntry> entries) {
        BaseEntry output = entries.get(0).clone();
        double newClass = getNewClass(data, entries);
        if (! Double.isNaN(newClass)) {
            output.setMeasuredClass(newClass);
        }
        return output;
    }
    
    /**
     * Average each property of a {@linkplain MultiPropertyEntry}
     * @param data Dataset containing these entries
     * @param entries List of classes that are BaseEntry objects
     * @return A single selection
     */
    static protected BaseEntry resolveMultipropertyEntry(Dataset data, List<BaseEntry> entries) {
        MultiPropertyEntry output = (MultiPropertyEntry) entries.get(0).clone();
        MultiPropertyDataset dataPtr = (MultiPropertyDataset) data;
        
        // Store original target property
        int originalTarget = output.getTargetProperty();
        
        // Handled the default class
        output.setTargetProperty(-1);
        dataPtr.setTargetProperty(-1, true);
        for (BaseEntry entry : entries) { ((MultiPropertyEntry) entry).setTargetProperty(-1); }
        double newClass = getNewClass(data, entries);
        if (! Double.isNaN(newClass)) {
            output.setMeasuredClass(newClass);
        }
        
        // Loop through each property
        for (int p=0; p<dataPtr.NProperties(); p++) {
            output.setTargetProperty(p);
            dataPtr.setTargetProperty(p, true);
            for (BaseEntry entry : entries) {
                ((MultiPropertyEntry) entry).setTargetProperty(p);
            }
            newClass = getNewClass(data, entries);
            if (!Double.isNaN(newClass)) {
                output.setMeasuredClass(newClass);
            }
        }
        
        // Reset target property
        output.setTargetProperty(originalTarget);
        dataPtr.setTargetProperty(originalTarget, true);
        for (BaseEntry entry : entries) { 
            ((MultiPropertyEntry) entry).setTargetProperty(originalTarget);
        }
        
        return output;
    }
    
    /**
     * Get the average of the measured class of an entry. For continuous classes,
     * computes average of all entries. For discrete, takes the lowest-index class
     * of all that occur the most frequently (ex: if class #0 and #1 both appear the
     * most, 0 is selected)
     * @param data Dataset being resolved
     * @param entries Duplicate entries
     * @return New class value, or NaN if no duplicates have a measured value
     */
    static protected double getNewClass(Dataset data, List<BaseEntry> entries) {
        if (data.NClasses() > 1) {
            // Store the number of times each class occurs
            double[] nHits = new double[data.NClasses()];
            for (BaseEntry entry : entries) {
                if (entry.hasMeasurement()) {
                    nHits[(int) entry.getMeasuredClass()]++;
                }
            }
            
            // Find the largest number of occurances
            double maxHits = StatUtils.max(nHits);
            if (maxHits < 1) {
                return Double.NaN;
            }
            
            // Get all entries that occur the most frequently
            List<Integer> best = new ArrayList<>(data.NClasses());
            for (int c=0; c<nHits.length; c++) {
                if (nHits[c] == maxHits) {
                    best.add(c);
                }
            }
            
            // Sort them
            Collections.sort(best);
            return best.get(0);
        } else {
            // Average measured class
            DescriptiveStatistics stats = new DescriptiveStatistics();
            for (BaseEntry entry : entries) {
                if (entry.hasMeasurement()) {
                    stats.addValue(entry.getMeasuredClass());
                }
            }
            
            // Return the average
            if (stats.getN() > 0) {
                return stats.getMean();
            } else {
                return Double.NaN;
            }
        }
    }
    
}
