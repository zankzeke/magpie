/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.utility;

import java.util.Arrays;
import java.util.List;
import magpie.models.BaseModel;
import magpie.models.interfaces.MultiModel;
import magpie.utility.UtilityOperations;

/**
 * Contains commands to handle MultiModel operations
 * @author Logan Ward
 * @version 0.1
 */
abstract public class MultiModelUtility {
    

    /**
     * Handles printing commands specific to MultiModels. Adds two commands to the printing (copy these
	 * to the Javadoc of any class that uses this operation)
	 * 
	 * <print><p><b>submodel</b> - Print out number of submodels</print>
	 * 
	 * <print><p><b>submodel &lt;number> [&lt;command...>]</b> - Pass a print command to one of the submodels
	 * <br><pr><i>number</i>: Index of model to operate on (starts at 0)
	 * <br><pr><i>command</i>: Print command that gets passed to that submodel</print>
	 * 
     * @param Model Model to operate on
     * @param Command Command to parse (should start with "submodel")
     * @return Product of print command
     * @throws Exception If something goes wrong
     */
    static public String handleSubmodelPrintCommand(MultiModel Model, List<String> Command) throws Exception {
        if (Command.size() == 1)
            return "Number of submodels: " + Integer.toString(Model.NModels());
		String Action = Command.get(1);
        if (UtilityOperations.isInteger(Action)) {
            int SubModelNumber = Integer.parseInt(Action);
            if (SubModelNumber >= Model.NModels()) {
                throw new Exception("ERROR: There are only " + Model.NModels() + " submodels");
            }
            List<String> SubModelCommand = Command.subList(2, Command.size());
			return Model.getModel(SubModelNumber).printCommand(SubModelCommand);
        }
        throw new Exception("ERROR: Submodel command not recognized.");
    }

    /**
     * Runs commands that are specific to {@linkplain MultiModel}-based classes.
     *
     * <p>Currently provides the following operations (make sure to copy this into
     * class description):
     *
     * <p><b><u>Implemented Commands</u></b>:
     *
     * <command><p><b>submodel</b> - Print the number of submodels</command>
     *
     * <command><p><b>submodel set generic $&lt;model></b> - Define a model template to use for all submodels
     * <br><pr><i>model</i>: An instance of {@linkplain BaseModel}.
     * Note: Do not use this command for {@linkplain CompositeRegression} unless each
     * model automatically uses a different random number seed. Otherwise, each
     * submodel will be identical.</command>
     *
     * <command><p><b>submodel set &lt;number> $&lt;model></b> - Set a specific submodel
     * <br><pr><i>number</i>: Index of the submodel to set (list starts with 0)
     * <br><pr><i>model</i>: An instance of {@linkplain BaseModel} to use for that model</command>
     *
     * <command><p><b>submodel get generic = &lt;output></b> - Retrieve the template for any unassigned submodels</command>
     *
     * <command><p><b>submodel get &lt;number> = &lt;output></b> - Retrieve a specific submodel
     * <br><pr><i>number</i>: Index of submodel to retrieve (list starts with 0)
     * Returns a clone of the model - you cannot use this to edit the model.</command>
     *
     * @param Model Model to be operated on
     * @param Command Command to be run (should not start with "submodel")
     * @return Something of use (depends on command issued)
     * @throws Exception
     */
    public static Object handleSubmodelCommand(BaseModel Model, List<Object> Command) throws Exception {
        MultiModel Ptr = (MultiModel) Model;
        if (Command.isEmpty()) {
            System.out.println("\tModel has " + Ptr.NModels() + " submodels");
            return null;
        }
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "get":
                {
                    // Usage: get submodel <generic|number> = <model>
                    int modelNumber = 0; // Set to -1 for generic model
                    try {
                        if (Command.get(1).toString().toLowerCase().startsWith("gen")) {
                            modelNumber = -1;
                        } else {
                            modelNumber = Integer.parseInt(Command.get(1).toString());
                        }
                    } catch (Exception e) {
                        throw new Exception("Usage: submodel set <generic|#> = <model>");
                    }
                    if (modelNumber == -1) {
                        return Ptr.getGenericModel();
                    } else {
                        Ptr.getModel(modelNumber).clone();
                    }
                }
                break;
            case "set": {
                    // Usage: set submodel <generic|number> $<model>
                    int modelNumber = 0; // Set to -1 for generic model
                    BaseModel model;
                    try {
                        if (Command.get(1).toString().toLowerCase().startsWith("gen")) {
                            modelNumber = -1;
                        } else {
                            modelNumber = Integer.parseInt(Command.get(1).toString());
                        }
                        model = (BaseModel) Command.get(2);
                    } catch (Exception e) {
                        throw new Exception("Usage: submodel set <generic|#> $<model>");
                    }
                    if (modelNumber == -1) {
                        Ptr.setGenericModel(model);
                    } else {
                        Ptr.setModel(modelNumber, model);
                    }
                }
                break;
            default:
                throw new Exception("ERROR: Submodel command not recognized: " + Action);
        }
        return null;
    }
    
}
