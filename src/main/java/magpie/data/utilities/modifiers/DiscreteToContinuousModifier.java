
package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Convert multiple, discrete class values to single continuous class. Converts class
 *  values to the probability of an entry being a user-specified class.
 * 
 * <p>If dataset is a {@linkplain MultiPropertyDataset}, this will store result 
 * as a new property.
 * 
 * <usage><b>Usage</b>: &lt;class name&gt;
 * <pr><br><i>class name</i>: New value will be probability of entry being this class</usage>
 * 
 * @author Logan Ward
 */
public class DiscreteToContinuousModifier extends BaseDatasetModifier {
	/** Name of class for which to compute probability. */
	protected String ClassName;
	
	@Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
			setClassName(Options.get(0).toString());
            if (Options.size() != 1) {
                throw new Exception();
            }
		} catch (Exception e) {
			throw new Exception(printUsage());
		}
    }

    @Override
    public String printUsage() {
        return "Usage: <class name>";
    }

	/**
	 * Set name of class for which to compute probability.
	 * @param className 
	 */
	public void setClassName(String className) {
		this.ClassName = className;
	}
	
	
	
	@Override
    protected void modifyDataset(Dataset data) {
		// Get index of target class
		int index = ArrayUtils.indexOf(data.getClassNames(), ClassName);
		if (index == -1) {
			throw new Error("Class name not found: " + ClassName);
		}
        /// Add property to each entry
        for (BaseEntry entry : data.getEntries()) {
			// Get current values
            double measured, predicted;
			if (entry.hasMeasurement()) {
				measured = entry.getMeasuredClass() == (double) index ? 1 : 0;
			} else {
				measured = Double.NaN;
			}
			if (entry.hasPrediction()) {
				predicted = entry.getClassProbilities()[index];
			} else {
				predicted = Double.NaN;
			}
            
			// Store them
            if (entry instanceof MultiPropertyEntry) {
                // Add property
                MultiPropertyEntry Ptr = (MultiPropertyEntry) entry;
                if (Ptr.getTargetProperty() != -1)
                    Ptr.addProperty(measured, predicted);
				else {
                    if (entry.hasMeasurement()) entry.setMeasuredClass(measured);
					if (entry.hasPrediction()) entry.setPredictedClass(predicted);
				}
            } else {
				if (entry.hasMeasurement()) entry.setMeasuredClass(measured);
				if (entry.hasPrediction()) entry.setPredictedClass(predicted);
			}
        }
		
        // Add property to dataset if MultiPropertyDataset
        if (data instanceof MultiPropertyDataset) {
            MultiPropertyDataset Ptr = (MultiPropertyDataset) data;
            String name = String.format("P(%s=%s)", Ptr.getTargetPropertyName(),
                Ptr.getClassName(index));
            if (Ptr.getTargetPropertyIndex() != -1) {
                Ptr.addProperty(name);
                Ptr.setTargetProperty(name, true);
            } else {
                Ptr.setClassNames(new String[]{name});
            }
        } else {
            data.setClassNames(new String[]{"P(" + ClassName + ")"});
        }
    }
}
