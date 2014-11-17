/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.user.server.util;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import magpie.data.utilities.filters.EntryRankerFilter;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.data.utilities.modifiers.AddPropertyModifier;
import magpie.models.BaseModel;
import magpie.optimization.rankers.*;
import magpie.user.CommandHandler;
import magpie.user.server.MagpieServer;

/**
 * Runs the search command. See {@linkplain MagpieServer} for details.
 * @author Logan Ward
 */
abstract public class SearchCommandRunner {
    /**
	 * Evaluate the properties of a user-provided list of entries.
	 * @param command Command to be parsed (see {@linkplain MagpieServer})
     * @param models Models to be used
     * @param datasetTemaplate Dataset template, used to calculate attributes
	 * @return Output to send to client
	 */
	static public List<String> runSearch(List<String> command, 
            Map<String,BaseModel> models, Dataset datasetTemaplate) {
		// Initialize output
		List<String> output = new LinkedList<>();
		
		// Get the entry generator
		BaseEntryGenerator generator;
		try {
			generator = getGenerator(command.get(0));
		} catch (Exception e) {
			output.add("Entry generator creation failed: " + e.getMessage());
			return output;
		}
		
		// Get entries to be run
		Dataset dataset = datasetTemaplate.emptyClone();
		generator.addEntriesToDataset(dataset);
		if (dataset.NEntries() > MagpieServer.MaxEntryEvaluations) {
			output.add("Exceeded maximum entry count: " + dataset.NEntries() +
					" > " + MagpieServer.MaxEntryEvaluations);
			return output;
		}
		try {
			dataset.generateAttributes();
		} catch (Exception e) {
			output.add("Attribute generation failed: " + e.getLocalizedMessage());
		}
		
		// Get/run objective function
		String action = command.get(1).split("\\s+")[0].toLowerCase();
		try {
			switch (action) {
				case "objective":
					output = runSingleObjective(command, dataset, models);
					break;
				case "multi":
					output = runMultiObjectiveScalarized(command, dataset, models);
					break;
				default: 
					throw new Exception("Search method not recognized: " + action);
			}
		} catch (Exception e) {
			output.add(e.getLocalizedMessage());
			return output;
		}
		
		return output;
	}

	/**
	 * Given the first line of user command, get the entry generator.
	 * @param command First line of command sent to thread
	 * @return {@linkplain BaseEntryGenerator}, as specified by command
	 * @throws Exception 
	 */
	protected static BaseEntryGenerator getGenerator(String command) throws Exception {
		// Get the search space
		String[] topCommand = command.split("\\s+");
		String method = topCommand[1];
		List<Object> options = new LinkedList<>();
		for (int i=2; i<topCommand.length; i++) options.add(topCommand[i]);
		BaseEntryGenerator generator = (BaseEntryGenerator) CommandHandler.instantiateClass(
				"data.utilities.generators." + method, options);
		return generator;
	}
	
	/**
	 * Evaluate a single-objective entry ranker
	 * @param command Command describing objective
	 * @param data Dataset to be evaluated
	 * @param models Models to be used
	 * @return Table of results of search (i.e. entry name) 
	 */
	protected static List<String> runSingleObjective(List<String> command, Dataset data,
			Map<String,BaseModel> models) throws Exception {
		List<String> output = new LinkedList<>();
		
		// Parse the objective command
		String[] words = command.get(1).split("\\s+");
		String property = words[1];
		BaseEntryRanker ranker;
		ranker = getObjectiveFunction(Arrays.copyOfRange(words, 1, words.length));
		
		// Get the number of entries
		int number = getNumberToReport(command);
		
		// Execute the search
		BaseModel model = models.get(property);
		if (model == null) {
			throw new Exception("No model for property: " + property);
		}
		model.run(data);
		
		// Filter the entries
		EntryRankerFilter filter = new EntryRankerFilter();
		filter.setNumberToFilter(number);
		filter.setExclude(false);
		filter.setRanker(ranker);
		filter.train(data);
		filter.filter(data);
		
		// Print out the results
		int[] ranking = ranker.rankEntries(data);
		for (int i : ranking) {
			BaseEntry entry = data.getEntry(i);
			output.add(String.format("%s\t%.4f", entry.toHTMLString(), 
				ranker instanceof ClassProbabilityRanker ? 
				ranker.objectiveFunction(entry) * 100 :
				entry.getPredictedClass()));
		}
		return output;
	}

