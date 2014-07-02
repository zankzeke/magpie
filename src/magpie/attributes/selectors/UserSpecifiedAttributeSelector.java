/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
     * @param Options List of attributes to use
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
     * Define a list of attributes (given either name or number) that will be used 
     *  when training a model
     * @param Attributes List of desired attributes
     */
    public void selectAttributes(List<String> Attributes) {
        SelectedAttributes.clear();
        if (Attributes.isEmpty()) return;
        
        this.SelectedAttributes.addAll(Attributes);
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
}
