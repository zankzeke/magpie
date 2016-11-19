package magpie.user.server;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;
import org.json.JSONObject;

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
    /** Short description of this model */
    public String Description = "";
    /** Long form description of model */
    public String Notes;

    /**
     * Initialize model package
     * @param data Dataset used to generate attributes
     * @param model Model to be evaluated
     */
    public ModelPackage(Dataset data, BaseModel model) {
        this.Dataset = data;
        this.Model = model;
    }

    public JSONObject toJSON() {
        JSONObject output = new JSONObject();

        // Add in the data
        output.put("property", Property);
        output.put("units", Model instanceof AbstractClassifier ?
                ((AbstractClassifier) Model).getClassNames() :
                Property);
        output.put("trainingSetDescription", TrainingSet);
        output.put("trainingSetSize", Model.TrainingStats.NumberTested);
        output.put("author", Author);
        output.put("citation", Citation);
        output.put("description", Description);
        output.put("notes", Notes);
        output.put("modelTrainedDate", Model.getTrainTime().toString());
        output.put("modelDetails", Model.printDescription(true));
        output.put("datasetDetails", Model.printDescription(true));

        return output;
    }
}
