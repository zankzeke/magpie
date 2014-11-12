/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.materials.util;

/**
 * Supplies lists of properties used when generating attributes. For now, 
 * it contains elemental properties that are used when generating attributes of compounds.<p>
 * 
 * Idea for expansion, create extensions of this class for different kinds of data, 
 * allow user to specify which one to use in the text interface.
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class PropertyLists {

    /** 
     * Generate a list of properties based on pre-defined sets. Currently available
     * lists are:<p>
     * <ul>
     * <li>heusler - Properties designed for Heusler (or other BCC) compounds</li>
     * <li>general - General set for any materials dataset</li>
     * </ul>
     * @param name Name of pre-defined list
     * @return A list populated with all elemental properties to be used
     * @throws Exception If property set not defined
     */
    public static String[] getPropertySet(String name) throws Exception {
        switch (name) {
            case "heusler":
                return new String[]{"Number", "MendeleevNumber", "AtomicWeight", "MeltingT",
                    "Column", "Row", "CovalentRadius", "Electronegativity", "NsValence",
                    "NpValence", "NdValence", "NfValence", "NValance", "NsUnfilled",
                    "NpUnfilled", "NdUnfilled", "NfUnfilled", "NUnfilled", "GSvolume_pa",
                    "GSbandgap", "GSmagmom", "BCCvolume_pa", "BCCefflatcnt",
                    "BCCenergydiff", "BCCvolume_padiff", "GSefflatcnt", "SpaceGroupNumber"};
            case "general":
                return new String[]{"Number", "MendeleevNumber", "AtomicWeight",
                    "MeltingT", "Column", "Row", "CovalentRadius", "Electronegativity",
                    "NsValence", "NpValence", "NdValence", "NfValence", "NValance",
                    "NsUnfilled", "NpUnfilled", "NdUnfilled", "NfUnfilled", "NUnfilled",
                    "GSvolume_pa", "GSbandgap", "GSmagmom", "SpaceGroupNumber"};
            case "yue":
                return new String[]{"Number", "MendeleevNumber", "AtomicWeight",
                    "MeltingT", "Column", "Row", "CovalentRadius", "Electronegativity",
                    "NsValence", "NpValence", "NdValence", "NfValence", "NValance",
                    "NsUnfilled", "NpUnfilled", "NdUnfilled", "NfUnfilled", "NUnfilled",
                    "GSvolume_pa", "GSbandgap", "GSmagmom", "SpaceGroupNumber", "BulkModulus",
                    "ShearModulus", "phi", "n_ws^third"};
            default:
                throw new Exception("Property set \"" + name + "\" not yet defined.");
        }
    }
    
}
