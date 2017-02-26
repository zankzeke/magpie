package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.user.CommandHandler;

/**
 * Filter entries that have property variables within or outside a certain range.
 * 
 * <usage><p><b>Usage</b>: &lt;predicted|measured> &lt;property&gt; &lt;inside|outside> &lt;lower bound> &lt;upper bound>
 * <br><pr><i>predicted|measured</i>: Whether to use the predicted or measure class variable
 * <br><pr><i>property</i>: Name of property used for filter
 * <br><pr><i>inside|outside</i>: Whether entries pass if they are inside or outside the range
 * <br><pr><i>lower bound</i>: Lower bound of range
 * <br><pr><i>upper bound</i>: Upper bound of range</usage>
 * @author Logan Ward
 * @version 0.1
 */
public class PropertyRangeFilter extends BaseDatasetFilter {
    /** Name of target property */
    String Target;
    /** Whether to use predicted or measured classes */
    boolean useMeasured;
    /** Keep entries inside range? */
    boolean insideRange = false;
    /** Lower bound of range */
    double lowerBound;
    /** Upper bound of range */
    double upperBound;
    
    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            if (Options[0].equalsIgnoreCase("predicted"))
                useMeasured = false;
            else if (Options[0].equalsIgnoreCase("measured")) 
                useMeasured = true;
            else
                throw new Exception();
            
            Target = Options[1];
            
            if (Options[2].equalsIgnoreCase("inside"))
                insideRange = true;
            else if (Options[2].equalsIgnoreCase("outside"))
                insideRange = false;
            else 
                throw new Exception();
            
            lowerBound = Double.parseDouble(Options[3]);
            upperBound = Double.parseDouble(Options[4]);
            if (lowerBound >= upperBound)
                throw new Exception();
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <predicted|measured> <inside|outside> <lower bound> <upper bound>";
    }

    
    @Override
    public boolean[] label(Dataset D) {
        if (! (D instanceof MultiPropertyDataset)) {
            throw new Error("Data must extend MultiPropertyDataset");
        }
        MultiPropertyDataset P = (MultiPropertyDataset) D;
        boolean[] output = new boolean[D.NEntries()];
        double[] value = useMeasured ? P.getMeasuredPropertyArray(Target) 
                : P.getPredictedPropertyArray(Target);
                
        for (int i=0; i<D.NEntries(); i++) {
            boolean isInside = false;
            if (value[i] > lowerBound && value[i] < upperBound)
                output[i] = insideRange;
            else
                output[i] = !insideRange;
        }
        return output;
    }

    @Override
    public void train(Dataset TrainingSet) {
           /* Nothing to train */
    }
}
