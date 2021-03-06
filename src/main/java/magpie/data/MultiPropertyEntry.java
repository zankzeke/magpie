package magpie.data;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Entry that can have multiple properties. Consider these properties as potential
 * class variables. This class simply allows a user to designate which property to use as 
 * the class variable, and seamlessly stores predictions of these properties internally.
 * 
 * <p>Users of entries that extend this class can designate a property as the class
 * variable using {@link #setTargetProperty(int)}. Often, this is performed through various
 * commands from the {@link MultiPropertyDataset}. You may notice that names of
 * properties are not stored in this class, and they are actually stored in MultiPropertyDataset.
 * This is to reminder users that they should access properties through the Dataset.
 * 
 * <p>If the "target" property is set to "-1", this class functions as a normal BaseEntry.
 * 
 * @author Logan Ward
 * @version 0.1
 * @see MultiPropertyDataset
 */
public class MultiPropertyEntry extends BaseEntry {
    /** List of values of each measured property at this composition. Value is equal
     * to Double.NaN if no measured property has been set.
     */
    private double[] MeasuredProperty = new double[0];
    /** List of predictions of each property at this composition. Stores them in a 2-D array
     * so that this same storage device can also hold class probabilities. Value is equal to
     * <code>null</code> if this property has not been predicted.
     */
    private double[][] PredictedProperty = new double[0][];
    /** Index of property being used as class variable. Equal to -1 if no property is being used. */
    private int TargetProperty = -1;

    public MultiPropertyEntry() {
    }

    /**
     * Create a new entry, given attributes
     *
     * @param attributes Attribute values
     */
    public MultiPropertyEntry(double[] attributes) {
        setAttributes(attributes.clone());
    }

    @Override
    public MultiPropertyEntry clone() {
        MultiPropertyEntry x = (MultiPropertyEntry) super.clone(); 
        if (MeasuredProperty != null)
            x.MeasuredProperty = MeasuredProperty.clone();
        if (PredictedProperty != null) {
            x.PredictedProperty = PredictedProperty.clone();
            for (int i=0; i<PredictedProperty.length; i++)
                if (PredictedProperty[i] != null)
                    x.PredictedProperty[i] = PredictedProperty[i].clone();
        }
        return x;
    }
    
    /**
     * Remove all information about properties
     */
    public void clearPropertyData() {
        MeasuredProperty = new double[0];
        PredictedProperty = new double[0][];
    }
	
	/**
	 * Define the number of properties this entry can support.
	 * 
	 * @param N Number of properties this entry should be able to store
	 */
	public void setNProperties(int N) {
		if (N < NProperties()) {
			MeasuredProperty = Arrays.copyOf(MeasuredProperty, N);
			PredictedProperty = Arrays.copyOf(PredictedProperty, N);
		} else if (N > NProperties()) {
			int oldLength = NProperties();
			double[] newMeas = new double[N - oldLength];
			Arrays.fill(newMeas, Double.NaN);
			MeasuredProperty = ArrayUtils.addAll(MeasuredProperty, newMeas);
			double[][] newPred = new double[N - oldLength][];
			Arrays.fill(newPred, null);
			PredictedProperty = ArrayUtils.addAll(PredictedProperty, newPred);
		}
	}
    
    /** 
     * Add a new property to this entry. 
     * @see MultiPropertyDataset#addProperty(java.lang.String, java.lang.String[])
     */
    public void addProperty() {
        addProperty(Double.NaN, Double.NaN);
    }
    
    /**
     * Add a new property and define measured property. 
     * 
     * @param measuredValue Measured value of new property
     * @see MultiPropertyDataset#addProperty(java.lang.String, java.lang.String[])
     */
    public void addProperty(double measuredValue) {
        addProperty(measuredValue, Double.NaN);
    }
    
    /**
     * Add a new property to this entry, set measured and predicted values
     * @param measuredValue Measured value of this property. Set to Double.NaN if unknown.
     * @param predictedValue Predicted value of this property. Set to Double.NaN if unknown.
     * @see MultiPropertyDataset#addProperty(java.lang.String, java.lang.String[]);
     */
    public void addProperty(double measuredValue, double predictedValue) {
        // Add in new measured property
        MeasuredProperty = ArrayUtils.add(MeasuredProperty, measuredValue);
        
        // Add in new predicted property
        if (Double.isNaN(predictedValue))
            PredictedProperty = ArrayUtils.add(PredictedProperty, null);
        else 
            PredictedProperty = ArrayUtils.add(PredictedProperty, new double[]{predictedValue});
    }
    
    

    @Override
    public double getMeasuredClass() {
        if (usingPropertyAsClass()) {
            return getMeasuredProperty(TargetProperty); 
        } else {
            return super.getMeasuredClass();
        }
    }

    @Override
    public void setMeasuredClass(double x) {
        if (usingPropertyAsClass()) {
            setMeasuredProperty(TargetProperty, x);
        } else {
            super.setMeasuredClass(x);
        }
    }

    /**
     * Get copy all measured properties.
     * @return Clone of internal array of measured properties
     */
    public double[] getMeasuredProperties() {
        return MeasuredProperty.clone();
    }

    /**
     * Define the values of all measured properties of this entry. Simply use Double.NaN
     * for all properties that have not been measured.
     * <p>This command will automatically reset the predicted properties, so use with caution.
     *
     * @param newValues Values of all measured properties
     */
    public void setMeasuredProperties(double[] newValues) {
        MeasuredProperty = newValues.clone();
        PredictedProperty = new double[newValues.length][];
        Arrays.fill(PredictedProperty, null);
    }
    
    /**
     * Get a single measured property. If not measured, will return NaN.
     * @param index Index of measured property to retrieve
     * @return Value of that property
     */
    public double getMeasuredProperty(int index) {
        return MeasuredProperty[index];
    }

    @Override
    public double getPredictedClass() {
        if (usingPropertyAsClass()) {
            return getPredictedProperty(TargetProperty);
        } else {
            return super.getPredictedClass();
        }
    }

    @Override
    public void setPredictedClass(double x) {
        if (usingPropertyAsClass()) {
            setPredictedProperty(TargetProperty, x);
        } else {
            super.setPredictedClass(x);
        }
    }

    @Override
    public double[] getClassProbilities() {
        if (usingPropertyAsClass()) {
            if (PredictedProperty[TargetProperty] == null) {
                return null;
            }
            return PredictedProperty[TargetProperty].length > 1 ?
                    PredictedProperty[TargetProperty] : null;
        } else {
            return super.getClassProbilities();
        }
    }

    /**
     * Get the predicted properties for an instance. If the property has not been predicted,
     *  returns Double.NaN.
     * @param index Index of property to retrieve
     * @return Predicted property
     */
    public double getPredictedProperty(int index) {
        if (PredictedProperty[index] == null) {
            return Double.NaN;
        } else if (PredictedProperty[index].length == 1) {
            return PredictedProperty[index][0];
        } else {
            int max = 0;
            for (int i = 1; i < PredictedProperty[index].length; i++) {
                if (PredictedProperty[index][i] > PredictedProperty[index][max]) {
                    max = i;
                }
            }
            return (double) max;
        }
    }

    /**
     * Get the predicted class probabilities for a certain property.
     * If the property has not been predicted, returns null.
     * @param index Index of property to retrieve
     * @return Predicted class probabilities for that property
     */
    public double[] getPropertyClassProbabilties(int index) {
        if (PredictedProperty[index] == null) {
            return null;
        } else {
            return PredictedProperty[index];
        }
    }

    /**
     * Look up which property is being used as the class variable. Returns -1 if
     * the class variable is something else.
     * @return Target property
     */
    public int getTargetProperty() {
        return TargetProperty;
    }

    /**
     * Define which property to use as the class variable. Set to -1 when using some other variable.
     *
     * @param index Index of property to use
     */
    public void setTargetProperty(int index) {
        if (index >= NProperties())
            throw new IllegalArgumentException("Entry only has " + NProperties() + " defined. Target property cannot be " + index);
        TargetProperty = index;
    }

    /**
     * @return Whether a property is being used as the class variable.
     */
    public boolean usingPropertyAsClass() {
        return TargetProperty >= 0;
    }

    /**
     * Whether a specific property of this entry has been measured
     * @param index Index of property to test
     * @return Whether it has been measured
     */
    public boolean hasMeasuredProperty(int index) {
	if (index >= MeasuredProperty.length) {
		return false;
	}
        return ! Double.isNaN(getMeasuredProperty(index));
    }

    /**
     * Whether an specific property has any predicted value
     * @param index Index of property to test
     * @return Whether that property has been predicted
     */
    public boolean hasPredictedProperty(int index) {
        return ! Double.isNaN(getPredictedProperty(index));
    }
    
    @Override
    public boolean hasMeasurement() {
        if (usingPropertyAsClass()) {
            return ! Double.isNaN(getMeasuredProperty(TargetProperty));
        } else {
            return super.hasMeasurement();
        }
    }
    
    @Override
    public boolean hasPrediction() {
        if (usingPropertyAsClass()) {
            return PredictedProperty[TargetProperty] != null;
        } else {
            return super.hasPrediction();
        }
    }

    @Override
    public boolean hasClassProbabilities() {
        if (usingPropertyAsClass()) {
            return hasPropertyClassProbabilities(TargetProperty);
        } else {
            return super.hasClassProbabilities();
        }
    }

    /**
     * Check whether a property has predicted class probabilities
     * @param index Index of property
     * @return Whether it has class probabilities
     */
    public boolean hasPropertyClassProbabilities(int index) {
        return PredictedProperty[index] != null && PredictedProperty[index].length > 1;
    }

    @Override
    public void deleteMeasuredClass() {
        if (usingPropertyAsClass()) {
            setMeasuredProperty(TargetProperty, Double.NaN);
        } else {
            super.deleteMeasuredClass();
        }
    }

    @Override
    public void deletePredictedClass() {
        if (usingPropertyAsClass()) {
            setPredictedProperty(TargetProperty, null);
        } else {
            super.deletePredictedClass();
        }
    }

    @Override
    public void setClassProbabilities(double[] probabilites) {
        if (usingPropertyAsClass()) {
            PredictedProperty[TargetProperty] = probabilites.clone();
        } else {
            super.setClassProbabilities(probabilites);
        }
    }

    /**
     * Set the measured value of a specific property of this entry. Use Double.NaN if it
     * has not been measured (though I am not sure why you would do this).
     * @param index Index of property to set
     * @param newValue New measured value
     */
    public void setMeasuredProperty(int index, double newValue) {
        MeasuredProperty[index] = newValue;
    }
    
    /**
     * Directly set the predicted value of a property.
     * @param index Index of property
     * @param newValue Its new value.
     */
    public void setPredictedProperty(int index, double newValue) {
        if (PredictedProperty[index] == null) {
            PredictedProperty[index] = new double[]{newValue};
        } else if (PredictedProperty[index].length == 1) {
            PredictedProperty[index][0] = newValue;
        } else {
            PredictedProperty[index] = new double[]{newValue};
        }
    }

    /**
     * Directly set the probabilities of an entry existing in one of several classes.
     * @param index Index of property
     * @param newValues Class probabilities. If <code>null</code>, marks this
     * class as unset
     */
    public void setPredictedProperty(int index, double[] newValues) {
        PredictedProperty[index] = newValues != null ? newValues.clone() : null;
    }

    /**
     * @return Number of properties defined for this entry
     */
    public int NProperties() {
        return MeasuredProperty.length;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject output = super.toJSON(); 
        
        // Save the property data
        JSONArray propertyData = new JSONArray();
        for (int p=0; p<NProperties(); p++) {
            JSONObject propertyVals = new JSONObject();
            propertyVals.put("measured", hasMeasuredProperty(p) ? 
                    getMeasuredProperty(p) : null);
            propertyVals.put("predicted", hasPredictedProperty(p) ?
                    getPredictedProperty(p) : null);
            if (hasPropertyClassProbabilities(p)) {
                propertyVals.put("probabilities", getPropertyClassProbabilties(p));
            }
            propertyData.put(propertyVals);
        }
        output.put("properties", propertyData);
        
        return output;
    }
}
