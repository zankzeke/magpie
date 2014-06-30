/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.models.regression;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.models.BaseModel;
import magpie.optimization.rankers.EntryRanker;
import magpie.optimization.rankers.MultiObjectiveEntryRanker;
import magpie.user.CommandHandler;

/**
 * Employs multiple models, each designed to predict a different property that composes
 *  the class variable. Class variable is defined by a {@link MultiObjectiveEntryRanker}.
 * 
 * <usage><p><b>Usage</b>: &lt;ranker method&gt; [&lt;ranker options...&gt;]
 * <br><pr><i>ranker method</i>: Multi-objective function ("?" for options)
 * <br><pr><i>ranker options</i>: Any options for the objective function</usage>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>submodel</b> - Print type and status of model associated with each property</command>
 * 
 * <command><p><b>&lt;model&gt; = submodel get &lt;property&gt;</b> - Get a model for a certain property
 * <br><pr><i>property</i>: Name of property associated with model</command>
 * 
 * <command><p><b>submodel set &lt;property&gt; $&lt;model&gt;</b> - Set a model for a certain property
 * <br><pr><i>property</i>: Name of property associated with model
 * <br><pr><i>model</i>: Regression model to use</command>
 * 
 * <command><p><b>submodel set generic $&lt;model&gt;</b> - Set a model for any property without an associated model
 * <br><pr><i>model</i>: Regression model to use</command>
 * 
 * <command><p><b>&lt;output&gt; = submodel get &lt;property> </b> - Get a model associated with a certain property
 * <br><pr><i>property</i>: Name of property associated with desired model</command>
 * 
 * @author Logan Ward
 */
public class MultiObjectiveRegression extends BaseRegression {
    /** Map of property name to model used to predict it. */
    protected Map<String,BaseModel> Models = new TreeMap<>();
    /** Objective function used to calculate class variable. */
    protected MultiObjectiveEntryRanker ObjFunction;
    /** Generic model (in case one has not already been defined) */
    protected BaseModel GenericModel;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String method;
        List<Object> rankerOptions;
        try {
            method = Options.get(0).toString();
            if (method.equals("?")) {
                System.out.println("Available multi-objective functions:");
                System.out.println(CommandHandler.printImplmentingClasses(MultiObjectiveEntryRanker.class, false));
                return;
            }
            rankerOptions = Options.subList(1, Options.size());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        EntryRanker obj = (EntryRanker) CommandHandler.instantiateClass(
                "optimization.rankers." + method, rankerOptions);
        if (! (obj instanceof MultiObjectiveEntryRanker)) {
            throw new Exception("Ranker must be a MultiObjectiveEntryRanker");
        }
        setObjectiveFunction((MultiObjectiveEntryRanker) obj);
    }

    @Override
    public String printUsage() {
        return "Usage: <ranker method> [<options...>]";
    }

