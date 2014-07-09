/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.*;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class is designed to store all information related to an entry in a Dataset
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class BaseEntry implements java.lang.Cloneable, java.io.Serializable,
        java.util.Comparator, java.lang.Comparable {
    /** Values of attributes */
    private List<Double> AttributeList;
    /** Measured value of class variable */
    private double Class;
    /** Probably of entry existing in each possible class (for classification) */
    private double[] Probability;
    /** Class variable predicted by a model */
    private double PredictedClass;
    /** Whether this entry has a measured class variable */
    private boolean measured=false;
    /** Whether this entry has a predicted class variable */
    private boolean predicted=false; 
    
    /** Create a blank entry */
    public BaseEntry() {
        this.AttributeList = new ArrayList<>();
        this.Probability = null;
    }
	
	/**
	 * Generate an entry by parsing a text string
	 * @param input String representing entry 
	 */
	public BaseEntry(String input) {
		// Find anything in the input that matches a number
		Matcher numMatcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?").matcher(input);
		AttributeList = new LinkedList<>();
		while (numMatcher.find()) {
			String number = numMatcher.group();
			AttributeList.add(Double.valueOf(number));
		}
		this.Probability = null;
	}

    /**
     * Get number of attributes currently set
     * @return Number of attributes
     */
    public int NAttributes() {
        return AttributeList.size();
    }
    
    /** 
     * Retrieve attributes for this entry
     * @return List of attributes (same order as {@linkplain Dataset#AttributeName})
     */
    public double[] getAttributes() {
        return ArrayUtils.toPrimitive(AttributeList.toArray(new Double[0]));
    }
    
    /**
     * Retrieve a certain attribute for this entry
     * @param index Index of attribute to retrieve
     * @return Value of specified attribute
     */
    public double getAttribute(int index) {
        return AttributeList.get(index);
    }

    /**
     * Sets attributes for this entry
     * @param attributes List of attributes (same order as {@linkplain Dataset#AttributeName})
     */
    public void setAttributes(double[] attributes) {
        this.AttributeList.clear();
        addAttributes(attributes);
    }
    
    /** 
     * Adds attribute value to the end of the list of current attributes
     * @param attribute Value of attribute to add
     */
    public void addAttribute(double attribute) {
        AttributeList.add(attribute);
    }
    
    /**
     * Adds several attributes to the end of the attribute list
     * @param attributes List of attribute values to be added
     */
    public void addAttributes(double[] attributes) {
        Double[] toAdd = new Double[attributes.length];
        for (int i=0; i<attributes.length; i++)
            toAdd[i] = attributes[i];
        AttributeList.addAll(Arrays.asList(toAdd));
    }
    
    /** 
     * Generates a clone of an entry. It only copies the pointer to the attribute
     * data, so the feature list is not duplicated. 
     * <p>Reasons for doing this:
     * <ol>
     * <li>Save on memory. This array is the primary memory hog of this system
     * <li>If you can the number of attributes, the pointer will change and you will not effect the original
     * </ol>
     * 
     * Note: If you change the value of 
     * @return Clone
     */
    @Override public BaseEntry clone() {
        BaseEntry copy;
        try { copy = (BaseEntry) super.clone(); }
        catch (CloneNotSupportedException c) { throw new Error(c); }
        copy.AttributeList = new ArrayList<>(AttributeList); 
        copy.Class = this.Class;
        copy.PredictedClass = this.PredictedClass;
        copy.measured = this.measured;
        copy.predicted = this.predicted;
        return copy;
    }
    
    @Override public int compare(Object A_obj, Object B_obj) {
        if (A_obj instanceof BaseEntry && B_obj instanceof BaseEntry) {
            BaseEntry A = (BaseEntry) A_obj, B = (BaseEntry) B_obj;
            // If A has more features, it is greater. 
            if (A.AttributeList.size() != B.AttributeList.size())
                return (A.AttributeList.size() > B.AttributeList.size()) ? 1 : -1;
            // Check which has greater features
            for (int i=0; i<A.AttributeList.size(); i++)
                if (A.getAttribute(i) != B.getAttribute(i))
                    return (A.getAttribute(i) > B.getAttribute(i)) ? 1 : -1;
            // We have concluded they are equal
            return 0;
        } else return 0;
    }
    
    @Override public int compareTo(Object B) { return compare(this, B); }
    
    @Override public int hashCode() {
        if (AttributeList.size() > 0)  return (int) AttributeList.hashCode();
        else return 1;
    }
    
    @Override public boolean equals(java.lang.Object other) {
        // Check if any of the 
        if (other instanceof BaseEntry) {
            BaseEntry obj = (BaseEntry) other;
            return AttributeList.equals(obj.AttributeList);
        } else return false;
    }
    
    /** @return Whether a measured class has been set for this entry */
    public boolean hasMeasurement() { return measured; }
    /** @return Whether a predicted class has been set for this entry */
    public boolean hasPrediction() { return predicted; }
    
    /** 
     * Set number of attributes that describe this entry
     * @param number Number of attributes
     */
    public void setAttributeCount(int number){
        if (AttributeList instanceof ArrayList) {
            ArrayList Ptr = (ArrayList) AttributeList;
            Ptr.ensureCapacity(number);
        }
    }

    /** 
     * Set the measured class variable
     * @param x Measured class
     */
    public void setMeasuredClass(double x){
        this.Class = x; measured=true;
    }
    /** 
     * Get the measured class variable
     * @return Measured class
     */
    public double getMeasuredClass() { return Class; }

    /** Set the predicted class variable
     * @param x Predicted class
     */
    public void setPredictedClass(double x) {
        PredictedClass = x; predicted=true;
        Probability=null;
    }
    
    /**
     * Get the predicted class variable
     * @return Predicted class
     */
    public double getPredictedClass() { return PredictedClass; }
    
    /** 
     * Set the predicted probability of a entry existing in each class
     * @param probabilites Probability of entry being in each class
     */
    public void setClassProbabilities(double[] probabilites) {
        Probability = probabilites.clone(); predicted=true;
        PredictedClass = 0;
        for (int i=1; i<Probability.length; i++)
            if (Probability[i]>Probability[(int)PredictedClass])
                PredictedClass=i;
    }
    
    /** 
     * Get the probability of an entry existing in each class. Returns null if 
     *  no class probabilities have been stored
     * @return Class probabilities
     */
    public double[] getClassProbilities() { 
        return Probability; 
    }
    
    @Override public String toString() {
        if (NAttributes() > 0) {
            String output = String.format("(%.3f", AttributeList.get(0));
            for(int i=1; i<NAttributes(); i++)
                output += String.format(",%.3f", AttributeList.get(i));
            output+=")";
            return output;
        } else 
            return "Nameless";
    }
    
    /**
     * Call this after generating attributes to ensure the array storing attributes
     *  is as small as possible.
     */
    public void reduceMemoryFootprint() {
        if (AttributeList instanceof ArrayList) {
            ArrayList Ptr = (ArrayList) AttributeList;
            Ptr.trimToSize();
        }
            
    }
}
