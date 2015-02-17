struct Entry {
	1: string name
	2: map<string,double> measured_properties = {}
}

service MagpieServer {
    /**
     * Compute the properties of each entry in a list. Returns results formatted as string
     * in a human-readable format.
	 * @param entries [in] List of entries to be evaluated
     * @param props [in] Names of properties to evaluate
     * @return List of each property for each entry (in string format)
     */
	list<list<string>> evaluateProperties(1:list<Entry> entries, 2:list<string> props)
	
	/**
	 * Search for optimal materials based on a single objective in a 
	 * user-defined space
	 *
	 *      _How to Define Objective Function_
	 *
	 * The first word in the objective function input should be the name of the 
	 * property being optimized, followed by whether to minimize or maximize the
	 * objective function, then the name of EntryRanker, and (finally) its options.
	 * 
	 * Summary: <property> <minimize|maximize> <EntryRanker method> <options...>
	 *
	 * Example: To find a material with a band gap close to 1.3 eV
	 *     bandgap minimize TargetEntryRanker 1.3
	 *
	 * Relevant Document Pages:
	 *
	 * ./javadoc/magpie/optimization/rankers/package-summary.html
	 *
	 *      _How to Define Search Space_
	 *
	 * Search spaces are created using EntryGenerator classes. The first
	 *  in the input string is the name of the generator class, which
	 * is followed by any options for the generator
	 *
	 * Summary: <EntryGenerator method> <options...>
	 *
	 * Example: 5 points on each binary containing either Al, Ni, or Zr
	 *     PhaseDiagramCompositionEntryGenerator 2 -alloy 0.2 Al Ni Zr
	 *
	 * Relevent Documentation Pages:
	 *
	 * ./javadoc/magpie/data/utilities/generators/package-summary.html
	 *
	 * @param obj [in] Objective function
	 * @param gen_method [in] Definition of search space
	 * @param to_list [in] Number of top candidates to return
	 * @return List of the top-performing entries
	 */
	list<Entry> searchSingleObjective(1:string obj, 2:string gen_method, 3:i32 to_list)
	
}
