package magpie.user;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import magpie.attributes.selectors.BaseAttributeSelector;
import magpie.cluster.BaseClusterer;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.optimization.BaseOptimizer;
import magpie.utility.interfaces.Printable;

/**
 * Keeps track of variables produced by user interface. Stored as a map between
 *  a name (String) and the variable (Object)
 * @author Logan Ward
 * @version 0.1
 */
public class Workspace {
    /** Map of variable name to object */
    protected Map<String, Object> Variables;

    /** 
     * Create a Workspace with an empty variable list
     */
    public Workspace() {
        Variables = new TreeMap<>();
    }
    
    /**
     * Get a dataset out of the variable list
     * @param Name Name of variable to be return
     * @return Dataset object if it is in the set
     * @throws Exception If Name is not in set or is not a Dataset Object
     */
    public Dataset getDataset(String Name) throws Exception {
        Dataset x;
        Object ptr = getObject(Name);
        if (! (ptr instanceof Dataset) )
            throw new Exception("ERROR: " + Name + " is not a Dataset");
        x = (Dataset) ptr;
        return x;
    }
    
    /**
     * Get a model out of the workspace
     * @param Name Name of model to be returned
     * @return BaseModel object if it is in the set
     * @throws Exception If Name is not in set or is not a BaseModel Object
     */
    public BaseModel getModel(String Name) throws Exception {
        BaseModel x;
        Object ptr = getObject(Name);
        if (! (ptr instanceof BaseModel) )
            throw new Exception("ERROR: " + Name + " is not a Dataset");
        x = (BaseModel) ptr;
        return x;
    }
    
    /**
     * Get an BaseAttributeSelector out of the Workspace
     * @param Name Name of BaseAttributeSelector to be returned
     * @return BaseAttributeSelector, if in set
     * @throws Exception If Name is not in set, or not an BaseAttributeSelector
     */
    public BaseAttributeSelector getAttributeSelector(String Name) throws Exception {
        BaseAttributeSelector x;
        Object ptr = getObject(Name);
        if (! (ptr instanceof BaseAttributeSelector) )
            throw new Exception("ERROR: " + Name + " is not an AttributeSelector");
        x = (BaseAttributeSelector) ptr;
        return x;
    }
    
    /**
     * Get an BaseClusterer out of the Workspace
     * @param Name Name of BaseClusterer to be returned
     * @return BaseAttributeSelector, if in set
     * @throws Exception If Name is not in set, or not an BaseClusterer
     */
    public BaseClusterer getClusterer(String Name) throws Exception {
        BaseClusterer x;
        Object ptr = getObject(Name);
        if (! (ptr instanceof BaseClusterer) )
            throw new Exception("ERROR: " + Name + " is not an AttributeSelector");
        x = (BaseClusterer) ptr;
        return x;
    }
    
    /**
     * Get an BaseClusterer out of the Workspace
     * @param Name Name of BaseClusterer to be returned
     * @return BaseAttributeSelector, if in set
     * @throws Exception If Name is not in set, or not an BaseClusterer
     */
    public BaseOptimizer getOptimizer(String Name) throws Exception {
        BaseOptimizer x;
        Object ptr = getObject(Name);
        if (! (ptr instanceof BaseOptimizer) )
            throw new Exception("ERROR: " + Name + " is not an AttributeSelector");
        x = (BaseOptimizer) ptr;
        return x;
    }
    
    /**
     * Test whether the user has created a variable
     * @param Name Name of variable
     * @return Whether it exists
     */
    public boolean hasVariable(String Name) {
        return Variables.containsKey(Name);
    }
    
    /**
     * Removes a variable from the workspace
     * @param Name Variable to be removed
     */
    public void removeVariable(String Name) {
        if (hasVariable(Name))
            Variables.remove(Name);
    }
    
    /**
     * Adds a variable to the workspace. Throws exception if variable is not of one
     * of the defined types {@link Workspace}.
     * @param Name Name of variable 
     * @param obj Pointer to variable
     * @throws Exception if variable has already been assigned
     */
    public void addVariable(String Name, Object obj) throws Exception {
        Variables.put(Name, obj);
    }
    
    /**
     * Gets an object that has been created by the user 
     * @param Name name of variable to retrieve
     * @return hasVariable(Name) ? That object : exception;
     * @throws Exception if variable does not exist
     */
    public Object getObject(String Name) throws Exception {
        if (hasVariable(Name))
            return Variables.get(Name);
        else {
            throw new Exception("ERROR: Variable " + Name + " not been set.");
        }
    }
    
    /**
     * Prints a table containing all variables currently in Workspace. Format:<p>
     *         Name	                Type	About<br>
     *   &lt;Variable name&gt;	  &lt;Variable type&gt; &lt;About variable&gt;<br>
     *  ...
     * @return String formatted as specified
     */
    public String printWorkspace() {
        String FormatString = "%12s\t%20s\t%s\n";
        String output = String.format(FormatString, "Name", "Type", "About");
        Iterator<String> iter = Variables.keySet().iterator();
        String Name, Type, About; Object Obj;
        while (iter.hasNext()) {
            Name = iter.next();
            try { Obj = getObject(Name); }
            catch (Exception e) { throw new Error("There should not be an error here!"); }
            Type = Obj.getClass().getSimpleName();
            if (Obj instanceof Printable) {
                Printable Ptr = (Printable) Obj;
                About = Ptr.about();
            } else About = "";
            output += String.format(FormatString, Name, Type, About);
        }
        return output;
    }
}
