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
    protected List<String> Selected_Attributes = new LinkedList<>();

    /**
     * Sets the object to select only attributes listed in input
     * @param Options List of attributes to use
     */
    @Override
    public void setOptions(List<Object> OptionsObj) {
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
        x.Selected_Attributes = new LinkedList<>(Selected_Attributes);
        return x; //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Define a list of attributes (given either name or number) that will be used 
     *  when training a model
     * @param Attributes List of desired attributes
     */
    public void selectAttributes(List Attributes) {
        Selected_Attributes.clear();
        if (Attributes.isEmpty()) return;
        
        if (Attributes.get(0) instanceof String)
            this.Selected_Attributes.addAll(Attributes);
        else if (Attributes.get(0) instanceof Integer)
            this.Attribute_ID = Attributes;
        else
            throw new Error("Attributes must contain Integers or Strings (what did you put in this!?)");
    }

    @Override
    protected void train_protected(Dataset Data) { 
        // Nothing needs to be trained 
    }

    @Override
    public void run(Dataset Data) {
        if (! Selected_Attributes.isEmpty()) {
            Attribute_ID.clear();
            String[] Names = Data.getAttributeNames();
            for (int i=0; i<Attribute_ID.size(); i++) {
                int id = ArrayUtils.indexOf(Names, Selected_Attributes.get(i));
                if (id == ArrayUtils.INDEX_NOT_FOUND)
                    throw new Error("Attribute \""+Selected_Attributes.get(i)+"\" not found");
                Attribute_ID.add(id);
            }
        }
        super.run(Data);
    }
}
