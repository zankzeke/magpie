/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.regression;

import java.util.Arrays;
import java.util.List;
import magpie.data.Dataset;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.user.CommandHandler;

/**
 * Guess a single, user-specified value for the class variable of all entries.
 * <p>User can add jitter to the guess, which adds a small random value to the guess. This has the effect of 
 * forcing the entries to be randomly ranked by an {@link BaseEntryRanker}, but without
 * changing the actual guess by that much. The actual size of this step is set by the 
 * value of {@link #Jitter}.
 * 
 * <usage><p><b>Usage</b>: &lt;guess> [-jitter &lt;jitter>]
 * <br><pr><i>guess</i>: Predicted value to use for each entry
 * <br><pr><i>value</i>: Amount of random variation added to guess (default=0)</usage>
 * 
 * @author Logan Ward
 * @version 0.2
 */
public class SingleGuessRegression extends BaseRegression {
    /** Guess for all classes */
    protected double Guess;
    /** Amount of jitter added to the answer */
    protected double Jitter;
    
    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            setGuess(Double.parseDouble(Options[0]));
            if (Options.length > 1)
                if (Options[1].equalsIgnoreCase("-jitter"))
                    Jitter = Double.parseDouble(Options[2]);
                else throw new Exception();
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Options: <guess> [-jitter <value>]";
    }
        
    /** Sets the user's guess 
     * @param x Desired guess
     */
    public void setGuess(double x) { Guess = x; };
    /** Get the user's guess
     * @return Currently-used guess
     */
    public double getGuess() { return Guess; }
    
    @Override public SingleGuessRegression clone() {
        SingleGuessRegression x = (SingleGuessRegression) super.clone();
        return x;
    }
    
    @Override public void train_protected(Dataset E) { };
    @Override public void run_protected(Dataset E) {
        double[] pred = new double[E.NEntries()];
        if (Jitter == 0)
            Arrays.fill(pred, Guess);
        else {
            for (int i=0; i<pred.length; i++)
                pred[i] = Guess + Jitter * ( -1 + 2 * Math.random());
        }
        E.setPredictedClasses(pred);
    }

    @Override
    public int getNFittingParameters() {
        return 0;
    }

    @Override
    protected String printModel_protected() {
        String output = "Class = " + String.format("%.5e\n", Guess);
        return output;
    }
}
