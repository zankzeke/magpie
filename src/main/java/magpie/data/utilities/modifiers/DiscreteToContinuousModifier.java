
package magpie.data.utilities.modifiers;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

/**
 * Convert multiple, discrete class values to single continuous class. Converts class
 *  values to the probability of an entry being a user-specified class.
 *
 * <p>If dataset is a {@linkplain MultiPropertyDataset} and you are using a property as the class variable,
 * this modifier will store result as a new property.</p>
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
            throw new RuntimeException("Class name not found: " + ClassName);
        }

        // Run according to the types
        if (data instanceof MultiPropertyDataset) {
            if (((MultiPropertyDataset) data).usingPropertyAsClass()) {
                applyMultiPropertyDataset((MultiPropertyDataset) data, index);
            } else {
                applyDataset(data, index);
            }
        } else {
            applyDataset(data, index);
        }

    }

    /**
     * Run modification on {@linkplain MultiPropertyDataset}s
     *
     * @param data  Dataset to be transformed
     * @param index Index of class being assessed
     */
    protected void applyMultiPropertyDataset(MultiPropertyDataset data, int index) {
        // Generate the new property name
        String name = String.format("P(%s=%s)", data.getTargetPropertyName(),
                data.getClassName(index));

        // Check whether this property already exists
        int propID = data.getPropertyIndex(name);
        boolean toAdd = propID == -1;

        // If needed, add it
        if (toAdd) {
            data.addProperty(name);
        } else {
            data.setPropertyClasses(propID, new String[]{name}); // Make sure it has the correct names
        }

        // Go through the entries
        for (BaseEntry entryPtr : data.getEntries()) {
            MultiPropertyEntry entry = (MultiPropertyEntry) entryPtr;

            // Get the new values
            double measured = getMeasured(entry, index);
            double predicted = getPredicted(entry, index);

            // Set/add new values
            if (toAdd) {
                entry.addProperty(measured, predicted);
            } else {
                if (entry.hasMeasurement()) {
                    entry.setMeasuredProperty(propID, measured);
                }
                if (entry.hasPrediction()) {
                    entry.setPredictedProperty(propID, predicted);
                }
            }
        }

        // Set target property as the output
        data.setTargetProperty(name, true);
    }

    /**
     * Run modification on {@linkplain Dataset}s
     *
     * @param data  Dataset to be transformed
     * @param index Index of class being assessed
     */
    protected void applyDataset(Dataset data, int index) {
        // Change the class names
        data.setClassNames(new String[]{"P(" + ClassName + ")"});

        // Alter entries
        for (BaseEntry entry : data.getEntries()) {
            // Get new values
            double measured = getMeasured(entry, index);
            double predicted = getPredicted(entry, index);

            // Update the entry
            if (entry.hasMeasurement()) {
                entry.setMeasuredClass(measured);
            }
            if (entry.hasPrediction()) {
                entry.setPredictedClass(predicted);
            }
        }
    }

    /**
     * Get the new measured value for an entry. 1 if the measured class equals target class, 0 if not, and NaN if
     * there is no measurement for this class
     *
     * @param index Index of target class
     * @param entry Entry to be assessed
     * @return New class value
     */
    private double getMeasured(BaseEntry entry, int index) {
        if (entry.hasMeasurement()) {
            return entry.getMeasuredClass() == (double) index ? 1 : 0;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Get the new value of this entry. Probability for target class if there is a prediction, NaN otherwise.
     *
     * @param entry Entry to be assessed
     * @param index Index of target class
     * @return New class value
     */
    private double getPredicted(BaseEntry entry, int index) {
        if (entry.hasPrediction()) {
            return entry.getClassProbilities()[index];
        } else {
            return Double.NaN;
        }
    }

}
