package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * Filter entries that have class variables within or outside a certain range.
 * 
 * <usage><p><b>Usage</b>: &lt;predicted|measured> &lt;inside|outside> &lt;lower bound> &lt;upper bound>
 * <br><pr><i>predicted|measured</i>: Whether to use the predicted or measure class variable
 * <br><pr><i>inside|outside</i>: Whether entries pass if they are inside or outside the range
 * <br><pr><i>lower bound</i>: Lower bound of range
 * <br><pr><i>upper bound</i>: Upper bound of range</usage>
 * @author Logan Ward
 * @version 0.1
 */
public class ClassRangeFilter extends BaseDatasetFilter {
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
            
            if (Options[1].equalsIgnoreCase("inside"))
                insideRange = true;
            else if (Options[1].equalsIgnoreCase("outside"))
                insideRange = false;
            else 
                throw new Exception();
            
            lowerBound = Double.parseDouble(Options[2]);
            upperBound = Double.parseDouble(Options[3]);
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
    protected boolean[] label(Dataset D) {
        if (useMeasured && (! D.getEntry(0).hasMeasurement()))
            throw new Error("Missing measured class.");
        if (!useMeasured && (! D.getEntry(0).hasPrediction()))
            throw new Error("Missing predicted class.");
        
        boolean[] output = new boolean[D.NEntries()];
        for (int i=0; i<D.NEntries(); i++) {
            boolean isInside = false;
            double value = useMeasured ? D.getEntry(i).getMeasuredClass()
                    : D.getEntry(i).getPredictedClass();
            if (value > lowerBound && value < upperBound)
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
