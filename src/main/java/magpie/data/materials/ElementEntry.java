package magpie.data.materials;

import magpie.data.MultiPropertyEntry;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;

/**
 * Holds identity and properties of an element.
 * 
 * @author Logan Ward
 */
public class ElementEntry extends MultiPropertyEntry {
    /** Element name */
    final private String ElementName;
    /** Element ID. (Z-1)*/
    final private int ElementID;

    /**
     * Create a new entry
     * @param element Element abbreviation
     * @throws Exception 
     */
    public ElementEntry(String element) throws Exception {
        ElementName = element;
        
        // Get the ID of this element
        if (element.equals("D") || element.equals("T")) {
            ElementID = 0;
            System.err.println("WARNING: H is considered identical to D and T"
                    + "in elemental property lookup tables.");
        } else {
            ElementID = ArrayUtils.indexOf(LookupData.ElementNames, element);
            if (ElementID == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Exception("Element not recognized: " + element);
            }
        }
    }

    /**
     * Create an entry given atomic number or element ID. 
     * @param ID ID number of the element
     * @param isZ Whether this ID number is the atomic number or (atomic number - 1)
     */
    public ElementEntry(int ID, boolean isZ) {
        ElementID = isZ ? ID - 1 : ID;
        ElementName = LookupData.ElementNames[ElementID];
    }

    /**
     * Get the ID of this element
     * @return 
     */
    public int getElementID() {
        return ElementID;
    }
    
    /**
     * Given an elemental-property lookup array, get the property for this entry.
     * @param lookup Lookup table of elemental properties
     * @return Property for this entry
     */
    public double getLookupValue(double[] lookup) {
        return lookup[ElementID];
    }

    @Override
    public String toString() {
        return ElementName;
    }

    @Override
    public String toHTMLString() {
        return toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject output = super.toJSON();
        
        output.put("element", ElementName);
        
        return output;
    }
}
