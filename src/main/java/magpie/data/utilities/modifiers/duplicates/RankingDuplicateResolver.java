package magpie.data.utilities.modifiers.duplicates;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyEntry;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.optimization.rankers.SimpleEntryRanker;
import magpie.user.CommandHandler;

/**
 * Use a ranking algorithm to determine which duplicate to pick. If there are multiple
 * entries with the best rank, the average is returned. Operates only on the 
 * measured class and/or property values
 * 
 * <usage><p><b>Usage</b>: &lt;maximize|minimize&gt; &lt;ranker&gt; [&lt;ranker options...&gt;]
 * <pr><br><i>maximize|minimize</i>: Whether to be highest or lowest entry based on ranker
 * <pr><br><i>ranker</i>: Name of {@linkplain BaseEntryRanker} to use
 * <pr><br><i>ranker options...</i>: Options for the ranker</usage>
 * @author Logan Ward
 */
public class RankingDuplicateResolver extends BaseDuplicateResolver {
    /** Ranker used to choose entries */
    BaseEntryRanker Ranker = new SimpleEntryRanker();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        boolean toMax;
        String rankerName;
        List<Object> rankerOptions;
        
        try {
            if (Options.get(0).toString().toLowerCase().startsWith("max")) {
                toMax = true;
            } else if (Options.get(0).toString().toLowerCase().startsWith("min")) {
                toMax = false;
            } else {
                throw new Exception();
            }
            rankerName = Options.get(1).toString();
            rankerOptions = Options.subList(2, Options.size());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Make the ranker
        BaseEntryRanker ranker = (BaseEntryRanker) 
                CommandHandler.instantiateClass("optimization.rankers."
                        + rankerName, rankerOptions);
        ranker.setMaximizeFunction(toMax);
        
        // Define options
        setRanker(ranker);
    }

    @Override
    public String printUsage() {
        return "Usage: <ranker> [<ranker options...>]";
    }

    /**
     * Set the ranker used to select between entries
     * @param ranker Desired ranker
     */
    public void setRanker(BaseEntryRanker ranker) {
        this.Ranker = ranker;
    }
    
    

    @Override
    protected BaseEntry resolveDuplicates(Dataset data, List<BaseEntry> entries) {
        // Prepare ranker
        Ranker.setUseMeasured(true);
        Ranker.train(data);
        
        // Pick some entries
        Dataset tempData = data.emptyClone();
        tempData.addEntries(entries);
        int[] results = Ranker.rankEntries(tempData);
        
        // Get the list of ones that are the best
        List<BaseEntry> bestEntries = new ArrayList<>(entries.size());
        double bestObjective = Ranker.objectiveFunction(entries.get(results[0]));
        for (BaseEntry entry : entries) {
            if (Ranker.objectiveFunction(entry) == bestObjective) {
                bestEntries.add(entry);
            }
        }
        
        // Return result
        if (bestEntries.size() == 1) {
            return bestEntries.get(0);
        } else {
            if (bestEntries.get(0) instanceof MultiPropertyEntry) {
                return AveragingDuplicateResolver.resolveMultipropertyEntry(data, entries);
            } else {
                return AveragingDuplicateResolver.resolveBaseEntry(data, entries);
            }
        }
    }
}
