struct entry {
	1: string name
	2: map<string,double> measured_properties = {}
}

exception MagpieServerException {
	1: string message
}

service MagpieServer {
	/**
         * Compute the properties of each entry in a list
         * @param entries [in] List of entries to be evaluated
         * @param props [in] Names of properties to evaluate
	 * @return List of each property for each entry
	 */
	list<list<double>> evaluateProperties(1:list<entry> entries, 2:list<string> props) throws (1:MagpieServerException ouch)
}
