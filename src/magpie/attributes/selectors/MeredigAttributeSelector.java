/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.attributes.selectors;

import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.util.LookupData;
import magpie.data.materials.util.PropertyLists;

/**
 * Select only attributes that were used by <a 
 * href="http://link.aps.org/doi/10.1103/PhysRevB.89.094104">Meredig 
 * <i>et al</i></a>. 
 * 
 * <p><b>Note</b> You must generate the fraction of each element present in the 
 * sample as attributes. 
 * 
 * <usage><p><b>Usage</b>: * No Options *</usage>
 * 
 * @author Logan Ward
 */
public class MeredigAttributeSelector extends UserSpecifiedAttributeSelector {

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        if (! OptionsObj.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No Options*";
    }

    @Override
    public void selectAttributes(List<String> Attributes) {
        throw new Error("Operation not supported for this class");
    }
    
    @Override
    protected List<Integer> train_protected(Dataset Data) {
        // First: Set the names of all attributes used by Meredig
        List<String> names = new LinkedList<>();
        for (String element : LookupData.ElementNames) {
            names.add("X_" + element);
        }
        names.add("mean_AtomicWeight");
        names.add("mean_Column");
        names.add("mean_Row");
        names.add("maxdiff_Number");
        names.add("mean_Number");
        names.add("maxdiff_CovalentRadius");
        names.add("mean_CovalentRadius");
        names.add("maxdiff_Electronegativity");
        names.add("mean_Electronegativity");
        for (String shell : new String[]{"s", "p", "d", "f"}) {
            names.add("mean_N" + shell + "Valence");
            names.add("frac_" + shell + "Valence");
        }
        
        // Second: Set them as the "user-selected" attributes
        super.selectAttributes(names);
        
        // Now, train the model
        return super.train_protected(Data); 
    }    
}
