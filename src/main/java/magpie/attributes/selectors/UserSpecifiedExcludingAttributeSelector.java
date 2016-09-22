package magpie.attributes.selectors;

import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Exclude attributes with a certain name
 * 
 * <usage><p><b>Usage</b>: &lt;names...>
 * <br><pr><i>names</i>: Names of attributes to select</usage>
 * 
 * @author Logan Ward
 */
public class UserSpecifiedExcludingAttributeSelector extends 
        UserSpecifiedAttributeSelector {

    /**
     * Define list of attributes that will be excluded before training a model.
     * @param Attributes List of attribute names
     */
    @Override
    public void selectAttributes(List<String> Attributes) {
        super.selectAttributes(Attributes); 
    }

    @Override
    protected List<Integer> train_protected(Dataset Data) {
        List<Integer> output = new LinkedList<>();
        String[] Names = Data.getAttributeNames();
        
        // Prepare a list of attributes to exclude
        for (int i=0; i<Data.NAttributes(); i++) {
            output.add(i);
        }
        
        // Exclude those attributes
        for (String name : SelectedAttributes) {
            int id = ArrayUtils.indexOf(Names, name);
            if (id == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Error("Attribute \""+name+"\" not found");
            }
            output.remove(id);
        }
        return output;
    }
    
    @Override
    public String printDescription(boolean htmlFormat) {
        String output = "Select only attributes that were not specified by a user:\n";
        
        if (htmlFormat) {
            output += "<br>";
        }
        
        for (String name : SelectedAttributes) {
            output += name + " ";
        }
        
        return output;
    }
}
