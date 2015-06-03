package magpie.models.regression.nonlinear;

import magpie.models.regression.AbstractNonlinearRegression;

/**
 * Example for the {@link AbstractNonlinearRegression} Class. Fits a regression model to:
 * <p><code>Class = a + b * mean_GSvolume_pa</code>
 * @author Logan Ward
 * @version 0.1
 */
public class SimpleLinearModelExample extends AbstractNonlinearRegression {
    
    @Override
    protected void defineVariables() {
        addVariable("mean_GSvolume_pa");
    }

    @Override
    protected void defineCoefficients() {
        addCoefficient("a", 1.0);
        addCoefficient("b", 1.0);
    }
    
    @Override
    protected double function(double[] variables, double[] coeff) {
        return coeff[0] + coeff[1] * variables[0];
    }

    @Override
    protected String printModel_protected() {
        return String.format("%.5e + %.5e * %s", getFittedCoefficient(0),
                getFittedCoefficient(1), getVariableName(0));
    }

    @Override
    public String printModelDescription(boolean htmlFormat) {
        String output = getClass().getName() + "\n";
        
        // Add HTML indentation
        if (htmlFormat) {
            output += "<div style=\"margin: 0 0 0 10\">\n";
        }
        
        output += "\tEquation: a + b * mean_GSvolume_pa\n";
        
        // Remove indentation
        if (htmlFormat) {
            output += "</div>";
        }
        
        return output;
    }

    
    
}
