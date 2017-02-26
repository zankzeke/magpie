package magpie.data.utilities.filters;

import magpie.data.Dataset;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.user.CommandHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Filter entries based on ranking. Lets the user pick the number of entries to keep,
 * whether to select the best or lest fit, use the predicted or measured class, and then
 * the overall method and its options. Consequently, the "Usage" is pretty messy.
 * 
 * <usage><p><b>Usage</b>: &lt;number> &lt;maximum|minimum> &lt;predicted|measured> &lt;method> [&lt;options...>]
 * <br><pr><i>number</i>: Number of entries that pass this filter
 * <br><pr><i>maximum|minimum</i>: Whether entries with a max/minimum value pass the filter
 * <br><pr><i>method</i>: Objective function used to rank entries. Name of an {@link BaseEntryRanker} ("?" for available methods)
 * <br><pr><i>options...</i>: Any options for the objective function</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class EntryRankerFilter extends BaseDatasetFilter {
    /** Number of entries to filter */
    private int NumberToFilter = 200;
    /** Entry ranking method */
    private BaseEntryRanker Ranker;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String Options[] = CommandHandler.convertCommandToString(OptionsObj);
        String RankingMethod;
        List<Object> MethodOptions;
		boolean measured;
		boolean maximize;
        try {
            NumberToFilter = Integer.parseInt(Options[0]);
            if (Options[1].toLowerCase().contains("max"))
                maximize = true;
            else if (Options[1].toLowerCase().contains("min"))
                maximize = false;
            else 
                throw new Exception();
                    
            if (Options[2].equalsIgnoreCase("measured"))
                measured = true;
            else if (Options[2].equalsIgnoreCase("predicted"))
                measured = false;
            else 
                throw new Exception();
            
            RankingMethod = Options[3];
            MethodOptions = OptionsObj.subList(4, Options.length);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Make the BaseEntryRanker
        Ranker = (BaseEntryRanker) CommandHandler.instantiateClass("optimization.rankers." + RankingMethod, MethodOptions);
        Ranker.setMaximizeFunction(maximize);
        Ranker.setUseMeasured(measured);
    }

    @Override
    public String printUsage() {
        return "Usage: <number> <maximum|minimum> <predicted|measured> <EntryRanker Method> <Method Options...>";
    }

    /**
     * Define how many entries will pass this filter
     * @param NumberToFilter Desired number
     */
    public void setNumberToFilter(int NumberToFilter) {
        this.NumberToFilter = NumberToFilter;
    }

	/**
	 * Define the ranker used to filter entries. 
	 * @param Ranker Desired ranker
	 */
	public void setRanker(BaseEntryRanker Ranker) {
		this.Ranker = Ranker;
	}
	
	

    @Override
    public void train(Dataset TrainingSet) {
        Ranker.train(TrainingSet);
    }

    @Override
    public boolean[] label(Dataset D) {
        if (Ranker.isUsingMeasured() && (! D.getEntry(0).hasMeasurement()))
            throw new RuntimeException("Missing measured class.");
        if (! Ranker.isUsingMeasured() && (! D.getEntry(0).hasPrediction()))
            throw new RuntimeException("Missing predicted class.");
        
        boolean[] passes = new boolean[D.NEntries()];
        Arrays.fill(passes, false);
        int[] ranks = Ranker.rankEntries(D);
        for (int i=0; i<Math.min(NumberToFilter, D.NEntries()); i++) passes[ranks[i]] = true;
        return passes;
    }

    
}
