
package magpie.models.regression;

import java.util.List;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.regression.MillerUpdatingRegression;
import org.apache.commons.math3.stat.regression.UpdatingMultipleLinearRegression;

/**
 * Performs linear regression using all attributes in a Dataset raised to integer
 * exponents. This method creates models that are nonlinear polynomials, like this:
 * <center><code>f(x,y,z) = a + b * x + c * x<sup>2</sup> + d * y + e * y<sup>2</sup> + ...</code></center>
 * 
 * <usage><p><b>Usage</b>: &lt;order>
 * <br><pr><i>order</i>: Maximum order of terms in polynomial</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PolynomialRegression extends BaseRegression {
    /** Desired order of polynomial */
    protected int order = 1;
    /** Number of attributes used in model */
    private int numAttributes = 0;
    /** Coefficients of each term in the polynomial */
    protected double[] coefficients = null;
    /** Names of attributes */
    private String[] attributeNames;

    @Override
    public void setOptions(List Options) throws Exception {
        try {
            order = Integer.parseInt(Options.get(0).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public PolynomialRegression clone() {
        PolynomialRegression x = (PolynomialRegression) super.clone(); 
		if (coefficients != null) {
			x.coefficients = coefficients.clone();
			x.attributeNames = attributeNames.clone();
		}
        return x;
    }
    
    @Override
    public String printUsage() {
        return "Usage: <order of polynomial>";
    }

	/**
	 * Define order of polynomial.
	 * @param order Desired order of polynomial.
	 */
	public void setOrder(int order) {
		this.order = order;
	}
    
    /**
     * Get coefficients of model. 
     * 
     * Arranged: Intercept, x, x<sup>2</sup>, ..., x<sup>N</sup>,  y, ..., y<sup>N</sup>, z, ...
     * @return 
     */
    public double[] getCoefficients() {
        return coefficients.clone();
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        if (TrainData.NAttributes() > 10) {
            System.err.println("WARNING: PolynomialRegression was not intended to be used data with many attributes." +
                    " Your program could be using a lot of memory right now. Consider using an attribute selector.");
        }
        // Extract necessary data
        double[][] attributes = TrainData.getAttributeArray();
        double[] classVariable = TrainData.getMeasuredClassArray();
        numAttributes = TrainData.NAttributes();
        attributeNames = TrainData.getAttributeNames();
        
        // Fit a polynomial model
        coefficients = fitPolynomialModel(attributes, order, classVariable);
    }

    @Override
    public void run_protected(Dataset TrainData) {
        if (TrainData.NAttributes() != numAttributes)
            throw new Error("Dataset has more attributes that what was used during training");
        
        // Run the model
        double[][] attributes = TrainData.getAttributeArray();
        double[] result = runPolynomialModel(attributes, order, coefficients);
        
        // Store results
        TrainData.setPredictedClasses(result);
    }

    @Override
    public int getNFittingParameters() {
        return 1 + numAttributes * order;
    }
    
    /**
     * Fit a polynomial model based on a matrix of attribute values. Returns the 
     *  coefficients of this model in the following order:<p>
     * Intercept, Coefficient of attribute1, Coefficient of attribute1<sup>2</sup>, ...,
     *   Coefficient of attribute2, ...
     * @param attributes Matrix containing attributes for each entry (entries are rows, attributes columns)
     * @param order Desired order of polynomial
     * @param classVariable Class variable for each entry
     * @return Coefficients for model
     */
    static public double[] fitPolynomialModel(double[][] attributes, int order, double[] classVariable) {
        double[][] expandedAttributes = expandAttributes(attributes, order);
        
        // Since we are using a UpdatingMultipleLinearRegression, it is not really 
        //  necessary to calculated all of the attributes at one time. But, 
        //  it is easier to do so and this method is not important enough to 
        //  spend time making it marginally more effiicent.
        UpdatingMultipleLinearRegression Fit = new MillerUpdatingRegression(expandedAttributes[0].length, false);
        Fit.addObservations(expandedAttributes, classVariable);
        
        // Get the results of the fit
        double[] parameters = Fit.regress().getParameterEstimates();
        
        // Set anything that is NaN (if it was not used in fit) to 0.0
        for (int i=0; i<parameters.length; i++)
            if (Double.isNaN(parameters[i])) parameters[i] = 0.0;
        
        return parameters;
    }
    
    /**
     * Expand attribute array in order to allow it to be used to fit a polynomial
     *  model. First column is all ones (for the intercept), other columns are for
     *  terms listed in the same order as {@linkplain #fitPolynomialModel(double[][], int, double[]) }.
     * @param attributes Matrix containing attributes for each entry (entries are rows, attributes columns)
     * @param order Desired order of polynomial
     * @return Matrix that can be used to fit this model
     */
    static public double[][] expandAttributes(double[][] attributes, int order) {
        double[][] output = new double[attributes.length][attributes[0].length * order + 1];
        for (int e=0; e < attributes.length; e++) {
            int count = 0;
            output[e][count++] = 1;
            for (int a=0; a < attributes[0].length; a++) {
                double attrValue = attributes[e][a];
                // Attribute to the first power
                output[e][count++] = attrValue;
                for (int o=1; o < order; o++)  {
                    // Attribute to the subsequent powers
                    output[e][count] = output[e][count - 1] * attrValue;
                    count++;
                }
            }
        }
        return output;
    }
    
    /**
     * Run a polynomial model based on a matrix of attribute values. Coefficients
     *  coefficients of this model in the following order:<p>
     * Intercept, Coefficient of attribute1, Coefficient of attribute1<sup>2</sup>, ...,
     *   Coefficient of attribute2, ...
     * @param attributes Matrix containing attributes for each entry (entries are rows, attributes columns)
     * @param order Desired order of polynomial
     * @param coefficients Coefficients of polynomial model
     * @return Value of model for each entry
     */
    static public double[] runPolynomialModel(double[][] attributes, int order, double[] coefficients) {
        double[][] expandedAttributes = expandAttributes(attributes, order);
        double[] output = new double[attributes.length];
        
        // Evaluate model
        for (int e=0; e<output.length; e++) {
            output[e] = coefficients[0];
            for (int i=1; i<coefficients.length; i++)
                output[e] += expandedAttributes[e][i] * coefficients[i];
        }
        
        return output;
    }

    @Override
    protected String printModel_protected() {
        String output = String.format("%.3e", coefficients[0]);
        int count=1;
        for (int a=0; a<numAttributes; a++) {
            for (int o=1; o<=order; o++) {
                output += String.format(" + %.3e * %s", coefficients[count++],
                        attributeNames[a]);
                if (o > 1) {
                     output += String.format(" ^ %d", o);
                }
                if (count % 4 == 0)
                    output += "\n\t";
            }
        }
        return output + "\n";
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        output.add("Order: " + order);
        
        return output;
    }
    
}
