package magpie.user.server.thrift;

import java.util.*;
import magpie.data.*;
import magpie.data.utilities.filters.EntryRankerFilter;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.data.utilities.modifiers.AddPropertyModifier;
import magpie.models.BaseModel;
import magpie.models.regression.AbstractRegressionModel;
import magpie.optimization.rankers.*;
import magpie.user.CommandHandler;
import org.apache.commons.lang3.tuple.*;
import org.apache.thrift.TException;

/**
 * Handles requests for the MagpieServer.
 * @author Logan Ward
 * @see MagpieServer
 */
public class MagpieServerHandler implements MagpieServer.Iface {
    /** Dataset template (used to calculate attributes) */
    protected Dataset TemplateDataset = null;
    /** Model to be run */
	protected Map<String,BaseModel> Models = new TreeMap<>();
	/** Maximum number of entries to evaluate (security measure) */
	public static long MaxEntryEvaluations = 50000;
    
    /**
     * Add a new model to the server handler
     * @param name Name of model (i.e., property that it models)
     * @param model Copy of model
     */
    public void addModel(String name, BaseModel model) {
        Models.put(name, model.clone());
    }

    /**
     * Define the dataset used to generate attributes
     * @param dataset Desired dataset
     */
    public void setTemplateDataset(Dataset dataset) {
        TemplateDataset = dataset.emptyClone();
    }
    
    @Override
    public List<List<String>> evaluateProperties(List<Entry> entries,
            List<String> props) throws TException {
        
		// Get names of entries to be be run
		List<String> entryNames = new ArrayList<>(entries.size());
		for (Entry e : entries) {
			entryNames.add(e.name);
		}
		
		// Provide location to store results for each entry
		List<List<String>> results = new LinkedList();
		for (String entry : entryNames) {
			List<String> toAdd = new LinkedList<>();
			results.add(toAdd);
		}
		
		// Store entries in dataset, create lookup array 
		Dataset dataset = TemplateDataset.emptyClone();
		Map<Integer, Integer> entryToResult = new TreeMap<>();
		List<Integer> failedList = new LinkedList<>();
		for (int i=0; i < entryNames.size(); i++) {
			String entry = entryNames.get(i);
			if (entry.isEmpty()) continue;
			try {
				BaseEntry parsed = dataset.addEntry(entry);
				entryToResult.put(dataset.NEntries()-1 , i);
			} catch (Exception e) {
				failedList.add(i);
			}
		}

		// Run the model and such
		try {
            
            // Add names of predicted properties to output
			for (Map.Entry<Integer, Integer> item : entryToResult.entrySet()) {
				BaseEntry entry = dataset.getEntry(item.getKey());
				Integer pos = item.getValue();		
				results.get(pos).add(entry.toString());
			}
            
            // Generate attributes used to perform models
			dataset.generateAttributes();
            
            // Predict each property
            for (String prop : props) {
                BaseModel model = Models.get(prop);
                
                // Special Case: No model for that property
                if (model == null) {
                    for (Integer pos : entryToResult.values()) {
						results.get(pos).add("No Model");
					}
                    continue;
                }
                
                // Run model
                model.run(dataset);
                double[] predicted = dataset.getPredictedClassArray();
                
                // Format results
                if (model instanceof AbstractRegressionModel) {
                    for (int i=0; i < dataset.NEntries(); i++ ) {
						int row = entryToResult.get(i);
                        results.get(row).add(String.format("%.3f", predicted[i]));
                    }
                } else {
                    double[][] probs = dataset.getClassProbabilityArray();
                    for (int i=0; i < dataset.NEntries(); i++ ) {
						int row = entryToResult.get(i);
                        results.get(row).add(String.format("%s (%.2f%%)", 
                                dataset.getClassName((int) Math.round(predicted[i])),
								probs[i][(int) predicted[i]] * 100.0));
                    }
                }
            }
		} catch (Exception e) {
            throw new TException(e);
		}
		
		// Add unparseable entries
		for (Integer entry : failedList) {
			results.get(entry).add(entryNames.get(entry));
			for (int i=0; i < Math.max(1, props.size()); i++) {
				results.get(entry).add("NA");
			}
		}
		
		return results;
    }

    @Override
    public List<Entry> searchSingleObjective(String obj,
            String gen_method, int to_list) throws TException {

        try {
            Pair<String,BaseEntryRanker> objective = getObjective(obj);
            String property = objective.getLeft();
            BaseEntryRanker ranker = objective.getRight();
            BaseEntryGenerator generator = getGenerator(gen_method);
            
            // Generate the dataset
            Dataset data = TemplateDataset.emptyClone();
            generator.addEntriesToDataset(data);
            
            // Execute the search
            BaseModel model = Models.get(property);
            if (model == null) {
                throw new Exception("No model for property: " + property);
            }
            model.run(data);

            // Filter the entries
            EntryRankerFilter filter = new EntryRankerFilter();
            filter.setNumberToFilter(to_list);
            filter.setExclude(false);
            filter.setRanker(ranker);
            filter.train(data);
            filter.filter(data);

            // Print out the results
            int[] ranking = ranker.rankEntries(data);
            List<Entry> output = new ArrayList<>(to_list);
            for (int i : ranking) {
                BaseEntry entry = data.getEntry(i);
                output.add(new Entry(entry.toString(), 
                        new TreeMap<String, Double>()));
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
    public List<Entry> searchMultiObjective(double p, List<String> objs, String gen_method, int to_list) throws TException {
        // Get the entry ranker
        AdaptiveScalarizingEntryRanker ranker = new AdaptiveScalarizingEntryRanker();
        ranker.setP(p);
        ranker.setUseMeasured(false);
        
        // Generate the dataset
        Dataset data = TemplateDataset.emptyClone();
        try {
            BaseEntryGenerator generator = getGenerator(gen_method);
            generator.addEntriesToDataset(data);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }

        // Get all of the objectives
        for (String obj : objs) {
            try {
                Pair<String,BaseEntryRanker> objective = getObjective(obj);
                ranker.addObjectiveFunction(objective.getLeft(),
                    objective.getRight());
            } catch (Exception e) {
                throw new TException(e.getMessage());
            }
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
            BaseModel model = Models.get(prop);
            if (model == null) {
                throw new TException("No model for property: " + prop);
            }
            dataptr.setTargetProperty(prop, true);
            model.run(data);
        }

        // Filter the entries
        EntryRankerFilter filter = new EntryRankerFilter();
        filter.setNumberToFilter(to_list);
        filter.setExclude(false);
        filter.setRanker(ranker);
        filter.train(data);
        filter.filter(data);

        // Print out the results
        int[] ranking = ranker.rankEntries(data);
        List<Entry> output = new ArrayList<>(to_list);
        for (int i : ranking) {
            MultiPropertyEntry entry = (MultiPropertyEntry) data.getEntry(i);
            output.add(new Entry(entry.toString(), new TreeMap<String, Double>()));
        }
        return output;
    }

}
