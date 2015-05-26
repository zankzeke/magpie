struct Entry {
    1: string name
    2: map<string,double> measured_properties = {}
    3: map<string,double> predicted_properties = {}
}

/**
 * Holds all known information about a model
 * 
 * Known properties:
 *  property  : Property being model
 *  units     : Units of prediction
 *  training  : Description of training set
 *  author    : Name/contact info of author
 *  citation  : Citation information of the model
 *  notes     : Short description of model
 *  dataType  : Type of data expected, defined by name of Magpie Dataset type
 *  modelType : Type of model
 */
struct ModelInfo {
    1: string property
    2: string units
    3: string author
    4: string training
    5: string citation
    6: string notes
    7: string dataType
    8: string modelType
}

service MagpieServer {

    /**
     * Get information about available models
     * @return Map of model name to model info 
     */
    map<string,ModelInfo> getModelInformation()

    /**
     * Compute the properties of each entry in a list. Returns results formatted as string
     * in a human-readable format.
     * @param entries [in] List of entries to be evaluated
     * @param props [in] Names of properties to evaluate
     * @return Entry objects with property measurements
     */
    list<Entry> evaluateProperties(1:list<Entry> entries, 2:list<string> props)
	
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
	
    /**
     * Search for optimal materials based on a multiple objectives in a 
     * user-defined space. Combines multiple objective functions using
     * the AdaptiveScalarizingEntryRanker. 
     *
     * Individual objective functions are defined in the same way as in the
     * single objective search.
     *
     * Relevant Documentation Pages:
     *
     * ./javadoc/magpie/optimization/rankers/AdaptiveScalarizingEntryRanker.html
     *
     * @param p [in] Tradeoff Parameter
     * @param objs [in] Objective functions
     * @param gen_method [in] Definition of search space
     * @param to_list [in] Number of top candidates to return
     * @return List of the top-performing entries
     */
    list<Entry> searchMultiObjective(1:double p, 2:list<string> objs, 
        3:string gen_method, 4:i32 to_list)

}
