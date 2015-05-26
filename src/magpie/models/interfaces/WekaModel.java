package magpie.models.interfaces;

/**
 * Operations that a Weka-based model must implement.
 * @author Logan Ward
 * @version 0.1
 */
public interface WekaModel {

    /** 
     * Return model name and options 
     * @return Name and options of Weka model used
     */
    public String getModelFull();

    /**
     * Return the model name 
     * @return Name of Weka model used
     */
    public String getModelName();

    /** 
     * Return the model options 
     * @return Options of Weka model used
     */
    public String[] getModelOptions();

    /** 
     * Set the underlying Weka-based model
     * @param model_type Model type (ie trees.J48)
     * @param options Options for the model
     * @throws java.lang.Exception
     */
    public void setModel(String model_type, String[] options) throws Exception;
}
