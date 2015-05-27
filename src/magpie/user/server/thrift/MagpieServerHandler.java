package magpie.user.server.thrift;

import java.util.*;
import magpie.data.*;
import magpie.data.utilities.filters.EntryRankerFilter;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.data.utilities.modifiers.AddPropertyModifier;
import magpie.models.BaseModel;
import magpie.optimization.rankers.*;
import magpie.user.CommandHandler;
import magpie.user.server.ModelPackage;
import org.apache.commons.lang3.tuple.*;
import org.apache.thrift.TException;

/**
 * Handles requests for the MagpieServer.
 * @author Logan Ward
 * @see MagpieServer
 */
public class MagpieServerHandler implements MagpieServer.Iface {
    /** Information about each model */
    protected Map<String,ModelPackage> ModelInformation = new TreeMap<>();
	/** Maximum number of entries to evaluate (security measure) */
	public static long MaxEntryEvaluations = 50000;
    
    /**
     * Add a new model to the server handler
     * @param name Name of model
     * @param modelInfo Information about model
     */
    public void addModel(String name, ModelPackage modelInfo) {
        ModelInformation.put(name, modelInfo);
    }
    
    @Override                                                                                                                                                                                       
    public List<Entry> evaluateProperties(List<Entry> entries,
            List<String> props) throws TException {
        
		// Get names of entries to be be run
		List<String> entryNames = new ArrayList<>(entries.size());
		for (Entry e : entries) {
			entryNames.add(e.name);
		}

		// Run the model and such
		try {
            
            // Predict each property
            for (String prop : props) {
                
                // Special Case: No model for that property
                if (! ModelInformation.containsKey(prop)) {
                    for (Entry entry : entries) {
                        entry.predictedProperties.put(prop, Double.NaN);
                    }
                    continue;
                }
                
                // Store entries in dataset, create lookup array 
                Dataset dataset = ModelInformation.get(prop).Dataset.emptyClone();
                Map<Integer, Integer> entryToResult = new TreeMap<>();
                List<Integer> failedList = new LinkedList<>();
                for (int i = 0; i < entryNames.size(); i++) {
                    String entry = entryNames.get(i);
                    if (entry.isEmpty()) {
                        continue;
                    }
                    try {
                        BaseEntry parsed = dataset.addEntry(entry);
                        entryToResult.put(dataset.NEntries() - 1, // Index of dataset entry
                                i); // Index of entry in input array
                    } catch (Exception e) {
                        failedList.add(i);
                    }
                }
                
                // Generate attributes used to perform models
                dataset.generateAttributes();
                
                BaseModel model = ModelInformation.get(prop).Model;
                
                // Run model
                model.run(dataset);
                double[] predicted = dataset.getPredictedClassArray();
                
                // Format results
                for (int i=0; i < dataset.NEntries(); i++ ) {
                    int row = entryToResult.get(i);
                    entries.get(row).predictedProperties.put(prop, predicted[i]);
                }
                
                // Add results for the entries that failed to parse
                for (Integer entry : failedList) {
                    entries.get(entry).predictedProperties.put(prop, Double.NaN);
                }
            }
		} catch (Exception e) {
            throw new TException(e);
		}
		
		return entries;
    }

