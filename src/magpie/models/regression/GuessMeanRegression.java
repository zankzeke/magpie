/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.regression;

import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Always guess the mean of the values provided during training. User can add jitter
 * in the same way as {@link SingleGuessRegression}.
 * 
 * <usage><p><b>Usage</b>: [-jitter &lt;jitter>]
 * <br><pr><i>value</i>: Amount of random variation added to guess (default=0)</usage>
 * @author Logan Ward
 * @verion 0.2
 */
public class GuessMeanRegression extends SingleGuessRegression {

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try { 
            if (Options.length > 0)
                if (Options[0].equalsIgnoreCase("-jitter"))
                    Jitter = Double.parseDouble(Options[1]);
                else throw new Exception();
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: [-jitter <value>]";
    }

    @Override
    public int getNFittingParameters() {
        return 1;
    }
    
    

    @Override
    public void train_protected(Dataset E) {
        Guess = StatUtils.mean(E.getMeasuredClassArray());
    }
}
