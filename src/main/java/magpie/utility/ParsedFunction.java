package magpie.utility;

import expr.*;
import java.io.Serializable;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;

/**
 * Parses a function from text string.
 * 
 * <p>Variables in the function must be surrounded by "#{" and "}" (think Bash,
 * but using # instead of $ to denote variables), and should not contain any characters 
 * that would be confused with a math operation (e.g., +) or be named after a math function 
 * (e.g., cos). Also, don't put any whitespace inside the {}'s.
 * 
 * <p>Dev note: This class *is not* thread safe.
 * 
 * @author Logan Ward
 * @see expr.Parser
 */
public class ParsedFunction implements MultivariateFunction, Serializable {
    /**
     * Variables of this function
     */
    final private List<Variable> Variables;
    /** 
     * Names of variables known to user. Created to allow variable names
     * to contain characters that are not parseable
     */
    final private List<String> VariableNames;
    /**
     * String that was originally supplied to parser
     */
    final private String Input;
    /**
     * Function that was parsed
     */
    final private Expr Function;
    /** 
     * How many variables have been generated to date
     */
    static private long numGenerations = 0;

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
        
        Matcher varMatcher = Pattern.compile("\\#\\{[^}]*\\}").matcher(function);
        while (varMatcher.find()) {
            String variableName = varMatcher.group();
            variableNames.add(variableName);
        }
        
        // Replace "#{...}" with the variable name, and create variables
        String toParse = function.trim();
        Variables = new ArrayList<>(variableNames.size());
        VariableNames = new ArrayList<>(variableNames.size());
        for (String name : variableNames) {
            // Get the name without #{}'s
            String shortName = name.substring(2, name.length()-1);
            VariableNames.add(shortName);
            
            // Get a name suited for Expr
            String niceName = getName();
            Variables.add(Variable.make(niceName));
            
            // Replace variable name with the nice one
            toParse = toParse.replace(name, niceName);
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
        return new ArrayList<>(VariableNames);
    }

    /**
     * Get the string that was provided as input
     * @return Input string
     */
    public String getInput() {
        return Input;
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
        for (int i=0; i<VariableNames.size(); i++) {
            if (VariableNames.get(i).equals(name)) {
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
    
    /**
     * Get a unique variable name
     * @return 
     */
    synchronized private static String getName() {
        return "x" + Long.toString(ParsedFunction.numGenerations++);
    }
}