    /**
     * Define the objective function used to calculate perform of each entry. This value is 
     *  assigned to the class variable (allows this model to be validated)
     * @param objFunction Objective function to use
     */
    public void setObjectiveFunction(MultiObjectiveEntryRanker objFunction) {
        this.ObjFunction = objFunction;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public MultiObjectiveRegression clone() {
        MultiObjectiveRegression x = (MultiObjectiveRegression) super.clone(); 
        x.Models = new TreeMap<>(Models);
        x.ObjFunction = ObjFunction.clone();
        return x;
    }
    
    /**
     * Get the model designed to predict a certain property. 
     * @param propertyName Name of property to be modeled
     * @return Clone of that model, or <code>null</code> if no model exists 
     *  for that property
     */
    public BaseModel getModel(String propertyName) {
        if (modelIsDefined(propertyName)) {
            return Models.get(propertyName).clone();
        } else {
            return null;
        }
    }
    
    /**
     * Define a model for a certain property. Will overwrite an existing model
     *  for that property.
     * @param propertyName Name of property
     * @param model Example of model to be used
     */
    public void setModel(String propertyName, BaseModel model) throws Exception {
        if (! (model instanceof AbstractRegressionModel)) {
            throw new Exception("Error: Model must be a regression model.");
        }
        Models.put(propertyName, model.clone());
    }

    /**
     * Define the model to be used for a property if one is not specifically defined.
     * @param model Generic regression model
     */
    public void setGenericModel(BaseModel model) throws Exception {
        if (! (model instanceof AbstractRegressionModel)) {
            throw new Exception("Error: Model must be a regression model.");
        }
        this.GenericModel = model.clone();
    }
    
    /**
     * Determine whether a model for a certain property is defined 
     * @param propertyName Name of property
     * @return Whether a model is defined
     */
    public boolean modelIsDefined(String propertyName) {
        return Models.containsKey(propertyName);
    }
    
    /**
     * Whether the model for a certain property is trained.
     * @param propertyName Name of property
     * @return Whether Model is defined and trained
     */
    public boolean modelIsTrained(String propertyName) {
        if (modelIsDefined(propertyName)) {
            return Models.get(propertyName).isTrained();
        } else {
            return false;
        }
    }
    
    /**
     * Whether the model for a certain property is validated
     * @param propertyName Name of property
     * @return Whether Model is defined and validated
     */
    public boolean modelIsValidated(String propertyName) {
        if (modelIsDefined(propertyName)) {
            return Models.get(propertyName).isValidated();
        } else {
            return false;
        }
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        if (! (TrainData instanceof MultiPropertyDataset)) {
            throw new Error("Data must extend MultiPropertyDataset");
        }
        
        // Calculate the value of each property
        MultiPropertyDataset d = (MultiPropertyDataset) TrainData;
        for (String prop : ObjFunction.getObjectives()) {
            if (! modelIsDefined(prop)) {
                try {
                    setModel(prop, GenericModel.clone());
                } catch (Exception e) {
					if (GenericModel == null) {
						throw new Error("No model defined for property (and no generic): " + prop);
					} else {
						throw new Error(e); // Should not happen
					}
                }
            }
            BaseModel model = Models.get(prop);
            d.setTargetProperty(prop);
            model.train(TrainData);
        }
        
        // Ensure the measured composite value is calculated for each entry
        ObjFunction.setUseMeasured(true);
        ObjFunction.train(d);
        d.setTargetProperty(-1); // Preparing to store target variable
        for (int i=0; i<d.NEntries(); i++) {
            d.getEntry(i).setMeasuredClass(ObjFunction.objectiveFunction(d.getEntry(i)));
        }
    }

    @Override
    public void run_protected(Dataset TrainData) {
        // Calculate the value of each property
        MultiPropertyDataset d = (MultiPropertyDataset) TrainData;
        for (Map.Entry<String, BaseModel> entry : Models.entrySet()) {
            String prop = entry.getKey();
            BaseModel model = entry.getValue();
            d.setTargetProperty(prop);
            model.run(TrainData);
        }
        
        // Ensure the measured composite value is calculated for each entry
        ObjFunction.setUseMeasured(false);
        d.setTargetProperty(-1); // Prepare to store target variable
        for (int i=0; i<d.NEntries(); i++) {
            d.getEntry(i).setPredictedClass(ObjFunction.objectiveFunction(d.getEntry(i)));
        }
    }

    @Override
    public int getNFittingParameters() {
        int output = 0;
        for (BaseModel model : Models.values()) {
            AbstractRegressionModel p = (AbstractRegressionModel) model;
            output += p.getNFittingParameters();
        }
        return output;
    }

    @Override
    protected String printModel_protected() {
        String output = "";
        for (Map.Entry<String, BaseModel> entry : Models.entrySet()) {
            String property = entry.getKey();
            BaseModel model = entry.getValue();
            output += ("\nModel for " + property + ":\n");
            output += model.printModel();
        }
        return output;
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            return super.runCommand(Command);
        }
        String Action = Command.get(0).toString();
        switch (Action.toLowerCase()) {
            case "submodel":
                return runSubmodelCommand(Command.subList(1, Command.size()));
            default:
                return super.runCommand(Command);
        }
    }
    
    /**
     * Run commands associated with operating on submodels
     * @param Command Command to run
     * @return Any associated output
     * @throws Exception If something goes wrong
     */
    protected Object runSubmodelCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.print("Current models:\n");
            for (Map.Entry<String, BaseModel> entry : Models.entrySet()) {
                String prop = entry.getKey();
                BaseModel model = entry.getValue();
                System.out.format("\t%s: %s - %s\n", prop, model.getClass().getSimpleName(), model.about());
            }
        }
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "set": {
                String name;
                BaseModel model;
                try {
                    name = Command.get(1).toString();
                    model = (BaseModel) Command.get(2);
                } catch (Exception e) {
                    throw new Exception("Usage: submodel set <property|generic> $<model>");
                }
                if (name.equalsIgnoreCase("generic")) {
                    setGenericModel(model);
                } else {
                    setModel(name, model);
                }
                return null;
            }
            case "get": {
                String name;
                try {
                    name = Command.get(1).toString();
                } catch (Exception e) {
                    throw new Exception("Usage: <model> = submodel get <property>");
                }
                if (! modelIsDefined(name)) {
                    throw new Exception("No model defined for property:" + name);
                }
                return getModel(name);
            }
            default:
                throw new Exception("Submodel command not recognized:" + Action);
        }
    }
}
