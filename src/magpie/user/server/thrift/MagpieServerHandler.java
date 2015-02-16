package magpie.user.server.thrift;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.regression.AbstractRegressionModel;
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
            results.clear();
            List<String> temp = new LinkedList<>();
			temp.add("Failure during evaluation: " + e.getLocalizedMessage());
            results.add(temp);
            return results;
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
}
