package magpie.user.server;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;
import magpie.user.server.thrift.ModelInfo;

/**
 * Holds information about a model.
 * @author Logan Ward
 */
public class ModelPackage {
    /** Dataset used to generate attributes */
    final public Dataset Dataset;
    /** Model to be evaluated */
    final public BaseModel Model;
    /** Name of property being modeled. HTML format suggested */
    public String Property = "Unspecified";
    /** Units for property */
    public String Units  = "Unspecified";
    /** Training set description */
    public String TrainingSet  = "Unspecified";
    /** Author of this model */
    public String Author = "Unspecified";
    /** Citation for this model */
    public String Citation = "Unspecified";
    /** Any other information about the model */
    public String Description = "";

    /**
     * Initialize model package
     * @param data Dataset used to generate attributes
     * @param model Model to be evaluated
     */
    public ModelPackage(Dataset data, BaseModel model) {
        this.Dataset = data;
        this.Model = model;
    }
    
    /**
     * Generate model info in a format suitable for Thrift interface
     * @return Model info in Thrift format
     */
    public ModelInfo generateInfo() { 
        ModelInfo info = new ModelInfo();
        
        // Store basic info
        info.author = Author;
        info.citation = Citation;
        info.notes = Description;
        info.property = Property;
        info.training = TrainingSet;
        
        // Store units or class names
        if (Model instanceof AbstractClassifier) {
            info.units = "";
            AbstractClassifier clfr = (AbstractClassifier) Model;
            boolean started = false;
            for (String name : clfr.getClassNames()) {
                if (started) {
                    info.units += ";";
                }
                info.units += name;
                started = true;
            }
        } else {
            info.units = Units;
        }
        info.dataType = Dataset.getClass().getSimpleName();
        info.modelType = Model.getClass().getSimpleName();
        return info;
    }
}
