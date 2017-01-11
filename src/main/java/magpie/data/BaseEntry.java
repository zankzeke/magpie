package magpie.data;

import magpie.utility.UtilityOperations;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is designed to store all information related to an entry in a Dataset
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class BaseEntry implements java.lang.Cloneable, java.io.Serializable,
        java.util.Comparator, java.lang.Comparable {
    /** Values of attributes */
    private double[] AttributeList = new double[0];
    /** Measured value of class variable */
    private double Class;
    /** Probably of entry existing in each possible class (for classification) */
    private double[] ClassProbabilites;
    /** Class variable predicted by a model */
    private double PredictedClass;
    /** Whether this entry has a measured class variable */
    private boolean measured=false;
    /** Whether this entry has a predicted class variable */
    private boolean predicted=false; 
    
    /** Create a blank entry */
    public BaseEntry() {
        this.ClassProbabilites = null;
    }
	
	/**
	 * Generate an entry by parsing a text string
	 * @param input String representing entry 
	 * @throws Exception If parse fails
	 */
	public BaseEntry(String input) throws Exception {
		// Find anything in the input that matches a number
		Matcher numMatcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?").matcher(input);
		List<Double> attributes = new LinkedList<>();
		while (numMatcher.find()) {
			String number = numMatcher.group();
			attributes.add(Double.valueOf(number));
		}
        
        // Transfer it to internal storage
        AttributeList = new double[attributes.size()];
        for (int i=0; i<attributes.size(); i++) {
            AttributeList[i] = attributes.get(i);
        }
        
		this.ClassProbabilites = null;
	}
    
    /**
     * Delete any cached information stored within this entry.
     */
    public void reduceMemoryFootprint() {
        // Nothing to do
    }

    /**
     * Get number of attributes currently set
     * @return Number of attributes
     */
    public int NAttributes() {
        return AttributeList.length;
    }
	
	/**
	 * Clear all currently-set attributes.
	 */
	public void clearAttributes() {
		AttributeList = new double[0];
	}
    
    /** 
     * Retrieve attributes for this entry
     * @return List of attributes (same order as {@linkplain Dataset#AttributeName})
     */
    public double[] getAttributes() {
        return AttributeList.clone();
    }
    
    /**
     * Sets attributes for this entry
     * @param attributes List of attributes (same order as {@linkplain Dataset#AttributeName})
     */
    public void setAttributes(double[] attributes) {
        AttributeList = attributes.clone();
    }

    /**
     * Retrieve a certain attribute for this entry
     * @param index Index of attribute to retrieve
     * @return Value of specified attribute
     */
    public double getAttribute(int index) {
        return AttributeList[index];
    }

    /**
     * Set a certain attribute for this entry
     * @param index Index of attribute to set
     * @param value Desired value of specified attribute
     */
    public void setAttribute(int index, double value) {
        AttributeList[index] = value;
    }
    
    /** 
     * Adds attribute value to the end of the list of current attributes
     * @param attribute Value of attribute to add
     */
    public void addAttribute(double attribute) {
        AttributeList = ArrayUtils.add(AttributeList, attribute);
    }
    
    /**
     * Adds several attributes to the end of the attribute list.
     * 
     * <p>Dev Note: This is faster than adding the individually.
     * 
     * @param attributes List of attribute values to be added
     */
    public void addAttributes(double[] attributes) {
        AttributeList = ArrayUtils.addAll(AttributeList, attributes);
    }
    
    /** 
     * Generates a clone of an entry. It creates a new list to store the attributes.
     * So, the attribute data is preserved, but you can change the list as desired.
     * 
     * @return Clone
     */
    @Override 
    public BaseEntry clone() {
        BaseEntry copy;
        try { copy = (BaseEntry) super.clone(); }
        catch (CloneNotSupportedException c) { throw new Error(c); }
        copy.AttributeList = AttributeList.clone();
        copy.Class = this.Class;
        copy.PredictedClass = this.PredictedClass;
        copy.measured = this.measured;
        copy.predicted = this.predicted;
        return copy;
    }
    
    @Override
    public int compare(Object A_obj, Object B_obj) {
        if (A_obj instanceof BaseEntry && B_obj instanceof BaseEntry) {
            BaseEntry A = (BaseEntry) A_obj, B = (BaseEntry) B_obj;
            // If A has more features, it is greater. 
            if (A.AttributeList.length != B.AttributeList.length)
                return (A.AttributeList.length > B.AttributeList.length) ? 1 : -1;
            // Check which has greater features
            for (int i=0; i<A.AttributeList.length; i++)
                if (A.getAttribute(i) != B.getAttribute(i))
                    return (A.getAttribute(i) > B.getAttribute(i)) ? 1 : -1;
            // We have concluded they are equal
            return 0;
        } else return 0;
    }
    
    @Override
    final public int compareTo(Object B) { return compare(this, B); }
    
    @Override public int hashCode() {
        if (AttributeList.length > 0) {
            return Arrays.hashCode(AttributeList);
        } else {
            return 1;
        }
    }
    
    @Override 
    public boolean equals(java.lang.Object other) {
        // Check if any of the 
        if (other instanceof BaseEntry) {
            BaseEntry obj = (BaseEntry) other;
            return Arrays.equals(obj.AttributeList, AttributeList);
        } else return false;
    }
    
    /** @return Whether a measured class has been set for this entry */
    public boolean hasMeasurement() { return measured; }
    /** @return Whether a predicted class has been set for this entry */
    public boolean hasPrediction() { return predicted; }
    /**
     * Whether this entry has predicted probabilities
     * @return Whether it has probabilities
     */
    public boolean hasClassProbabilities() {
        return ClassProbabilites != null;
    }
    
    /**
     * Delete measured class variable.
     */
    public void deleteMeasuredClass() {
        measured = false;
    }
    
    /**
     * Delete predicted class variable.
     */
    public void deletePredictedClass() {
        predicted = false;
    }

    /**
     * Get the measured class variable
     *
     * @return Measured class, if known. NaN otherwise
     */
    public double getMeasuredClass() {
        return hasMeasurement() ? Class : Double.NaN;
    }

    /**
     * Set the measured class variable
     * @param x Measured class
     */
    public void setMeasuredClass(double x){
        this.Class = x; measured=true;
    }

    /**
     * Get the predicted class variable
     *
     * @return Predicted class, if known. NaN otherwise
     */
    public double getPredictedClass() {
        return hasPrediction() ? PredictedClass : Double.NaN;
    }
    
    /** Set the predicted class variable
     * @param x Predicted class
     */
    public void setPredictedClass(double x) {
        PredictedClass = x; predicted=true;
        ClassProbabilites=null;
    }
    
    /** 
     * Set the predicted probability of a entry existing in each class
     * @param probabilites Probability of entry being in each class
     */
    public void setClassProbabilities(double[] probabilites) {
        ClassProbabilites = probabilites.clone(); predicted=true;
        PredictedClass = 0;
        for (int i=1; i<ClassProbabilites.length; i++)
            if (ClassProbabilites[i]>ClassProbabilites[(int)PredictedClass])
                PredictedClass=i;
    }
    
    /** 
     * Get the probability of an entry existing in each class. Returns null if 
     *  no class probabilities have been stored
     * @return Class probabilities
     */
    public double[] getClassProbilities() { 
        return ClassProbabilites.clone(); 
    }
    
    @Override 
    public String toString() {
        if (NAttributes() > 0) {
            String output = String.format("(%.3f", AttributeList[0]);
            for(int i=1; i<NAttributes(); i++)
                output += String.format(",%.3f", AttributeList[i]);
            output+=")";
            return output;
        } else 
            return "Nameless";
    }
    
    /**
     * Print entry in a HTML-friendly format.
     * @return Entry as a string
     */
    public String toHTMLString() {
        return toString();
    }
    
    /**
     * Print entry as a JSON object
     * @return JSON object
     */
    public JSONObject toJSON() {
        JSONObject output = new JSONObject();
        
        // Set the attribute and class values
        output.put("attributes", AttributeList != null ? UtilityOperations.toJSONArray(AttributeList) : new double[0]);
        
        JSONObject classVals = new JSONObject();
        classVals.put("measured", hasMeasurement() ? getMeasuredClass() : null);
        classVals.put("predicted", hasPrediction() ? getPredictedClass() : null);
        if (hasClassProbabilities()) {
            classVals.put("probabilities", getClassProbilities()); 
        }
        output.put("class", classVals);
        
        return output;
    }
    
}
