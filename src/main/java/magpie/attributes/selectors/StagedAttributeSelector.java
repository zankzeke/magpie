package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.List;
import magpie.data.Dataset;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Selector that uses multiple selection strategies in series. Runs subsequent
 * attribute selectors on the set of attributes selected by the previous selector.
 * 
 * <usage><p><b>Usage</b>: $&lt;selectors...&gt;
 * <br><pr><i>selectors</i>: List of {@linkplain BaseAttributeSelector} in
 * order in which they will be run</li></usage>
 * 
 * @author Logan Ward
 */
public class StagedAttributeSelector extends BaseAttributeSelector 
        implements Citable {
    /** 
     * List of attribute selectors to use for selection. [0] is the first
     * selector to be run.
     */
    protected List<BaseAttributeSelector> Selectors = new ArrayList<>();

    @Override
    public BaseAttributeSelector clone() {
        StagedAttributeSelector x = (StagedAttributeSelector) super.clone();
        
        x.Selectors = new ArrayList<>(Selectors.size());
        for (BaseAttributeSelector sel : Selectors) {
            x.Selectors.add(sel.clone());
        }
        
        return x;
    }
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        clearSelectorList();
        
        try {
            for (Object opt : Options) {
                addSelector((BaseAttributeSelector) opt);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<selectors...>";
    }
    
    /**
     * Clear the list of selectors
     */
    public void clearSelectorList() {
        Selectors.clear();
    }
    
    /**
     * Set the list of selectors used by this tool
     * @param selectors List of selectors in desired order
     */
    public void setSelectors(List<BaseAttributeSelector> selectors) {
        Selectors = new ArrayList<>(selectors);
    }
    
    /**
     * Add new selector to list, will be run last
     * @param selector Selector to add
     */
    public void addSelector(BaseAttributeSelector selector) {
        Selectors.add(selector);
    }

    /**
     * Get the current list of selectors
     * @return List of selectors
     */
    public List<BaseAttributeSelector> getSelectors() {
        return new ArrayList<>(Selectors);
    }

    @Override
    protected List<Integer> train_protected(Dataset data) {
        Dataset localData = data.clone();
        
        // Run them sequentially
        for (BaseAttributeSelector sel : Selectors) {
            sel.train(localData);
            sel.run(localData);
        }
        
        // Get the selections from the last selector
        return Selectors.get(Selectors.size() - 1).getSelections();
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = "Run several selectors sequentially. Each selector"
                + " is run on the subset selected by the previous:\n";
        
        if (htmlFormat) {
            output += "<ol>\n";
        }
        
        for (int i=0; i<Selectors.size(); i++) {
            output += (htmlFormat ? "<li>" : String.format("%d. ", i+1))
                    + Selectors.get(i).printDescription(htmlFormat);
            if (htmlFormat) {
                output += "</li>";
            }
            output += "\n";
        }
        
        if (htmlFormat) {
            output += "</ol>";
        }
        
        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = new ArrayList<>();
        
        for (BaseAttributeSelector sel : Selectors) {
            if (sel instanceof Citable) {
                output.addAll(((Citable) sel).getCitations());
            }
        }
        
        return output;
    }
}
