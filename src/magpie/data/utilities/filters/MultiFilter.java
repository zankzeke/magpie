
package magpie.data.utilities.filters;

import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * Filter that incorporates multiple other filters. User supplies multiple filters, which
 *  are processed sequentially. For each filter:
 * 
 * <ol>
 * <li>Any entries that did not pass previous filter are ignored
 * <li>Label operation is calculated to determine which (of the remaining) entries pass this filter
 * <li>If the filter is set to exclude those entries, each result from 1 is logically negated
 * <li>For the entries that passed previous filter, results are updated
 * </ol>
 * 
 * <usage><p><b>Usage</b>: -filter &lt;include|exclude&gt; &lt;name&gt; &lt;options...&gt; [-filter &lt;...&gt;]
 * <br><pr><i>include|exclude</i>: Whether to include or exclude entries that pass this filter
 * <br><pr><i>name</i>: Name of filtering method
 * <br><pr><i>options</i>: Options for that filter
 * <br>You can add as many filters as you want, just put "-filter" between each
 * definition. Syntax is the same as for the first filter. Filters are executed
 * sequentially.</usage>
 * 
 * @author Logan Ward
 */
public class MultiFilter extends BaseDatasetFilter {
	/** List of filters currently in use */
	protected List<BaseDatasetFilter> Filters = new LinkedList<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int pos = 0;
        while (pos < Options.size()) {
            boolean toInclude;
            String filterName;
            List<Object> filterOptions = new LinkedList<>();
            try {
                if (! Options.get(pos++).toString().equalsIgnoreCase("-filter")) {
                    throw new Exception();
                }
                filterName = Options.get(pos++).toString().toLowerCase(); // temp
                if (filterName.startsWith("in")) {
                    toInclude = true;
                } else if (filterName.startsWith("ex")) {
                    toInclude = false;
                } else {
                    throw new Exception();
                }
                filterName = Options.get(pos++).toString();
				while (pos < Options.size() && 
						! Options.get(pos).toString().equals("-filter")) {
					filterOptions.add(Options.get(pos++));
				}
            } catch (Exception e) {
                throw new Exception(printUsage());
            }
            
            // Instantiate the filter
            BaseDatasetFilter newFilter;
            try {
                newFilter = (BaseDatasetFilter) CommandHandler.instantiateClass(
                    	"data.utilities.filters." + filterName, filterOptions);
            } catch (Exception e) {
                throw new Exception(filterName + "-" + e.getLocalizedMessage());
            }
			newFilter.setExclude(! toInclude);
			addFilter(newFilter);
        }
    }

	@Override
	public String printUsage() {
		return "Usage:  -filter <include|exclude> <name> <options...> [-filter <...>]";
	}
	
	/**
	 * Clear list of filters.
	 */
	public void clearFilters() {
		Filters.clear();
	}
	
	/**
	 * Append filter to the current list of filters in use
	 * @param filter Filter to be added
	 */
	public void addFilter(BaseDatasetFilter filter) {
		Filters.add(filter);
	}

	@Override
	public void train(Dataset TrainingSet) {
		for (BaseDatasetFilter filter : Filters) {
			filter.train(TrainingSet);
		}
	}

	@Override
	protected boolean[] label(Dataset D) {
		boolean[] output = Filters.get(0).label(D);
		if (Filters.get(0).toExclude()) {
			negate(output);
		}
		for (BaseDatasetFilter filter : Filters.subList(1, Filters.size())) {
			boolean[] newLabels = filter.label(D);
			if (filter.toExclude()) {
				negate(newLabels);
			}
			// Only for entries that passed previous filter, compute whether they passed
			//  this one
			for (int i=0; i<output.length; i++) {
				if (output[i]) {
					output[i] = newLabels[i];
				}
			}
		}
		return output;
	}
	
	/**
	 * Negates each entry in an array of truth values.
	 * @param list List to be negated
	 */
	private void negate(boolean[] list) {
		for (int i=0; i<list.length; i++) {
			list[i] = ! list[i];
		}
	}
}
