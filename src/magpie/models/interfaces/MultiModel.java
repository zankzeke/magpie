/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.interfaces;

import magpie.models.BaseModel;
import magpie.models.utility.MultiModelUtility;

/**
 * Interface for models that contain submodels.
 * 
 * <p>Implementations should be sure to add a call to operations from {@linkplain MultiModelUtility}, where necessary
 * 
 * @author Logan Ward
 * @version 0.1
 */
public interface MultiModel {

    /** 
     * @return number of model slots currently available 
     */
    public int NModels();

    /** 
     * Set the model template
     * @param x Template model (will be cloned)
     */
    public void setGenericModel(BaseModel x);
    
    /**
     * Get the model template
     * @return Pointer to the generic model
     */
    public BaseModel getGenericModel();

    /** 
     * Set a specific submodel. Will increase number of models if necessary.
     * 
     * <p>Any implementation should not clone the model. This will allow people to construct
     * a model used already-trained models.</p>
     * @param index Index of submodel to be set
     * @param x Model to be used (creates a clone)
     */
    public void setModel(int index, BaseModel x);
    
    /**
     * Get a specific submodel.
     * @param index Model to retrieve
     * @return Requested model or null if index >= NModels
     */
    public BaseModel getModel(int index);

    /** 
     * Defines the number of models to be trained. If a generic model is set, it will
     * automatically use it to defined any new models
     * @param n Number of models to use
     */
    void setNumberOfModels(int n);    
}
