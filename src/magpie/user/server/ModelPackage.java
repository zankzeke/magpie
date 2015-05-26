package magpie.user.server;

import magpie.data.Dataset;
import magpie.models.BaseModel;

/**
 * Holds information about a model.
 * @author Logan Ward
 */
public class ModelPackage {
    /** Dataset used to generate attributes */
    final public Dataset Dataset;
    /** Model to be evaluated */
    final public BaseModel Model;

    /**
     * Initialize model package
     * @param data Dataset used to generate attributes
     * @param model Model to be evaluated
     */
    public ModelPackage(Dataset data, BaseModel model) {
        this.Dataset = data;
        this.Model = model;
    }
    
}
