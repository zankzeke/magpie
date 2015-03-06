package magpie.utility;

import expr.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;

/**
 * Parses a function from text string.
 * 
 * <p>Variables in the function must be surrounded by "${" and "}" (think Bash),
 * and should not contain any characters that would be confused with a math 
 * operation or be named after a math function (e.g., cos). Also, don't put any
 * whitespace inside the {}'s.
 * 
 * @author Logan Ward
 * @see expr.Parser
 */
public class ParsedFunction implements MultivariateFunction {
    /**
     * Variables of this function
     */
    final private List<Variable> Variables;
    /**
     * String that was originally supplied to parser
     */
    final private String Input;
    /**
     * Function that was parsed
     */
    final private Expr Function;

    /**
     * Parse a function from a text string. Uses the <a href="https://github.com/darius/expr">
     * Expr</a> Java library.
     * @param function Function to be parsed. See Javadoc for format
     * @throws Exception 
     */
    public ParsedFunction(String function) throws Exception {
        // Store original function 
        Input = function.trim();
        
        // Get variable names
        Set<String> variableNames = new TreeSet<>();
        
        Matcher varMatcher = Pattern.compile("\\$\\{[^}]*\\}").matcher(function);
        while (varMatcher.find()) {
            String variableName = varMatcher.group();
            variableNames.add(variableName);
        }
        
        // Replace "${...}" with the variable name, and create variables
        String toParse = function.trim();
        Variables = new ArrayList<>(variableNames.size());
        for (String name : variableNames) {
            String shortName = name.substring(2, name.length()-1);
            toParse = toParse.replace(name, shortName);
            Variables.add(Variable.make(shortName));
        }
        
        // Parse the function
        Parser parser = new Parser();
        for (Variable var : Variables) {
            parser.allow(var);
        }
        
        // Get the function
        Function = parser.parseString(toParse);
    }
    
    /**
     * Get the list of variables. Returns them in the same order expected by
     * {@linkplain #value(double[]) )
     * @return 
     */
    public List<String> getVariableNames() {
        List<String> output = new ArrayList<>(Variables.size());
        for (Variable var : Variables) {
            output.add(var.name());
        }
        return output;
    }
    
    /**
     * Define the value of a certain variable in this expression
     * @param name Name of variable
     * @param val Desired value
     * @throws Exception 
     */
    public void setVariable(String name, double val) throws Exception {
        // Get index
        int ind = -1;
        for (int i=0; i<Variables.size(); i++) {
            if (Variables.get(i).name().equals(name)) {
                ind = i;
                break;
            }
        }
        if (ind == -1) {
            throw new Exception("No such variable: " + name);
        }
        setVariable(ind, val);
    }
    
    /**
     * Define the value of a certain variable in this expression
     * @param index Index of variable
     * @param val Desired value
     */
    public void setVariable(int index, double val) {
        Variables.get(index).setValue(val);
    }
    
    /**
     * Print the number of variables this function expects
     * @return Number of variables
     */
    public int numVariables() {
        return Variables.size();
    }

    @Override
    public double value(double[] point) {
        if (point.length != numVariables()) {
            throw new DimensionMismatchException(point.length, numVariables());
        }
        for (int i=0; i<point.length; i++) {
            Variables.get(i).setValue(point[i]);
        }
        return Function.value();
    }
    
    /**
     * Evaluate function with current variable values
     * @return Value of function
     */
    public double evaluate() {
        return Function.value();
    }
}