    @Override
    public List<Entry> searchSingleObjective(String obj,
            String genMethod, int numToList) throws TException {

        try {
            Pair<String,BaseEntryRanker> objective = getObjective(obj);
            String property = objective.getLeft();
            BaseEntryRanker ranker = objective.getRight();
            BaseEntryGenerator generator = getGenerator(genMethod);
            
            // Generate the dataset
            Dataset data = ModelInformation.get(property).Dataset;
            generator.addEntriesToDataset(data);
            data.generateAttributes();
            
            // Execute the search
            BaseModel model = ModelInformation.get(property).Model;
            if (model == null) {
                throw new Exception("No model for property: " + property);
            }
            model.run(data);

            // Filter the entries
            EntryRankerFilter filter = new EntryRankerFilter();
            filter.setNumberToFilter(numToList);
            filter.setExclude(false);
            filter.setRanker(ranker);
            filter.train(data);
            filter.filter(data);

            // Print out the results
            int[] ranking = ranker.rankEntries(data);
            List<Entry> output = new ArrayList<>(numToList);
            for (int i : ranking) {
                BaseEntry entry = data.getEntry(i);
                Entry toAdd = new Entry();
                toAdd.name = entry.toString();
                toAdd.predictedProperties.put(property, entry.getPredictedClass());
                output.add(toAdd);
            }
            return output;
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    /**
     * Parse objective function command.
     * @param command Command defining objective function
     * @return Pair: Property considered : EntryRanker for that property
     * @throws Exception 
     */
    protected static Pair<String,BaseEntryRanker> getObjective(String command)
            throws Exception {
        // Get the Components
        String[] obj_command = command.split("\\s+");
        String property;
        boolean toMinimize;
        try {
            property = obj_command[0];
            if (obj_command[1].toLowerCase().startsWith("min")) {
                toMinimize = true;
            } else if (obj_command[1].toLowerCase().startsWith("max")) {
                toMinimize = false;
            } else {
                throw new Exception();
            }
            if (obj_command.length < 3) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception("Objective function format: <property> "
                    + "<minimize|maximize> <method> <options...>");
        }
        BaseEntryRanker ranker = getEntryRanker(obj_command[2],
                Arrays.asList(obj_command).subList(3, obj_command.length));
        ranker.setMaximizeFunction(!toMinimize);
        return new ImmutablePair<>(property, ranker);
    }
    
    /**
     * Given the first line of user command, get the entry generator.
     *
     * @param command First line of command sent to thread
     * @return {@linkplain BaseEntryGenerator}, as specified by command
     * @throws Exception
     */
    protected static BaseEntryGenerator getGenerator(String command) throws Exception {
        // Get the search space
        String[] topCommand = command.split("\\s+");
        String method = topCommand[0];
        List<Object> options = new LinkedList<>();
        for (int i = 1; i < topCommand.length; i++) {
            options.add(topCommand[i]);
        }
        BaseEntryGenerator generator = (BaseEntryGenerator) CommandHandler.instantiateClass(
                "data.utilities.generators." + method, options);
        return generator;
    }

    /**
     * Get an entry ranker for a single property.
     *
     * @param method Name of entry ranker
     * @param options Options for entry ranker
     * @return BaseEntryRanker describing this objective function
     * @throws Exception
     */
    protected static BaseEntryRanker getEntryRanker(String method, 
            List<String> options) throws Exception {
        // Convert options
        List<Object> options_obj = new ArrayList<Object>(options);
        
        // Instantiate ranker
        BaseEntryRanker ranker = (BaseEntryRanker) CommandHandler.instantiateClass(
                "optimization.rankers." + method, options_obj);
        ranker.setUseMeasured(false);
        return ranker;
    }

    @Override
    public List<Entry> searchMultiObjective(double p, List<String> objs, 
            String genMethod, int numToList) throws TException {
        // Get the entry ranker
        AdaptiveScalarizingEntryRanker ranker = new AdaptiveScalarizingEntryRanker();
        ranker.setP(p);
        ranker.setUseMeasured(false);
        
        // Check if objectives are empty
        if (objs.isEmpty()) {
            throw new TException("Objectives cannot be empty");
        }
        
        // Get all of the objectives
        Dataset data = null;
        for (String obj : objs) {
            try {
                Pair<String,BaseEntryRanker> objective = getObjective(obj);
                ranker.addObjectiveFunction(objective.getLeft(),
                    objective.getRight());
                if (data ==  null) {
                    data = ModelInformation.get(objective.getLeft()).Dataset;
                }
            } catch (Exception e) {
                throw new TException(e.getMessage());
            }
        }
        
        // Generate the dataset
        try {
            BaseEntryGenerator generator = getGenerator(genMethod);
            generator.addEntriesToDataset(data);
            data.generateAttributes();
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }


        // Add properties to dataset
        if (!(data instanceof MultiPropertyDataset)) {
            throw new TException("Dataset template is not a MultiPropertyDataset");
        }
        MultiPropertyDataset dataptr = (MultiPropertyDataset) data;
        AddPropertyModifier mdfr = new AddPropertyModifier();
        mdfr.setPropertiesToAdd(Arrays.asList(ranker.getObjectives()));
        mdfr.transform(data);

        // Run models
        for (String prop : ranker.getObjectives()) {
            BaseModel model = ModelInformation.get(prop).Model;
            if (model == null) {
                throw new TException("No model for property: " + prop);
            }
            dataptr.setTargetProperty(prop, true);
            model.run(data);
        }

        // Filter the entries
        EntryRankerFilter filter = new EntryRankerFilter();
        filter.setNumberToFilter(numToList);
        filter.setExclude(false);
        filter.setRanker(ranker);
        filter.train(data);
        filter.filter(data);

        // Print out the results
        int[] ranking = ranker.rankEntries(data);
        List<Entry> output = new ArrayList<>(numToList);
        for (int i : ranking) {
            MultiPropertyEntry entry = (MultiPropertyEntry) data.getEntry(i);
            Entry toAdd = new Entry();
            toAdd.name = entry.toString();
            for (int pr=0; pr<dataptr.NProperties(); pr++) {
                toAdd.predictedProperties.put(dataptr.getPropertyName(pr), 
                        entry.getPredictedProperty(pr));
            }
            output.add(toAdd);
        }
        return output;
    }

    @Override
    public Map<String, ModelInfo> getModelInformation() throws TException {
        Map<String,ModelInfo> output = new TreeMap<>();
        for (Map.Entry<String, ModelPackage> entrySet : ModelInformation.entrySet()) {
            String name = entrySet.getKey();
            ModelPackage pack = entrySet.getValue();
            output.put(name, pack.generateInfo());
        }
        return output;
    }

}
