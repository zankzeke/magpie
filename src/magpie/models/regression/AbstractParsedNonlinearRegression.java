package magpie.models.regression;

import java.util.LinkedList;
import java.util.List;
import magpie.user.CommandHandler;

/**
 * Superclass of {@link AbstractNonlinearRegression}-based models which parse the equation
 * of interest from text input. 
 * 
 * <p>Use this class by passing a specially-formatted equation to 
 * {@linkplain #parseFormula(java.lang.String) .
 * 
 * <usage><p><b>Usage</b>: &lt;equation>
 * <br><pr><i>equation</i>: Equation to be fitted. See Javadoc for rules.</usage>
 * <p>Rules for writing an equation:
 * <ul>
 * <li>Variables are attributes from a dataset, and must be defined using the notation:
 * <code>#{a:&lt;Attribute Name&gt;}</code></li>
 * <li>Fitting constants must be defined using the expression: <code>#{&lt;Constant Name&gt;}</code></li>
 * </ul>
 * <p>Example expression with two fitting constants and a single attribute 
 *  variable : <code>#{a} + #{b} * #{a:NComp}</code>
 * 
 * @author Logan Ward
 */
public abstract class AbstractParsedNonlinearRegression extends AbstractNonlinearRegression {
    /** Stores the user-provided formula */
    protected String UserFormula = "";

    @Override
    protected void defineCoefficients() {
        /* Nothing to do */
    }

    /**
     * Reads an expression and stores the names of all variables. Also sets the
     *  required fields of {@link AbstractNonlinearRegression}
     * @param Expression Expression to be parsed
     * @return Expression in a form usable by Expr
     * @throws Exception If poorly-formed expression
     */
    protected abstract String defineVariables(String Expression) throws Exception;

    @Override
    protected void defineVariables() {
        /* Nothing to do */
    }

    /**
     * Define the internal {@link Evaluator} and parse the user-defined function
     * @param Expression User-defined function
     * @throws Exception If the formula is illegible
     */
    public abstract void prepareEvaluator(String Expression) throws Exception;

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        if (Options.length == 0) {
            throw new Exception(printUsage());
        }
        // Write down the formula as a string
        String formula = Options[0];
        for (int i = 1; i < Options.length; i++) {
            formula += " " + Options[i];
        }
        
        parseFormula(formula);
    }

    /**
     * Parse formula to use as model. See description of {@linkplain AbstractParsedNonlinearRegression}
     * for rules.
     * @param formula Formula to be parsed
     * @throws Exception 
     * @see AbstractParsedNonlinearRegression
     */
    public void parseFormula(String formula) throws Exception {
        // Parse formula
        UserFormula = formula;
        
        // Get list of variables
        formula = defineVariables(formula);
        
        // Set up the evaluator
        prepareEvaluator(formula);
    }

    @Override
    public String printUsage() {
        return "Usage: <equation to be fit (see docs)>";
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = new LinkedList<>();
        
        output.add("Equation: " + UserFormula + "\n");
        
        return output;
    }

    @Override
    protected String printModel_protected() {
        // Works by pasting fitted coefficients into the UserFormula
        String output = "Class = " + UserFormula;
        for (int i=0; i<NCoefficients(); i++) {
            output = output.replace("#{" + getCoefficientName(i) + "}",
                    String.format("%.5e", getFittedCoefficient(i))); // STOPPED HERE
        }
        return output + "\n";
    }
}
