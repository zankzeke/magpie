package magpie.attributes.selectors;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;
import magpie.utility.WekaUtility;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.core.Instances;
import weka.core.OptionHandler;

/**
 * Encapsulates Weka's ASSearch routines for attribute selection.
 * 
 * <usage><p><b>Usage</b>: [-eval &lt;eval method> [&lt;eval options...>]] [-search &lt;search method> [&lt;search options...>]]
 * <br><pr><i>eval method</i>: How to evaluate attributes. Name of a Weka ASEvalution class
 * <br><pr><i>eval options...</i>: Any options for the attribute evaluator.
 * <br><pr><i>search method</i>: How to search for optimal set. Name of a Weka ASSearch class
 * <br><pr><i>eval options...</i>: Any options for the searcher</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class WekaAttributeSelector extends BaseAttributeSelector {
    /** Weka class used to populate a list of attributes */
    protected ASSearch Searcher;
    /** Weka class that is used to rank subsets of attributes */
    protected ASEvaluation Evaluator;

    /** Generates a WekaAttributeSelector with default methods */
    public WekaAttributeSelector() {
       Searcher = getASSearch("BestFirst", null);
       Evaluator = getASEvaluator("CfsSubsetEval", null);
    }
    
    @Override
    public WekaAttributeSelector clone() {
        WekaAttributeSelector x = (WekaAttributeSelector) super.clone();
        try {
            x.Evaluator = ASEvaluation.makeCopies(Evaluator, 1)[0];
            x.Searcher = ASSearch.makeCopies(Searcher, 1)[0];
        } catch (Exception e) {
            throw new Error(e);
        }
        return x;
    }

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        if (Options == null || Options.length == 0) return;
        boolean set_searcher = false, set_evaluator = false;
        String SearcherType = "", SearcherOptions[] = null;
        String EvalType = "", EvalOptions[] = null;
        try {
            List<String> OptionPtr = Arrays.asList(Options);
            // Look to see which options are set 
            int searchStart = OptionPtr.indexOf("-search");
            int evalStart = OptionPtr.indexOf("-eval");
            
            // Get the evaluator options 
            if (evalStart != -1) {
                set_evaluator = true;
                EvalType = Options[evalStart + 1];
                int End = evalStart < searchStart ? searchStart : Options.length;
                EvalOptions = Arrays.copyOfRange(Options, evalStart + 2, End);
            }
            
            // Get the searcher options
            if (searchStart != -1) {
                set_searcher = true;
                SearcherType = Options[searchStart + 1];
                int End = searchStart < evalStart ? evalStart : Options.length;
                SearcherOptions = Arrays.copyOfRange(Options, searchStart + 2, End);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
        if (set_evaluator) {
            setEvaluator(EvalType, EvalOptions);
		}
        if (set_searcher) {
			setSearcher(SearcherType, SearcherOptions);
		}
            
    }

    @Override
    public String printUsage() {
        return "Usage: [-eval <SubsetEvaluator class> [<options...>]] " +
                    "[-search <ASSearch Class> [<options...>]";
    }

	/**
	 * Set the method used to evaluate subsets of entries.
	 * @param Name Name of ASEvaluator class
	 * @param Options Any options for that class
	 */
	public void setEvaluator(String Name, String[] Options) {
		this.Evaluator = getASEvaluator(Name, Options);
		trained = false;
	}

	/**
	 * Set the method used to search for optimal subsets.
	 * @param Name Name of ASSearch class
	 * @param Options Any options for that class
	 */
	public void setSearcher(String Name, String[] Options) {
		this.Searcher = getASSearch(Name, Options);
		trained = false;
	}
	
    /**
     * Generates a Weka ASSearch object, given names and options
     * @param Name Name of ASSearch (fully qualified)
     * @param Options Any options (can be null)
     * @return Newly instantiated object
     */
    static protected ASSearch getASSearch(String Name, String[] Options ) {
        ASSearch newObj;
        if (! Name.startsWith("weka.attributeSelection."))
            Name = "weka.attributeSelection." + Name;
        try {
            newObj = (ASSearch) Class.forName(Name).newInstance();
            if (newObj instanceof OptionHandler) {
                OptionHandler opt = (OptionHandler) newObj;
                opt.setOptions(Options);
            }
        } catch (Exception e) {
            throw new Error(e);
        }
        return newObj;
    }
    
    /**
     * Generates a Weka ASEvaluator object, given name and options
     * @param Name Name of ASEvaluator (fully qualified)
     * @param Options Any options (can be null)
     * @return Newly instantiated object
     */
    static protected ASEvaluation getASEvaluator(String Name, String[] Options ) {
        ASEvaluation newObj;
        if (! Name.startsWith("weka.attributeSelection."))
            Name = "weka.attributeSelection." + Name;
        try {
            newObj = (ASEvaluation) Class.forName(Name).newInstance();
            if (newObj instanceof OptionHandler) {
                OptionHandler opt = (OptionHandler) newObj;
                opt.setOptions(Options);
            }
        } catch (Exception e) {
            throw new Error(e);
        }
        return newObj;
    }
    
    @Override protected List<Integer> train_protected(Dataset data) {
        List<Integer> output = new LinkedList<>();
        try {
            // Transfer data to a Weka format
            Instances wekadata = data.transferToWeka(true, 
                    data.NClasses() > 1);
            Evaluator.buildEvaluator(wekadata);
            
            // Get the results
            int[] list = Searcher.search(Evaluator, wekadata);
            for (int i=0; i<list.length; i++) {
                output.add(list[i]);
            }
            data.restoreAttributes(wekadata);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = "Uses Weka to perform attribute selection.\n";
        
        // Print out the evaluator
        if (htmlFormat) {
            output += "<br>";
        }
        output += "Evaluator: " + Evaluator.getClass().getSimpleName();
        if (Evaluator instanceof OptionHandler) {
            for (String option : ((OptionHandler) Evaluator).getOptions()) {
                output += " " + option;
            }
        }
        output += "\n";
        
        // Print out the search method
        if (htmlFormat) {
            output += "<br>";
        }
        output += "Searcher: " + Searcher.getClass().getSimpleName();
        if (Searcher instanceof OptionHandler) {
            for (String option : ((OptionHandler) Searcher).getOptions()) {
                output += " " + option;
            }
        }
        
        return output;
    }
    
    
}
