package magpie.user.server.thrift;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.utilities.filters.EntryRankerFilter;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.models.BaseModel;
import magpie.models.regression.AbstractRegressionModel;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.user.CommandHandler;
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
            // Get the Components
            String[] obj_command = obj.split("\\s+");
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
                throw new Exception("Objective function format: <property> " +
                        "<minimize|maximize> <method> <options...>");
            }
            
            BaseEntryRanker ranker = getObjectiveFunction( obj_command[2],
                    Arrays.asList(obj_command).subList(3, obj_command.length));
            ranker.setMaximizeFunction(!toMinimize);
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
    protected static BaseEntryRanker getObjectiveFunction(String method, 
            List<String> options) throws Exception {
        // Convert options
        List<Object> options_obj = new ArrayList<Object>(options);
        
        // Instantiate ranker
        BaseEntryRanker ranker = (BaseEntryRanker) CommandHandler.instantiateClass(
                "optimization.rankers." + method, options_obj);
        ranker.setUseMeasured(false);
        return ranker;
    }

//    /**
//     * Evaluate a multi-objective entry ranker that uses adaptive scalarization.
//     *
//     * @param command Command describing objective
//     * @param data Dataset to be evaluated
//     * @param models Models to be used
//     * @return Table of results of search (i.e. entry name)
//     */
//    protected static List<String> runMultiObjectiveScalarized(List<String> command, Dataset data,
//            Map<String, BaseModel> models) throws Exception {
//        List<String> output = new LinkedList<>();
//
//        // Parse the objective command
//        String[] words = command.get(1).split("\\s+");
//        double p;
//        try {
//            p = Double.parseDouble(words[1]);
//        } catch (Exception e) {
//            throw new Exception("Expected second word of multi to be the P value");
//        }
//
//        // Get the entry ranker
//        AdaptiveScalarizingEntryRanker ranker = new AdaptiveScalarizingEntryRanker();
//        ranker.setP(p);
//        ranker.setUseMeasured(false);
//
//        // Get all of the propeties
//        List<String> properties = new LinkedList<>();
//        for (String line : command.subList(2, command.size() - 1)) {
//            words = line.split("\\s+");
//            try {
//                String prop = words[0];
//                BaseEntryRanker obj = getObjectiveFunction(words);
//                ranker.addObjectiveFunction(prop, obj);
//                properties.add(prop);
//            } catch (Exception e) {
//                throw new Exception("Failed parsing objective on line: " + line);
//            }
//        }
//
//        // Get the number of entries
//        int number = getNumberToReport(command);
//
//        // Add properties to dataset
//        if (!(data instanceof MultiPropertyDataset)) {
//            throw new Exception("Dataset template is not a MultiPropertyDataset");
//        }
//        MultiPropertyDataset dataptr = (MultiPropertyDataset) data;
//        AddPropertyModifier mdfr = new AddPropertyModifier();
//        mdfr.setPropertiesToAdd(properties);
//        mdfr.transform(data);
//
//        // Run model
//        for (String prop : properties) {
//            BaseModel model = models.get(prop);
//            if (model == null) {
//                throw new Exception("No model for property: " + prop);
//            }
//            dataptr.setTargetProperty(prop, true);
//            model.run(data);
//        }
//
//        // Filter the entries
//        EntryRankerFilter filter = new EntryRankerFilter();
//        filter.setNumberToFilter(number);
//        filter.setExclude(false);
//        filter.setRanker(ranker);
//        filter.train(data);
//        filter.filter(data);
//
//        // Print out the results
//        int[] ranking = ranker.rankEntries(data);
//        for (int i : ranking) {
//            MultiPropertyEntry entry = (MultiPropertyEntry) data.getEntry(i);
//            String toAdd = entry.toHTMLString();
//            for (String prop : properties) {
//                int index = dataptr.getPropertyIndex(prop);
//                toAdd += String.format("\t%.4f", entry.getPredictedProperty(index));
//            }
//            output.add(toAdd);
//        }
//        return output;
//    }

    
}
