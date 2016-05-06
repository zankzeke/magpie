package magpie.attributes.expanders;

import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.CartesianSumGenerator;
import magpie.utility.ParsedFunction;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Generates new attributes that are arbitrary functions of other attributes.
 * The idea for this class was based on work by 
 * <a href="http://link.aps.org/doi/10.1103/PhysRevLett.114.105503">
 * L. Ghiringhelli <i>et al.</i></a>
 * 
 * <p> To define a function used to generate new attributes, simply write out a 
 * expression with the variables surrounded by #{}'s (e.g., (#{x} + #{y}/#{x})).
 * This expander will substitute the all possible combinations of attributes
 * for each of those variables. If you would like to generate all permutations of
 * attributes in a specific formula, write out all possible orders of variables
 * into that formula (ex: both #{x}/#{y} and #{y}/#{x})
 * 
 * <p>Variables names that start with "r:" can be used to allow only certain
 * attributes. These variables are expected to be of the format "r:[name],[regex]"
 * where [name] is a unique name and [regex] is the 
 * For example, "r:1,*" would match all attributes and "r:2,a.*z$" would match
 * only attributes that start with a and end with z. The formula "#{r:x,*}*#{r:y,time}"
 * would multiple all attributes with the attribute named "time". Note, this will
 * generate all permutations of attributes for each variable.
 * 
 * <p><b>Examples</b>
 * 
 * <ul>
 * <li>#{x}^2 : Square all attributes
 * <li>#{r:x,^a.*} : Square all attributes beginning with "a"
 * <li>#{x}*#{y} : Multiply each combination of attributes
 * <li>#{r:[0-9]}*#{y} : Multiply any attribute containing a number with all other attributes
 * </ul>
 * 
 * <p><usage><b>Usage</b>: "&lt;function #1&gt;" ["&lt;function #2&gt;"] [&lt;...&gt;]
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
        clearFunctionList();
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
        for (ParsedFunction f : Functions) {
            
            // Get information about this function
            List<String> variableNames = f.getVariableNames();
            String inputString = f.getInput();

            // Generate combinations
            List<int[]> combins = generateCombinations(oldNames, f);
            
            // Generate names
            List<String> newNames = new LinkedList<>();
            for (int[] comb : combins) {
                // Get the new attribute name for this combination
                String newName = inputString;
                for (int i = 0; i < variableNames.size(); i++) {
                    newName = newName.replace("#{" + variableNames.get(i) + "}",
                            oldNames[comb[i]]);
                }
                newNames.add(newName);
            }
            Data.addAttributes(newNames);

            // Compute attributes
            double[] newVals = new double[combins.size()];
            double[] attrs = new double[f.numVariables()];
            for (BaseEntry e : Data.getEntries()) {
                // Compute the values for each etnry
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
    
    /**
     * Compute all acceptable combinations of attributes for a 
     * @param attributeNames
     * @param function
     * @return 
     */
    protected List<int[]> generateCombinations(String[] attributeNames,
            ParsedFunction function) {
        // Check if any of the variable names are regexs
        boolean anyRegex = false;
        for (String name : function.getVariableNames()) {
            if (name.toLowerCase().startsWith("r:")) {
                anyRegex = true;
                break;
            }
        }
        
        // Make combinations
        List<int[]> output;
        if (anyRegex) {
            // Get the possible attributes for each variable
            List<Collection<Integer>> possibleAttributes = new ArrayList<>(function.numVariables());
            for (String varName : function.getVariableNames()) {
                // Check whether this variable is a regex
                Collection<Integer> possible = new ArrayList<>(attributeNames.length);
                if (varName.toLowerCase().startsWith("r:")) {
                    // Get the regex
                    int firstComma = varName.indexOf(",");
                    if (firstComma == -1) {
                        throw new RuntimeException("Bad variable name (" 
                                + varName + "). Expected format: r:[name],[regex]");
                    }
                    String regex = varName.substring(firstComma + 1);
                    Pattern pattern = Pattern.compile(regex);
                    
                    // Get ones that match
                    for (int v=0; v<attributeNames.length; v++) {
                        if (pattern.matcher(attributeNames[v]).matches()) {
                            possible.add(v);
                        }
                    }
                    
                    // Edge case: No matching attributes
                    if (possible.isEmpty()) {
                        throw new RuntimeException("No attributes match regex: " + regex);
                    }
                } else {
                    for (int v=0; v<attributeNames.length; v++) {
                        possible.add(v);
                    }
                }
                possibleAttributes.add(possible);
            }
            
            // Determine number of combinations
            int nComb = 1;
            for (Collection<Integer> coll : possibleAttributes) {
                nComb *= coll.size();
            }
            
            // Generate combinations
            output = new ArrayList<>(nComb);
            for (List<Integer> comb : new CartesianSumGenerator<>(possibleAttributes)) {
                // Check for duplicates (not allowed!)
                if (comb.size() != new TreeSet<>(comb).size()) {
                    continue;
                }
                
                // Conver to int[] and add
                int[] temp = new int[comb.size()];
                for (int v=0; v<temp.length; v++) {
                    temp[v] = comb.get(v);
                }
                output.add(temp);
            }
        } else {
            // Make combinations
            output = new ArrayList<>((int) CombinatoricsUtils.binomialCoefficient(
                    attributeNames.length, 
                    function.numVariables()));
            for (int[] comb : new Combinations(
                    attributeNames.length, function.numVariables())) {
                output.add(comb);
            }
        }
        
        return output;
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
