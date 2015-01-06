package magpie.models.regression.nonlinear;

import magpie.models.regression.AbstractNonlinearRegression;

/**
 * Test for the {@link AbstractNonlinearRegression} Class. Fits a regression model to a
 *  model found to correct deviation from Vegard's law:
 * <p><code>Class = a - b * dev_GSvolume_pa * MeanIoincCharacter </code>
 * @author Logan Ward
 * @version 0.1
 */
public class VolumeCorrectionTest extends AbstractNonlinearRegression {
    
    @Override
    protected void defineVariables() {
        addVariable("dev_GSvolume_pa");
        addVariable("MeanIonicChar");
    }
    
    @Override
    protected void defineCoefficients() {
        addCoefficient("a", 1.0);
        addCoefficient("b", 1.0);
    }
    
    @Override
    protected double function(double[] variables, double[] coeff) {
        return coeff[0] - coeff[1] * variables[0] * variables[1];
    }

    @Override
    protected String printModel_protected() {
        return String.format("%.5e + %.5e * %s * %s", getFittedCoefficient(0),
                getFittedCoefficient(1), getVariableName(0), getVariableName(1));
    }
}
