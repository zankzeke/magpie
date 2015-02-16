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
	
}
