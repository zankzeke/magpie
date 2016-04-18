package magpie.optimization.rankers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import magpie.user.CommandHandler;

/**
 * Shortcut for an {@linkplain BaseEntryRanker} to operate on a certain property,
 * rather than the default class. 
 * 
 * <usage><p><b>Usage</b>: &lt;property&gt; &lt;ranker&gt; [&lt;ranker options...&gt;]
 * <pr><br><i>property</i>: Name of property used for ranking
 * <pr><br><i>ranker</i>: Name of {@linkplain BaseEntryRanker} to use
 * <pr><br><i>ranker options...</i>: Options for the ranker</usage>
 * @author Logan Ward
 */
public class PropertyRanker extends BaseEntryRanker {
    /** Name of property used for ranking */
    protected String PropertyName;
    /** Index of property used for ranking */
    protected int PropertyIndex;
    /** Underlying ranker */
    BaseEntryRanker SubRanker = new SimpleEntryRanker();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String propName;
        String rankerName;
        List<Object> rankerOptions;
        
        try {
            propName = Options.get(0).toString();
            rankerName = Options.get(1).toString();
            rankerOptions = Options.subList(2, Options.size());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Make the ranker
        BaseEntryRanker ranker = (BaseEntryRanker) 
                CommandHandler.instantiateClass("optimization.rankers."
                        + rankerName, rankerOptions);
        
        // Define options
        setPropertyName(propName);
        setSubRanker(ranker);
    }

    @Override
    public String printUsage() {
        return "Usage: <property> <ranker> [<ranker options...>]";
    }

    /**
     * Set the ranker employed by this class.
     * @param subRanker Ranker used to rank by property values
     */
    public void setSubRanker(BaseEntryRanker subRanker) {
        this.SubRanker = subRanker;
        
        // Define the settings
        subRanker.setUseMeasured(isUsingMeasured());
        subRanker.setMaximizeFunction(isMaximizing());
    }

    /**
     * Set the name of property used for ranking
     * @param name Name of property
     */
    public void setPropertyName(String name) {
        this.PropertyName = name;
        PropertyIndex = -1; // Reset the index
    }

    @Override
    public void setUseMeasured(boolean useMeasured) {
        super.setUseMeasured(useMeasured); 
        SubRanker.setUseMeasured(useMeasured);
    }

    @Override
    public void setMaximizeFunction(boolean toMaximize) {
        super.setMaximizeFunction(toMaximize); 
        SubRanker.setMaximizeFunction(toMaximize);
    }

    @Override
    public void train(Dataset data) {
        // Get the property index
        MultiPropertyDataset dataPtr = (MultiPropertyDataset) data;
        PropertyIndex = dataPtr.getPropertyIndex(PropertyName);
        if (PropertyIndex == -1) {
            throw new RuntimeException("No such property: " + PropertyName);
        }
     
        // Change target property
        int ot = dataPtr.getTargetPropertyIndex();
        dataPtr.setTargetProperty(PropertyIndex, true);
        
        // Train
        SubRanker.train(data);
        
        // Change it back
        dataPtr.setTargetProperty(ot, true);
    }

    @Override
    public double objectiveFunction(BaseEntry entry) {
        // Check that we've trained
        if (PropertyIndex == -1) {
            throw new RuntimeException("Ranker not yet trained");
        }
        
        // Set target property
        MultiPropertyEntry entryPtr = (MultiPropertyEntry) entry;
        int ot = entryPtr.getTargetProperty();
        entryPtr.setTargetProperty(PropertyIndex);
        
        // Get the result
        double output = SubRanker.objectiveFunction(entry);
        
        // Reset property
        entryPtr.setTargetProperty(ot);
        
        return output;
    }
}
