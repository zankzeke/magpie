
package magpie.statistics.performance;

import magpie.data.Dataset;
import magpie.optimization.algorithms.OptimizationHelper;
import magpie.user.CommandHandler;
import org.apache.commons.math3.stat.StatUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Statistics for the ability of a model to find entries with a class near a target value. 
 * Basically, all this does is report statistics of <code>abs( class - target )</code> for 
 * each entry. 
 * 
 * <p><i>Window Size Analysis</i>
 * <p>This class can perform an analysis how many entries are predicted to have a class variable
 * be less than a certain distance from the target value, and how many of those actually truly fall
 * within some specified distance of the target. Consider this as a way of testing how well this
 * model can be used as a way of screening candidate materials for possessing a certain 
 * level of a target property.In order to run this analysis, specify a maximum window 
 * size and a tolerance window for successful predictions. This chart can be printed out 
 * using the "window" command.
 * 
 * <p><i>Candidate Analysis</i>
 * <p>Similarly, one can test then number of entries truly within the acceptance tolerance when
 * the a certain number of closest candidates are selected. For a range of numbers of 
 * candidates up to the maximum number of candidates, the average distance of the entries from 
 * the target value and fraction correctly identified to be within a certain tolerance are calculated. 
 * These results will be calculated if  the maximum number of candidates and tolerance are set.
 * Print using the "rank" command;
 * 
 * <usage><p><b>Usage</b>: &lt;target> [-accept &lt;Acceptance Tolerance>] [-window &lt;Max Window Size>] [-cands &lt;Max Number Candidates>]
 * <br><pr><i>Target</i>: Desired value of class variable
 * <br><pr><i>Acceptance Tolerance</i>: If the actual class variable is within this value of the target, consider the prediction a success
 * <br><pr><i>Maximum Window Size</i>: Set this and Acceptance Tolerance to analyze accuracy as a function of tolerance window size
 * <br><pr><i>Max Number Candidates</i>: Set this and acceptance tolerance in order to study accuracy as a function of number of candidates selected</usage>
 * 
 * <p><b><u>Implemented Print Commands:</u></b>
 * 
 * <print><p><b>rank</b> - Prints how the performance of best-performing entries according to model predictions</print>
 * 
 * <print><p><b>window</b> - Prints statistics regarding this model&rsquo;s ability to act as a filter</print>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class TargetRegressionStatistics extends RegressionStatistics {
    /** Target value of class variable */
    protected double Target;
    /** Tolerance for successful prediction */
    protected double Tolerance = -1;
    /** Maximum tolerance during window size analysis */
    protected double MaxWindowSize = -1;
    /** Maximum number of candidates for ranking analysis */
    protected int MaxCandidates = -1;
    /** How many steps to use in window/ranking analysis */
    private int NSteps = 20;
    /** Results from candidate analysis. First column is number of top candidates selected,
     * second is average distance from target, third is number within tolerance */
    private double[][] rankAnalysis = null;
    /** Results from window analysis. First column is window size, second is the number of 
     * entries inside window, third is fraction within tolerance */
    private double[][] windowAnalysis = null;

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            setTarget(Double.parseDouble(Options[0]));
            int count = 1;
            while (count < Options.length) {
                switch (Options[count].toLowerCase()) {
                    case "-accept":
                        count++;
                        setTolerance(Double.parseDouble(Options[count]));
                        break;
                    case "-window":
                        count++;
                        setMaxWindowSize(Double.parseDouble(Options[count]));
                        break;
                    case "-cands":
                        count++;
                        setMaxCandidates(Integer.parseInt(Options[count]));
                        break;
                    default:
                        throw new Exception();
                }
                count++;
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Make sure tolerance is defined if -window or -cands has been run
        if (MaxWindowSize > 0 && Tolerance < 0)
            throw new Exception("-accept must be set with -window");
        if (MaxCandidates > 0 && Tolerance < 0)
            throw new Exception("-accept must be set with -cands");
    }

    @Override
    public String printUsage() {
        return "Usage: <Target> [-accept <Acceptance Tolerance>] [-window <Max Window Size>] [-cands <Max Number Candidates>]";
    }

    /**
     * Set the number of steps to use when calculating model performance for ranking
     *  or filtering.
     * @param NSteps Number of step increments (default: 20)
     */
    public void setNSteps(int NSteps) {
        this.NSteps = NSteps;
    }
    
    

    /**
     * Define target value of class variable.
     * @param Target Desired target value
     */
    public void setTarget(double Target) {
        this.Target = Target;
    }

    /**
     * Define the error threshold that is considered a successful prediction. For example,
     *  if the target value is 2.5 and anything between 2.0 and 3.0 is OK, use 0.5.
     *  Set to -1 if you do not want to use any kind of tolerance analysis
     * @param Tolerance Desired tolerance
     */
    public void setTolerance(double Tolerance) {
        this.Tolerance = Tolerance;
    }

    /**
     * Set the maximum number of candidates for an analysis of how the number of
     *  candidates affects prediction accuracy. Note that one must also specify the 
     *  desired tolerance.
     * @param MaxCandidates Desired maximum number of candidates
     */
    public void setMaxCandidates(int MaxCandidates) {
        this.MaxCandidates = MaxCandidates;
    }

    /**
     * Set the maximum window size for an analysis of how tolerance window size affects
     *  prediction accuracy. Note that one must also specify the desired tolerance.
     * @param MaxWindowSize Desired maximum window size
     */
    public void setMaxWindowSize(double MaxWindowSize) {
        this.MaxWindowSize = MaxWindowSize;
    }

    @Override
    protected void evaluate_protected(Dataset Results) {
        Measured = Results.getMeasuredClassArray();
        Predicted = Results.getPredictedClassArray();
        for (int i=0; i<Measured.length; i++) {
            Measured[i] = Math.abs(Measured[i] - Target);
            Predicted[i] = Math.abs(Predicted[i] - Target);
        }
        
        // Get simple population statistics
        getStatistics(Measured, Predicted);
        
        // If max candidates is set, perform candidate analysis
        if (MaxCandidates > 0) 
            runCandidateAnalysis(Measured, Predicted);
        
        // If max window size is set, perform filter analysis
        if (MaxWindowSize > 0)
            runWindowAnalysis(Measured, Predicted);
    }
    
    /**
     * Perform a candidate selection analysis (described in documentation for this
     *  class as a whole). Basically, its just the accuracy of the model as a function 
     *  of number of candidates selected.
     * @param measured Measured distance of class variable from target
     * @param predicted Predicted distance of class variable from target
     */
    protected void runCandidateAnalysis(double[] measured, double[] predicted) {
        if (Tolerance < 0)
            throw new Error("Cannot perform analysis: Tolerance not set");
        
        // Find which entries are predicted to be closest to the target
        double[] predictedClone = Arrays.copyOf(predicted, predicted.length);
        int[] rank = OptimizationHelper.sortAndGetRanks(predictedClone, false);
        
        // Gradually step up window size
        double stepSize = predicted.length <= MaxCandidates ? 1.0 :
                (double) MaxCandidates / (double) NSteps;
        rankAnalysis = new double[Math.min(predicted.length, NSteps)][3];
        for (int step = 0; step < rankAnalysis.length; step++) {
            int nCands = (int) Math.round((step + 1) * stepSize);
            // Get the measureddistances of the top candidates
            double[] topMeasured = new double[nCands];
            for (int i=0; i<nCands; i++)
                topMeasured[i] = measured[rank[i]];
            // Generate statistics
            double mean = StatUtils.mean(topMeasured);
            double nHits = 0;
            for (int i=0; i<nCands; i++)
                if (topMeasured[i] < Tolerance) nHits++;
            // Store results
            rankAnalysis[step][0] = (double) nCands;
            rankAnalysis[step][1] = mean;
            rankAnalysis[step][2] = nHits;
        }
    }
    
    /**
     * Perform a window-size analysis. Basically, this test the ability of a model
     * to find candidates within a certain window if all materials within a specified
     * tolerance are selected.
     * @param measured Measured distance of class variable from target
     * @param predicted Predicted distance of class variable from target
     */
    protected void runWindowAnalysis(double[] measured, double[] predicted) {
        if (Tolerance < 0)
            throw new Error("Cannot perform analysis: Tolerance not set");
        
        // Test several different window sizes
        double stepSize = MaxWindowSize / (double) NSteps;
        windowAnalysis = new double[NSteps][3];
        for (int step=0; step<NSteps; step++) {
            double windowSize = stepSize * (step + 1);
            // Find number inside window / hits
            double insideWindow = 0, nHits = 0;
            for (int i=0; i<predicted.length; i++) {
                if (predicted[i] < windowSize) {
                    insideWindow++;
                    if (measured[i] < Tolerance) nHits++;
                }
            }
            // Store results
            windowAnalysis[step][0] = windowSize;
            windowAnalysis[step][1] = insideWindow;
            windowAnalysis[step][2] = insideWindow > 0 ? nHits / insideWindow : insideWindow;
        }
    }
    
    /**
     * Print results of the candidate ranking analysis.
     * @return A table containing for each row: Number of candidates selected, average distance, hit count
     */
    public String printCandidateAnalysis() {
        if (rankAnalysis == null)
            throw new Error("Candidate analysis was not run");
        String output = String.format("%10s\t%12s\t%12s\n", "Candidates", "Mean Error", "# Below Tol");
        for (double[] level : rankAnalysis) {
            output += String.format("%10.0f\t%12.5e\t%12.0f\n", level[0], level[1], level[2]);
        }
        return output;
    }
    
    /**
     * Print results of the tolerance size analysis.
     * @return A table containing for each row: Window size, # inside window, Fraction correct
     */
    public String printWindowAnalysis() {
        if (windowAnalysis == null)
            throw new Error("Window analysis was not run");
        String output = String.format("%12s\t%16s\t%16s\n", "Window Size", "# Inside Window", "Frac Below Tol");
        for (double[] window : windowAnalysis) {
            output += String.format("%12.5e\t%16.0f\t%16.8e\n", window[0], window[1], window[2]);
        }
        return output;
    }


    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty())
            return super.printCommand(Command);
        
        switch (Command.get(0).toLowerCase()) {
            case "rank":
                return printCandidateAnalysis();
            case "window":
                return printWindowAnalysis();
            default:
                return super.printCommand(Command);
        }
    }
    
    
}
