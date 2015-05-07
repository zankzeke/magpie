package magpie.attributes.expansion;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.ParsedFunction;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Generates new attributes that are arbitrary functions of other attributes. To 
 * define a function used to generate new attributes, simply write out a 
 * expression with the variables surrounded by ${}'s (e.g., (${x} + ${y}/${x})).
 * This expander will substitute the all possible combinations of attributes
 * for each of those variables.
 * 
 * <p>The idea for this class was based on work by L. Ghiringhelli <i>et al.</i> 
 * posted to ArXiv. If you're reading this and the proper citation is not 
 * yet in this documentation, please contact Logan Ward.
 * 
 * <p><usage><b>Usage</b>: "&lt;function #1&gt;" ["&lt;function #2&gt;] [&lt;...&gt;]
 * <br><pr><i>function</i>: Function describing a new combination of attributes.
 * <br>Functions should follow the syntax outlined in the Javadoc</usage>
 * 
 * @author Logan Ward
 * @see ParsedFunction
 */
public class FunctionExpander extends BaseAttributeExpander {
    /** List of functions */
    final private List<ParsedFunction> Functions = new LinkedList<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        for (Object obj : Options) {
            addNewFunction(obj.toString());
        }
    }
    
    @Override
    public String printUsage() {
        return "Usage: \"<function 1>\" [\"<function 2\">] <...>";
    }
    
    /**
     * Clear list of functions used to generate new attributes.
     */
    public void clearFunctionList() {
        Functions.clear();
    }

    /**
     * Add a new function to be used for expansion
     * @param function Function to be parsed
     * @throws Exception 
     */
    public void addNewFunction(String function) throws Exception {
        ParsedFunction newFunc = new ParsedFunction(function);
        Functions.add(newFunc);
    }

    @Override
    public void expand(Dataset Data) {
        // Get list of attribute names
        String[] oldNames = Data.getAttributeNames();
        
        // Generate names
        List<String> newNames = new LinkedList(
                Arrays.asList(Data.getAttributeNames()));
        for (ParsedFunction f : Functions) {
            // Get information about this function
            List<String> variableNames = f.getVariableNames();
            String inputString = f.getInput();
            
            // Generate combinations
            Combinations combins = new Combinations(oldNames.length, 
                    f.numVariables());
            
            for (int[] comb : combins) {
                // Get the new attribute name for this combination
                String newName = inputString;
                for (int i=0; i<variableNames.size(); i++) {
                    newName = inputString.replace("${" + variableNames.get(i) + "}",
                            oldNames[comb[i]]);
                }
                newNames.add(newName);
            }
        }
        Data.setAttributeNames(newNames);
        
        // Compute attributes
        for (BaseEntry e : Data.getEntries()) {
            for (ParsedFunction f : Functions) {
                // Generate combinations
                Combinations combins = new Combinations(oldNames.length,
                        f.numVariables());
                int nCombins = (int) CombinatoricsUtils.binomialCoefficient(
                        oldNames.length, f.numVariables());

                // Compute the values for each etnry
                double[] newVals = new double[nCombins];
                double[] attrs = new double[f.numVariables()];
                int count = 0;
                for (int[] comb : combins) {
                    // Get the entry
                    for (int a = 0; a < f.numVariables(); a++) {
                        attrs[a] = e.getAttribute(comb[a]);
                    }

                    // Evaluate function
                    newVals[count++] = f.value(attrs);
                }
                
                // Add the new attribute
                e.addAttributes(newVals);
            }

        }

        // Call garbage collection
        for (BaseEntry e : Data.getEntries()) {
            e.reduceMemoryFootprint();
        }
    }
    
}
