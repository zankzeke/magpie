package magpie.models.regression;

import java.util.List;
import java.util.Random;
import magpie.data.Dataset;

/**
 * Randomly assign class values. User specifies the range, guesses are uniformly distributed.
 * 
 * <usage><p><b>Usage</b>: &lt;lower bound> &lt;upper bound>
 * <br><pr><i>lower bound</i>: Lower bound of guessing range
 * <br><pr><i>upper bound</i>: Upper bound of guessing range</usage>
 * @author Logan Ward
 */
public class RandomGuessRegression extends BaseRegression {
    /** Lower bound of guess */
    private double LowerBound = 1.0;
    /** Upper bound of guess */
    private double UpperBound = 2.0;

    @Override
    public void setOptions(List Options) throws Exception {
        try { 
            LowerBound = Double.parseDouble(Options.get(0).toString());
            UpperBound = Double.parseDouble(Options.get(1).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <lower bound> <upper bound>";
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        /** Nothing to train */
    }

    @Override
    public void run_protected(Dataset TrainData) {
        Random rand = new Random(TrainData.NAttributes());
        double[] guess = new double[TrainData.NEntries()];
        double Range = UpperBound - LowerBound;
        for (int i=0; i<guess.length; i++)
            guess[i] = LowerBound + Range * rand.nextDouble();
        TrainData.setPredictedClasses(guess);
    }

    @Override
    public int getNFittingParameters() {
        return 0;
    }

    @Override
    protected String printModel_protected() {
        return "Class = (Random number between " + LowerBound + " and " + UpperBound + ")";
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        output.add(String.format("Lower Bound: %.4e", LowerBound));
        output.add(String.format("Upper Bound: %.4e", UpperBound));
        
        return output;
    }
}
