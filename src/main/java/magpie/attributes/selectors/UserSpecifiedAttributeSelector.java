package magpie.attributes.selectors;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Use only attributes with a specified name or ID number.
 * 
 * <usage><p><b>Usage</b>: &lt;names...>
 * <br><pr><i>names</i>: Names of attributes to select</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */

public class UserSpecifiedAttributeSelector extends BaseAttributeSelector {
    /** Names of attributes to be used */
    protected List<String> SelectedAttributes = new LinkedList<>();

    /**
     * Sets the object to select only attributes listed in input
     * @param OptionsObj List of attributes to use
	 * @throws java.lang.Exception
     */
    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        if (OptionsObj.isEmpty()) {
            throw new Exception(printUsage());
        }
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        List Attributes = new LinkedList();
        Attributes.addAll(Arrays.asList(Options));
        selectAttributes(Attributes);
    }

    @Override
    public String printUsage() {
        return "Usage: <attributeNames...>";
    }

    @Override
    public UserSpecifiedAttributeSelector clone() {
        UserSpecifiedAttributeSelector x = (UserSpecifiedAttributeSelector) super.clone();
        x.SelectedAttributes = new LinkedList<>(SelectedAttributes);
        return x; //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Clear list of user-specified attributes
     */
    public void clearSpecifiedAttributes() {
        SelectedAttributes.clear();
        trained = false;
    }
    
    /**
     * Add an attribute to the list of attributes to be selected
     * @param attribute Name of attribute
     */
    public void addAttributeSpecification(String attribute) {
        SelectedAttributes.add(attribute);
        trained = false;
    }
    
    /**
     * Define a list of attributes that will be used when training a model
     * @param Attributes List of desired attributes
     */
    public void selectAttributes(List<String> Attributes) {
        SelectedAttributes.clear();
        if (Attributes.isEmpty()) {
			throw new Error("You must provide at least one attribute");
		}
        
        SelectedAttributes.addAll(Attributes);
		trained = false;
    }

    @Override
    protected List<Integer> train_protected(Dataset Data) { 
        List<Integer> output = new LinkedList<>();
        String[] Names = Data.getAttributeNames();
        for (String name : SelectedAttributes) {
            int id = ArrayUtils.indexOf(Names, name);
            if (id == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Error("Attribute \""+name+"\" not found");
            }
            output.add(id);
        }
        return output;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = "Select only attributes that were specified by a user:\n";
        
        if (htmlFormat) {
            output += "<br>";
        }
        
        for (String name : SelectedAttributes) {
            output += name + " ";
        }
        
        return output;
    }
}
