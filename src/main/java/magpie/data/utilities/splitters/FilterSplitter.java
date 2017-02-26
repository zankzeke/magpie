package magpie.data.utilities.splitters;

import magpie.data.Dataset;
import magpie.data.utilities.filters.BaseDatasetFilter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Split entries based on whether they pass a {@linkplain BaseDatasetFilter}. Entries that pass are placed in split 0
 * and those that do not pass are placed in 1.
 *
 * <usage><p><b>Usage</b>: $lt;filter&gt;</p>
 * <pr><br><i>filter</i>: {@linkplain BaseDatasetFilter} used to partition data</usage>
 *
 * @author Logan Ward
 */
public class FilterSplitter extends BaseDatasetSplitter {
    /** Filter used for splitting the data */
    protected BaseDatasetFilter Filter;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        BaseDatasetFilter filter;
        try {
            if (Options.size() != 1) {
                throw new IllegalArgumentException();
            }
            filter = (BaseDatasetFilter) Options.get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }

        // Set the filter
        setFilter(filter);
    }

    @Override
    public String printUsage() {
        return "Usage: $<filter>";
    }

    /**
     * Set the filter used to partition data
     * @param filter Desired filter
     */
    public void setFilter(BaseDatasetFilter filter) {
        Filter = filter;
    }

    @Override
    public void train(Dataset TrainingSet) {
        Filter.train(TrainingSet);
    }

    @Override
    public int[] label(Dataset D) {
        int[] output = new int[D.NEntries()];
        boolean[] filterLabels = Filter.label(D);
        for (int i=0; i<filterLabels.length; i++) {
            output[i] = filterLabels[i] ? 0 : 1;
        }
        return output;
    }

    @Override
    public List<String> getSplitNames() {
        return Arrays.asList(new String[]{"PassesFilter", "DidNotPassFilter"});
    }

    @Override
    protected List<String> getSplitterDetails(boolean htmlFormat) {
        List<String> output = new LinkedList<>();

        output.add("Filter: " + Filter.getClass().getName());

        return output;
    }
}