	/**
	 * Get an entry ranker for a single property.
	 * 
	 * <p>Format: &lt;property name&lt; 
	 * &lt;minimize|maximize|target&gt; [&lt;target value&gt;]
	 * @param words Words describing objective function
	 * @return BaseEntryRanker describing this objective function
	 * @throws Exception 
	 */
	protected static BaseEntryRanker getObjectiveFunction(String[] words) throws Exception {
		BaseEntryRanker ranker;
		switch (words[1].toLowerCase()) {
			case "minimize":
				ranker = new SimpleEntryRanker();
				ranker.setMaximizeFunction(false);
				break;
			case "maximize":
				ranker = new SimpleEntryRanker();
				ranker.setMaximizeFunction(true);
				break;
			case "target":
				try {
					double target = Double.parseDouble(words[2]);
					ranker = new TargetEntryRanker(target);
                    ranker.setMaximizeFunction(false);
				} catch (Exception e) {
					throw new Exception("Expected number as third arguement of objective");
				}
				ranker.setMaximizeFunction(false);
				break;
			case "class":
				try {
					String className = words[2];
					ranker = new ClassProbabilityRanker(className);
					ranker.setMaximizeFunction(true);
				} catch (Exception e) {
					throw new Exception("Expected class name as third arguement of objective");
		                }
				break;
			default:
				throw new Exception("Objective not recognized: " + words[2]);
		}
		ranker.setUseMeasured(false);
		return ranker;
	}
	
	/**
	 * Evaluate a multi-objective entry ranker that uses adaptive scalarization.
	 * @param command Command describing objective
	 * @param data Dataset to be evaluated
	 * @param models Models to be used
	 * @return Table of results of search (i.e. entry name) 
	 */
	protected static List<String> runMultiObjectiveScalarized(List<String> command, Dataset data,
			Map<String,BaseModel> models) throws Exception {
		List<String> output = new LinkedList<>();
		
		// Parse the objective command
		String[] words = command.get(1).split("\\s+");
		double p;
		try {
			p = Double.parseDouble(words[1]);
		} catch (Exception e) {
			throw new Exception("Expected second word of multi to be the P value");
		}
		
		// Get the entry ranker
		AdaptiveScalarizingEntryRanker ranker = new AdaptiveScalarizingEntryRanker();
		ranker.setP(p);
		ranker.setUseMeasured(false);
		
		// Get all of the propeties
		List<String> properties = new LinkedList<>();
		for (String line : command.subList(2, command.size() - 1)) {
			words = line.split("\\s+");
			try {
				String prop = words[0];
				BaseEntryRanker obj = getObjectiveFunction(words);
				ranker.addObjectiveFunction(prop, obj);
				properties.add(prop);
			} catch (Exception e) {
				throw new Exception("Failed parsing objective on line: " + line);
			}
		}
		
		// Get the number of entries
		int number = getNumberToReport(command);
		
		// Add properties to dataset
		if (! (data instanceof MultiPropertyDataset)) {
			throw new Exception("Dataset template is not a MultiPropertyDataset");
		}
		MultiPropertyDataset dataptr = (MultiPropertyDataset) data;
		AddPropertyModifier mdfr = new AddPropertyModifier();
		mdfr.setPropertiesToAdd(properties);
		mdfr.transform(data);
		
		// Run model
		for (String prop : properties) {
			BaseModel model = models.get(prop);
			if (model == null) {
				throw new Exception("No model for property: " + prop);
			}
			dataptr.setTargetProperty(prop, true);
			model.run(data);
		}
		
		// Filter the entries
		EntryRankerFilter filter = new EntryRankerFilter();
		filter.setNumberToFilter(number);
		filter.setExclude(false);
		filter.setRanker(ranker);
		filter.train(data);
		filter.filter(data);
		
		// Print out the results
		int[] ranking = ranker.rankEntries(data);
		for (int i : ranking) {
			MultiPropertyEntry entry = (MultiPropertyEntry) data.getEntry(i);
			String toAdd = entry.toHTMLString();
			for (String prop : properties) {
				int index = dataptr.getPropertyIndex(prop);
				toAdd += String.format("\t%.4f", entry.getPredictedProperty(index));
			}
			output.add(toAdd);
		}
		return output;
	}

	/**
	 * Parse the number of entries to report
	 * @param command Command being executed
     * @return Number of entries to report
	 * @throws NumberFormatException 
	 */
	protected static int getNumberToReport(List<String> command) throws Exception {
		try {
			return Integer.parseInt(command.get(command.size()-1).split("\\s+")[1]);
		} catch (Exception e) {
			throw new Exception("Expected last line to contain number to be reported");
		}
	}
    
}
