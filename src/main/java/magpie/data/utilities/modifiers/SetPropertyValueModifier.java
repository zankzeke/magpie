package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Set the value of a certain property of each entry. Sets both the measured and
 * predicted value for that property.
 * 
 * <usage><p><b>Usage</b>: &lt;name&gt; &lt;value&gt;
 * <br><pr><i>name</i>: Name of property to be set
 * <br><pr><i>value</i>: Value to be set for all entries</usage>
 * 
 * @author Logan Ward
 */
public class SetPropertyValueModifier extends BaseDatasetModifier {
    /** Name of property to be set */
    protected String PropertyName;
    /** Value to set for property. Can be numeric or name of a class, will be 
     * stored as a string either way */
    protected String PropertyValue;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            String name = Options.get(0).toString();
            String value = Options.get(1).toString();
            
            setPropertyValue(name, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <name> <value>";
    }
    
    /**
     * Set the name and value of property to be set
     * @param name Name of property
     * @param value Value of property. Can be a number, or the name of a class
     */
    public void setPropertyValue(String name, String value) {
        PropertyName = name;
        PropertyValue = value;
    }
    
    /**
     * Set the name and value of property to be set
     * @param name Name of property
     * @param value Value of property
     */
    public void setPropertyValue(String name, double value) {
        setPropertyValue(name, Double.toString(value));
    }

    @Override
    protected void modifyDataset(Dataset dataPtr) {
        if (! (dataPtr instanceof MultiPropertyDataset)) {
            throw new IllegalArgumentException("Data must be a MultiPropertyDataset");
        }
        
        // Get the ID of the property to be changed
        MultiPropertyDataset data = (MultiPropertyDataset) dataPtr;
        int propertyID = data.getPropertyIndex(PropertyName);
        if (propertyID == -1) {
            throw new RuntimeException("No such property: " + PropertyName);
        }
        
        // Turn the property value into something numeric
        double newValue;
        double[] newProbs = null;
        boolean isDiscrete = data.getPropertyClassCount(propertyID) > 1;
        if (isDiscrete) {
            // Get the class value
            String[] propertyNames = data.getPropertyClasses(propertyID);
            newValue = ArrayUtils.indexOf(propertyNames, PropertyValue);
            if (((int) newValue) == -1) {
                throw new RuntimeException("Invalid option for property: " + PropertyValue);
            }
            
            // Make the probabilities
            newProbs = new double[data.getPropertyClassCount(propertyID)];
            newProbs[(int) newValue] = 1.0;
        } else {
            newValue = Double.parseDouble(PropertyValue);
        }
        
        // Set the value for each entry
        for (BaseEntry entryPtr : data.getEntries()) {
            MultiPropertyEntry entry = (MultiPropertyEntry) entryPtr;
            
            // Set the measured value
            entry.setMeasuredProperty(propertyID, newValue);
            
            // Set the predicted value
            if (isDiscrete) {
                entry.setPredictedProperty(propertyID, newProbs);
            } else {
                entry.setPredictedProperty(propertyID, newValue);
            }
        }
    }
}
