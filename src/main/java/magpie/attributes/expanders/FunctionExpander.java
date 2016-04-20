package magpie.attributes.expanders;

import java.net.URL;
import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.ParsedFunction;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Generates new attributes that are arbitrary functions of other attributes. To 
 * define a function used to generate new attributes, simply write out a 
 * expression with the variables surrounded by ${}'s (e.g., (#{x} + #{y}/#{x})).
 * This expander will substitute the all possible combinations of attributes
 * for each of those variables.
 * 
 * <p>The idea for this class was based on work by 
 * <a href="http://link.aps.org/doi/10.1103/PhysRevLett.114.105503">
 * L. Ghiringhelli <i>et al.</i></a>
 * 
 * <p><usage><b>Usage</b>: "&lt;function #1&gt;" ["&lt;function #2&gt;] [&lt;...&gt;]
 * <br><pr><i>function</i>: Function describing a new combination of attributes.
 * <br>Functions should follow the syntax outlined in the Javadoc</usage>
 * 
 * @author Logan Ward
 * @see ParsedFunction
 */
public class FunctionExpander extends BaseAttributeExpander implements Citable {
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
    synchronized public void expand(Dataset Data) {
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
                    newName = newName.replace("#{" + variableNames.get(i) + "}",
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
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String,Citation>> output = new LinkedList<>();
        Citation citation = new Citation(this.getClass(),
                "Article",
                new String[]{"L. Ghiringhelli", "et al."},
                "Big Data of Materials Science: Critical Role of the Descriptor",
                "http://link.aps.org/doi/10.1103/PhysRevLett.114.105503",
                null
            );
        output.add(new ImmutablePair<>("Introduced idea of combining attributes with"
                + " simple functions.", citation));
        return output;
    }
}
