
package magpie.data.utilities.modifiers;

import java.util.Iterator;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Remove a possible class from a dataset. Either changes all entries with that are
 *  measured to have undesired class to a different one (ex: semiconductor -&gt;
 *  nonmetal) or removes those entries.
 * 
 * <p>Example: If you have a dataset where each entry is measured to be either a 
 * metal, semiconductor, or insulator and want to only have a "metal" and "nonmetal" 
 * class, you could use this entry to change all entries marked as a semiconductor
 * to be insulator.
 * 
 * <usage><p><b>Usage</b>: &lt;undesired class&gt; [&lt;new class&gt;]
 * <br><pr><i>undesired class</i>: Name of class to be removed
 * <br><pr><i>new class</i>: Measurements corresponding to undesired class will be changed to this</usage>
 * 
 * @author Logan Ward
 */
public class ClassEliminationModifier extends BaseDatasetModifier {
    /** Name of class to be eliminated */
    private String classToEliminate = null;
    /** Optional: Name of new name for those classes */
    private String newClassName = null;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            setClassToEliminate(Options.get(0).toString());
            if (Options.size() > 1) {
                setNewClassName(Options.get(1).toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <undesired class> [<new class>]";
    }

    /**
     * Define the name of the class to eliminate.
     * @param classToEliminate Name of undesired class
     */
    public void setClassToEliminate(String classToEliminate) {
        this.classToEliminate = classToEliminate;
    }
    
    /**
     * Define the name of the new class for entries measured to exist in the undesired 
     * class. Set to "null" to just delete those entries.
     * @param newClassName New class value (null to delete)
     */
    public void setNewClassName(String newClassName) {
        this.newClassName = newClassName;
    }

    @Override
    protected void modifyDataset(Dataset Data) {
        // Get index of class to remove
        int toRemove = ArrayUtils.indexOf(Data.getClassNames(), classToEliminate);
        if (toRemove == -1) {
            throw new RuntimeException("Dataset does not contain class: " + classToEliminate);
        }
        
        // If set, get index of new class
        int newValue = -1;
        if (newClassName != null) {
            newValue = ArrayUtils.indexOf(Data.getClassNames(), newClassName);
            if (newValue == -1) {
                throw new Error("Dataset does not contain class: " + newClassName);
            }
        }
        
        // Modify all entries
        Iterator<BaseEntry> iter = Data.getEntriesWriteAccess().iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            // Only operate on entries with a measured class
            if (! e.hasMeasurement()) {
                continue;
            }
            // Get current class value
            double currentValue = e.getMeasuredClass();
            // See if it should be changed / removed
            if (currentValue == toRemove) {
                if (newClassName == null) {
                    iter.remove();
                    continue;
                } else {
                    currentValue = newValue;
                }
            }
            // If class value is greater than the one being remove, decrement it
            if (currentValue > toRemove) {
                currentValue--;
            }
            // Store it
            e.setMeasuredClass(currentValue);
        }
        
        // Modify dataset
        String[] oldClassNames = Data.getClassNames();
        String[] newClassNames = ArrayUtils.remove(oldClassNames, toRemove);
        Data.setClassNames(newClassNames);
    }
}
