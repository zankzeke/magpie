/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.models;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Employs multiple models, each designed to predict a different property that composes
 *  the class variable. 
 * @author Logan Ward
 */
public class MultiPropertyModel implements Serializable, Cloneable {
    /** Map of property name to model used to predict it */
    protected Map<String,BaseModel> Models = new TreeMap<>();

    @Override
    protected MultiPropertyModel clone() throws CloneNotSupportedException {
        MultiPropertyModel x = (MultiPropertyModel) super.clone(); 
        x.Models = new TreeMap<>(Models);
        return x;
    }
    
    /**
     * Get the model designed to predict a certain property. 
     * @param propertyName Name of property to be modeled
     * @return Clone of that model, or <code>null</code> if no model exists 
     *  for that property
     */
    public BaseModel getModel(String propertyName) {
        if (modelIsDefined(propertyName)) {
            return Models.get(propertyName).clone();
        } else {
            return null;
        }
    }
    
    /**
     * Define a model for a certain property. Will overwrite an existing model
     *  for that property.
     * @param propertyName Name of property
     * @param model Example of model to be used
     */
    public void setModel(String propertyName, BaseModel model) {
        Models.put(propertyName, model);
    }
    
    /**
     * Determine whether a model for a certain property is defined 
     * @param propertyName Name of property
     * @return Whether a model is defined
     */
    public boolean modelIsDefined(String propertyName) {
        return Models.containsKey(propertyName);
    }
    
    /**
     * Whether the model for a certain property is trained.
     * @param propertyName Name of property
     * @return Whether Model is defined and trained
     */
    public boolean modelIsTrained(String propertyName) {
        if (modelIsDefined(propertyName)) {
            return Models.get(propertyName).isTrained();
        } else {
            return false;
        }
    }
    
    /**
     * Whether the model for a certain property is validated
     * @param propertyName Name of property
     * @return Whether Model is defined and validated
     */
    public boolean modelIsValidated(String propertyName) {
        if (modelIsDefined(propertyName)) {
            return Models.get(propertyName).isValidated();
        } else {
            return false;
        }
    }
}
