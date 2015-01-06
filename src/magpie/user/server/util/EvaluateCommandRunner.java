
package magpie.user.server.util;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.regression.AbstractRegressionModel;
import magpie.user.server.MagpieServer;

/**
 * Runs the evaluate command. See {@linkplain MagpieServer} for details.
 * @author Logan Ward
 */
abstract public class EvaluateCommandRunner {
    /**
	 * Evaluate the properties of a user-provided list of entries.
	 * @param command Command to be parsed (see {@linkplain MagpieServer})
     * @param models Models to be used
     * @param DatasetTemplate Dataset template
	 * @return Output to send to client
	 */
	static public List<String> runEvaluation(List<String> command, 
            Map<String,BaseModel> models, Dataset DatasetTemplate) {
		// Get the list of properties to be evaluated.
		String[] topCommand = command.get(0).toLowerCase().split("\\s+");
		String[] properties = Arrays.copyOfRange(topCommand, 1, topCommand.length);
		
		// Get names of entries to be be run
		List<String> entries = new LinkedList<>();
		for (String entry : command.subList(1, command.size())) {
			if (entry.isEmpty()) continue;
			entries.add(entry);
		}
		
		// Provide location to store results for each entry
		List<List<String>> results = new LinkedList();
		for (String entry : entries) {
			List<String> toAdd = new LinkedList<>();
			results.add(toAdd);
		}
		
		// Store entries in dataset, create lookup array 
		Dataset dataset = DatasetTemplate.emptyClone();
		Map<Integer, Integer> entryToResult = new TreeMap<>();
		List<Integer> failedList = new LinkedList<>();
		for (int i=0; i < entries.size(); i++) {
			String entry = entries.get(i);
			if (entry.isEmpty()) continue;
			try {
				BaseEntry parsed = dataset.addEntry(entry);
				entryToResult.put(dataset.NEntries()-1 , i);
			} catch (Exception e) {
				failedList.add(i);
			}
		}

		// Run the model and such
		List<String> output = new LinkedList<>();
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
            for (String prop : properties) {
                BaseModel model = models.get(prop);
                
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
            output.clear();
			output.add("Failure during evaluation: " + e.getLocalizedMessage());
            return output;
		}
		
		// Add unparseable entries
		for (Integer entry : failedList) {
			results.get(entry).add(entries.get(entry));
			for (int i=0; i < Math.max(1, properties.length); i++) {
				results.get(entry).add("NA");
			}
		}
		
		// Compile results
		for (List<String> toCollapse : results) {
			Iterator<String> iter = toCollapse.iterator();
			String toAdd = iter.next();
			while (iter.hasNext()) {
				toAdd += "\t" + iter.next();
			}
			output.add(toAdd);
		}
		
		return output;
	}
    
}
