/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.regression;

import java.util.LinkedList;
import java.util.List;
import expr.*;
import java.util.Iterator;

/**
 * Performs non-linear regression on a function supplied as a string. Uses a modified version of
 * the <a href="https://github.com/darius/expr"> Expr</a> library, which should have been
 * provided with this software.
 * 
 * <p>Formatting rules are shown in {@linkplain  AbstractParsedNonlinearRegression}.
 * 
 * <usage><p><b>Usage</b>: &lt;equation to be fit...&gt;
 * <br><pr><i>equation</i>: Equation to be fit. See Javadoc of {@linkplain 
 * AbstractParsedNonlinearRegression} for details.</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class NonlinearRegressionExpr extends AbstractParsedNonlinearRegression {
    /** Handles the evaluation of the user-defined formula */
    private Expr Evaluator;
    
    /** 
     * List of Variables used by the Expr library. This list is ordered with the
     * attribute variables first, and then the fitting coefficients.
     */
    private List<Variable> Variables = new LinkedList<>();
    
    /**
     * Reads an expression and stores the names of all variables. Also sets the 
     *  required fields of {@link AbstractNonlinearRegression}
     * @param Expression Expression to be parsed
     * @return Expression in a form usable by Expr
     * @throws Exception If poorly-formed expression
     */
    @Override
    protected String defineVariables(String Expression) throws Exception {
        int curPos = 0; // Current position of parser
        while (true) {
            // Find the next variable
            int varPos = Expression.indexOf("#{", curPos);
            if (varPos == -1) break; // No more variables
            int varEnd = Expression.indexOf("}", varPos);
            if (varEnd == -1) throw new Exception("Poorly formed expression - Can't find }");
            curPos = varEnd; // Move up the serach
            String varName = Expression.substring(varPos + 2, varEnd);
            
            // Determine whether it is a fitting constant or variable
            boolean isVariable = varName.startsWith("a:");
            
            // Add it to the appropriate list
            if (isVariable) {
                String AttrName = varName.substring(2);
                addVariable(AttrName);
            } else {
                addCoefficient(varName);
            }
        }
        
        // Create a list of variables with the following order
        // Also, remove the #{<...>} notation from the formula string
        for (int i=0; i<NVariables(); i++) {
            String varName = "a:" + getVariableName(i);
            Variable newVariable = Variable.make(varName);
            Expression = Expression.replace("#{" + varName + "}", varName);
            Variables.add(newVariable);
        }
        for (int i=0; i<NCoefficients(); i++) {
            String varName = getCoefficientName(i);
            Variable newVariable = Variable.make(varName);
            Expression = Expression.replace("#{" + varName + "}", varName);
            Variables.add(newVariable);
        }
        
        return Expression;
    }
    
    /**
     * Define the internal {@link Evaluator} and parse the user-defined function
     * @param Expression User-defined function
     * @throws SyntaxException If the formula is illegible
     */
    public void prepareEvaluator(String Expression) throws SyntaxException {
        Parser Parser = new Parser();
        // Define the variable set
        Iterator<Variable> iter = Variables.iterator();
        while (iter.hasNext()) Parser.allow(iter.next());
        // Parse the expression
        Evaluator = Parser.parseString(Expression);
    }

    @Override
    protected double function(double[] variables, double[] coeff) {
        for (int i=0; i<variables.length; i++) Variables.get(i).setValue(variables[i]);
        for (int i=0; i<coeff.length; i++) 
            Variables.get(variables.length + i).setValue(coeff[i]);
        return Evaluator.value();
    }
}