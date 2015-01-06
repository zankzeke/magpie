package magpie.models.regression;

import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.regression.nonlinear.SimpleLinearModelExample;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Abstract class for models that fit terms using non-linear regression. Implementations of
 *  this class must provide information about the function being fitted by fulfilling 
 *  these operations.
 * <ol>
 * <li>{@link #function} - Function to be fitted. Expressed as a function of independent variables and fitting coefficients.</li>
 * <li>{@link #defineVariables} - Map the variables in function to attributes.</li>
 * <li>{@link #defineCoefficients} - Define names of fitting coefficients</li>
 * </ol>
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * <p> A simple example of these operations is shown in the source code of {@link SimpleLinearModelExample}.
 * 
 * <p>Currently, this code uses Powell's algorithm as implemented in Apache Common's Math Library: {@link PowellOptimizer}
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>nonlinear guess &lt;name> &lt;guess></b> - Set the initial guess for a certain parameter
 * <br><pr><i>name</i>: Name of parameter to define guess for
 * <br><pr><i>guess</i>: Initial guess for that parameter</command>
 * 
 * <command><p><b>nonlinear maxiter &lt;maxiter></b> - Set maximum number of iterations for fitting routines
 * <br><pr><i>maxiter</i>: Maximum number of iterations</command>
 *  
 * @author Logan Ward
 * @version 1.0
 */
public abstract class AbstractNonlinearRegression extends BaseRegression {
    /** Maximum of number of iterations the optimizer can perform */
    private int MaxIter = 100;
    /** Stores the names of attributes used a dependant variables */
    private final List<String> VariableNames;
    /** Names of fitting coefficients */
    private final List<String> FittingNames;
    /** Stores the indices of attributes used as dependant variables */
    private int[] VariableIndices;
    /** Initial guesses supplied to optimization algorithm */
    private final List<Double> InitialGuess;
    /** Final result from parameter fitting */
    private double[] FittedCoefficients;
    /** Whether program has been initialized (i.e. variables defined) */
    private boolean initialized = false;

    public AbstractNonlinearRegression() {
        this.InitialGuess = new LinkedList<>();
        this.FittingNames = new LinkedList<>();
        this.VariableNames = new LinkedList<>();
    }

    @Override
    public void setOptions(List Options) throws Exception {
        /* No options by default, change settings via Java interface */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    /**
     * Define the maximum number of iterations allowed. Anything less than 0 is 
     *  interpreted as functionally infinite.
     * @param MaxIter Desired maximum iteration count.
     */
    public void setMaxIterations(int MaxIter) {
        if (MaxIter < 0) // Anything less than 0 means infinite
            MaxIter = Integer.MAX_VALUE;
        this.MaxIter = MaxIter;
    }

    /**
     * Get the maximum number of iterations allowed
     * @return Maximum # of iterations
     */
    public int getMaxIter() {
        return MaxIter;
    }
    
    /**
     * Get the value of fitted coefficient
     * @param index Index of coefficient to retrieve
     * @return Value of that coefficient
     */
    public double getFittedCoefficient(int index){
        if (! isTrained()) 
            throw new Error("ERROR: Model not yet trained.");
        if (index >= FittedCoefficients.length)
            throw new Error("ERROR: Only " + FittedCoefficients.length + " fitted coefficents. Requested: " + index);
        return FittedCoefficients[index];
    }
        
    /**
     * Generate an anonymous {@link MultivariateFunction} object that calculates
     *  the error between the equation described by the user as a function of the 
     *  fitting coefficients.
     * 
     * @param Data Dataset used to calculate the error
     * @return Multivariate function as described above
     */
    protected MultivariateFunction makeObjectiveFunction(final Dataset Data) {
        return new MultivariateFunction() {
            @Override
            public double value(double[] point) {
                double[] sqerror = new double[Data.NEntries()];
                for (int i=0; i < Data.NEntries(); i++) {
                    BaseEntry E = Data.getEntry(i);
                    sqerror[i] = evaluateFunction(E, point) - E.getMeasuredClass();
                    sqerror[i] *= sqerror[i];
                }
                return StatUtils.mean(sqerror);
            }
        };
    }
    
    /** 
     * Define which attributes are used by the function. This function is run a single time
     *  during training. As long as variables are defined by the time this function is complete,
     *  it does not matter how your code actually does it.
     * <p>Variables are defined using the {@link #addVariable} function.     * 
     * <p>In {@link #function} these names are mapped to the <code>variable</code> array 
     * in the same order you define them.
     */
    protected abstract void defineVariables();
    
    /**
     * Define a variable to be used in the user function (if not already defined)
     * @param Name Name of attribute to be include in equation.
     */
    protected void addVariable(String Name) {
        if (! VariableNames.contains(Name))
            VariableNames.add(Name);
    }
    
    /**
     * @return Number of variables used by the user-defined function
     */
    protected int NVariables() {
        return VariableNames.size();
    }
    
    /**
     * @param index Index of variable to be retrieved
     * @return Name of that attribute variable
     */
    public String getVariableName(int index) {
        return VariableNames.get(index);
    }
    
    /**
     * Define the names and, if desired, initial guesses of fitting coefficients. As
     * long as coefficients are defined by the time this program exits, it does not matter
     * what this code actually does (i.e. you can define coefficients in other functions).
     * <p>Coefficients are defined using the {@link #addCoefficient} function.
     * <p> In {@link #function} these names are mapped to the <code>coeff</code> array 
     * in the same order you define them.
     * 
     */
    protected abstract void defineCoefficients();
    
    /**
     * Adds a new fitting parameter to function (if not already defined). Uses a default guess of 1.0.
     * @param Name Name of fitting parameter
     */
    protected void addCoefficient(String Name) {
        addCoefficient(Name, 1.0);
    }
    
    /**
     * Adds a new fitting parameter to function (if not already defined)
    * @param Name Name of fitting parameter
     * @param Guess Initial guess
     */
    protected void addCoefficient(String Name, Double Guess) {
        if (! FittingNames.contains(Name)) {
            FittingNames.add(Name); InitialGuess.add(Guess);
        }
    }
    
    /**
     * @return Number of fitting parameters in function
     */
    public int NCoefficients() {
        return FittingNames.size();
    }

    @Override
    public int getNFittingParameters() {
        return NCoefficients();
    }    
    
    /**
     * Return name of coefficient, given index
     * @param index Index of coefficient to 
     * @return Name of that coefficient
     */
    public String getCoefficientName(int index) {
        if (NCoefficients() <= index)
            throw new Error("ERROR: Not that many coefficients");
        return FittingNames.get(index);
    }
    
    
    /**
     * Set the initial guess for a coefficient
     * @param Name Name of coefficient to set
     * @param value Desired initial guess
     * @throws Exception If there is no coefficient by that name
     */
    public void setInitialGuess(String Name, double value) throws Exception {
        int index = FittingNames.indexOf(Name);
        if (index == -1) throw new Exception("ERROR: No coefficient with name: " + Name);
        InitialGuess.set(index, value);
    }
    
    /**
     * Function defined by the user. Program supplies attributes in the same order as 
     * defined in {@linkplain #defineVariables}. 
     * @param variables Values of attributes used as independent variables
     * @param coeff Coefficients of equation
     * @return Value of equation given those inputs
     */
    abstract protected double function(double[] variables, double[] coeff);

    /**
     * Evaluate the user-defined function.
     * @param Entry Entry to be evaluated
     * @param coeff Coefficients to use
     * @return Value of the function for that entry
     */
    private double evaluateFunction(BaseEntry Entry, double[] coeff) {
        double[] x = new double[NVariables()];
        for (int i=0; i < NVariables(); i++) 
            x[i] = Entry.getAttribute(VariableIndices[i]);
        return function(x, coeff);
    }
    
    @Override
    protected void train_protected(Dataset TrainData) {
        if (! initialized) {
            defineVariables(); defineCoefficients(); initialized = true;
        }
        findVariableIndicies(TrainData);
        try {
            FittedCoefficients = fitFunction(TrainData);
        } catch (TooManyEvaluationsException e) {
            throw new Error("Maximum number of iterations exceeded.");
        }
        
        // If we are doing robust fitting
        /* 
         * if (RobustParameter < Double.Infinity) 
         *    clone TrainData
         *    eliminate outliers from clone
         *    train model on clone
         */
    }

    @Override
    public void run_protected(Dataset TrainData) {
        double[] result = new double[TrainData.NEntries()];
        for (int i=0; i<TrainData.NEntries(); i++)
            result[i] = evaluateFunction(TrainData.getEntry(i), FittedCoefficients);
        TrainData.setPredictedClasses(result);
    }
       
    /**
     * Given a dataset: Get the indices of {@link #VariableNames}, store them.
     * @param Data Dataset of interest
     */
    private void findVariableIndicies(Dataset Data) {
        VariableIndices = new int[VariableNames.size()];
        for (int i=0; i < VariableNames.size(); i++) {
            int index = Data.getAttributeIndex(VariableNames.get(i));
            if (index == -1) 
                throw new Error("Dataset is missing attribute - " + VariableNames.get(i));
            VariableIndices[i] = index;
        }
    }
    
    /**
     * Generate the initial guesses as a double array
     * @return Array containing initial guesses contained in {@link #InitialGuess}
     */
    private double[] getStartingPoint() {
        double[] x = new double[NCoefficients()];
        for (int i=0; i < NCoefficients(); i++) x[i] = InitialGuess.get(i);
        return x;
    }

    /**
     * Given a dataset, fit the coefficients
     * @param TrainData Dataset to use for training
     * @return Fitted coefficients
     * @throws TooManyEvaluationsException 
     */
    private double[] fitFunction(Dataset TrainData) throws TooManyEvaluationsException {
        // Define the optimization problem
        MultivariateOptimizer optimizer = new PowellOptimizer(1e-6, Double.MIN_VALUE);
        InitialGuess init = new InitialGuess(getStartingPoint());
        ObjectiveFunction objFunction = new ObjectiveFunction(makeObjectiveFunction(TrainData));
        MaxIter maxIter = new MaxIter(MaxIter);
        MaxEval maxEval = MaxEval.unlimited();
        
        // Run the optimizer
        PointValuePair result = optimizer.optimize(init, objFunction, 
            GoalType.MINIMIZE, maxIter, maxEval);
        return result.getPoint();
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) return super.runCommand(Command);
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "nonlinear": case "nonlin": case "nl":
                return runNonlinearCommand(Command.subList(1, Command.size()));
        }
        return super.runCommand(Command); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Handle commands for nonliear models
     * @param Command
     * @return
     * @throws Exception 
     */
    public Object runNonlinearCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) throw new Exception("This is a nonlinear model. But you didn't ask for anything.");
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "guess": {
                String Name; double guess;
                try {
                    Name = Command.get(1).toString();
                    guess = Double.parseDouble(Command.get(2).toString());
                } catch (Exception e) {
                    throw new Exception("Usage: nonlinear guess <parameter name> <guess>");
                }
                setInitialGuess(Name, guess);
            } break;
            case "maxiter": {
                int maxIters;
                try {
                    maxIters = Integer.parseInt(Command.get(1).toString());
                } catch (Exception e) {
                    throw new Exception("Usage: nonlinear maxiter <maxiter>");
                }
                setMaxIterations(maxIters);
            } break;
            default:
                throw new Exception("Nonlinear command not recognized: " + Action);
        }
        return null;
    }
}
