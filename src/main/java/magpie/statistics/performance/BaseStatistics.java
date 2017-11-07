package magpie.statistics.performance;

import magpie.data.Dataset;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;
import magpie.utility.interfaces.Printable;
import magpie.utility.interfaces.Savable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;

import java.io.PrintWriter;
import java.util.*;

/**
 *
 * This class stores basic statistics about model performance and contains methods 
 * to calculate each of these statistics.
 * 
 * <p>Implementations of this case need to supply {@linkplain #evaluate(magpie.data.Dataset)}, 
 *  which actually performs the statistical calculations. Make sure to store the 
 *  measurements and predicted class variables! It is also up to the implementation
 *  to provide some way of storing results and printing them using the toString() operation.
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * <command><p><b>evaluate $&lt;dataset></b> - Evaluate measured vs. predicted class of entries<br>
 * <pr><i>dataset</i>: {@linkplain Dataset} object to be evaluated.</command>
 * 
 * <p><b><u>Implemented Print Commands:</u></b>
 * 
 * <print><p><b>stats</b> - Print out all statistics</print>
 * 
 * <print><p><b>roc</b> - Print out Receiver Operating Characteristic curve</print>
 *
 * <print><p><b>baseline</b> - Print statistics about the training data</print>
 *
 * 
 * <p><b><u>Implemented Save Commands</u></b></u>
 * 
 * <save><p><b>data</b> - Save predicted and measured class values used to compute
 * statistics</save>
 * 
 * @author Logan Ward
 */
abstract public class BaseStatistics implements java.io.Serializable, 
        java.lang.Cloneable, Printable, Options, Commandable, Savable {
    /** Number of entries evaluated */
    public int NumberTested=0;  
    /** Receiver operating characteristic curve*/
    public double[][] ROC;
    /**
     * Area under receiver operating characteristic curve normalized such that 1.0
     * is a perfect classifier and 0.0 is a perfectly-random classifier.
     */
    public double ROC_AUC;
    /** Measured value of class variable */
    protected double[] Measured;
    /** Predicted value of class variable */
    protected double[] Predicted;

    /**
     * Integrate area under the between ROC curve.
     * @return Area between ROC Curve and x-axis. 1 is a perfect classifier, 0 is a perfectly-bad classifier, and 0.5
     * is equivalent to random guessing.
     */
    private void integrateROCCurve() {
        double value = 0;
        double last;

        // Get strictly increasing list of values.
        List<Double> FPR = new LinkedList<>();
        List<Double> Sensitivity = new LinkedList<>();
        FPR.add(ROC[0][1]);
        Sensitivity.add(ROC[0][1]);
        last = ROC[0][1];
        for (int i = 0; i < ROC.length; i++) {
            if (ROC[i][1] > last) {
                FPR.add(ROC[i][1]);
                Sensitivity.add(ROC[i][2]);
                last = ROC[i][1];
            } else if (ROC[i][1] == last)
                Sensitivity.set(FPR.size() - 1, ROC[i][2]);
        }
        // Do simple trapezoid integration from the minimum (0) to the max (1)
        for (int i = 0; i < FPR.size() - 1; i++) {
            value += (Sensitivity.get(i) + Sensitivity.get(i + 1)) / 2.0 * (FPR.get(i + 1) - FPR.get(i));
        }

        // Set the value for the area under the curve
        ROC_AUC = value;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        /** Nothing to do */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    @Override public Object clone() throws CloneNotSupportedException {
        BaseStatistics x = (BaseStatistics) super.clone();
        x.NumberTested = NumberTested;
        x.ROC_AUC = ROC_AUC;
        if (ROC != null) {
            x.ROC = new double[ROC.length][];
            for(int i=0; i<ROC.length; i++)
                x.ROC[i] = ROC[i].clone();
        }
        return x;
    }

    /**
     * Generates statistics about the performance on a model.
     * @param results Dataset containing both measured and predicted classes.
     */
    final public void evaluate(Dataset results) {
        if (results.NEntries() == 0) {
            throw new IllegalArgumentException("Results dataset is empty.");
        }
        evaluate_protected(results);
    }

    /**
     * Internal method for acutally computing results. {@linkplain #evaluate(Dataset)} contains
     * some error-checking code
     *
     * @param results Dataset to be evaluated
     */
    abstract protected void evaluate_protected(Dataset results);

    /**
     * Generate the receiver operating characteristic curve based on the measured
     * and score variables for many instances.
     *
     * Computes the ROC curve using the False Positive Rate for the x-axis and True Positive Rate for the y-axis.
     * Assumes that the positive label is 0.
     *
     * Stores the result in {@linkplain #ROC}
     *
     * @param measured Measured class variable
     * @param score Classification score. Higher score means 'more likely to be positive class'
     * @param maxSteps Number of steps in curve
     */
    protected void getROCCurve(int[] measured, double[] score, int maxSteps) {
        // Set number of steps to be at most the number
        int nSteps = Math.min(maxSteps, measured.length);

        // Initialize the ROC curve
        ROC = new double[nSteps+2][3];
        ROC[0][0] = Double.POSITIVE_INFINITY; // Best possible score
        ROC[nSteps][0] = Double.NEGATIVE_INFINITY; // Worst possible score
        ROC[nSteps][1] = 1;
        ROC[nSteps][2] = 1;

        // Count the number of true and negative glasses
        int P = 0;
        for (int i=0; i<measured.length; i++) {
            if (measured[i] == 0) {
                P++;
            }
        }
        int N = measured.length - P;

        // Gradually increase the cutoff
        double step_size = 100 / (nSteps+1);
        for (int i=0; i<nSteps; i++) {
            double cutoff = StatUtils.percentile(score, 100 - step_size * (i+1));
            double TP=0, FP=0;
            for (int j=0; j<measured.length; j++) {
                TP += measured[j] == 0 && score[j]>=cutoff ? 1 : 0;
                FP += measured[j] != 0 && score[j]>=cutoff ? 1 : 0;
            }
            ROC[i+1][0]=cutoff;
            ROC[i+1][1]= N > 0 ? FP/N : 1.0; // False positive rate
            ROC[i+1][2]= P > 0 ? TP/P : 0.0; // True positive rate
        }

        integrateROCCurve();
    }
    
    /** 
     * Print the ROC Curve as a plottable string. Format for each row:<br><br>
     * [Cutoff] [False Positive Rate] [Sensitivity]
     * @return ROC curve for model. 
     */
    public String printROCCurve() {
        if (ROC == null) {
            throw new RuntimeException("ROC Curve not yet calculated");
        }
        String Output = "";
        Output += String.format("%11s\t%11s\t%11s\n","Value","FPR","Sensitivity");
        for (int i=0; i<ROC.length; i++) {
            Output += String.format("%.5e\t%.5e\t%.5e\n", ROC[i][0], ROC[i][1], ROC[i][2]);
        }
        return Output;
    }
    
    @Override
    public String about() {
        // Print the number entries tested
        return "Number Tested: " + this.NumberTested;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Print statistics about the measured class values. Useful as context for interpreting how good the
     * performance statistics actually are
     * @return String listing all of the measured baseline statistics
     */
    abstract public String printBaselineStats();

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return about();
        switch(Command.get(0).toLowerCase()) {
            case "stats":
                return toString();
            case "roc":
                return printROCCurve();
            case "baseline":
                return printBaselineStats();
            default:
                throw new IllegalArgumentException("Print command \"" + Command.get(0)
                        + "\" not recognized");
        }
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println(about()); 
            return null;
        }
        String Action = Command.get(0).toString();
        switch (Action.toLowerCase()) {
            case "evaluate": case "eval": {
                    // Usage: evaluate $<dataset>
                    // Generate statistics based on some dataset
                    if (Command.size() != 2 || ! (Command.get(1) instanceof Dataset))
                        throw new Exception("Usage: evaluate $<dataset>");
                    Dataset Data = (Dataset) Command.get(1);
                    if (Data.NEntries() == 0) throw new Exception("ERROR: "
                            + "Dataset contains no data");
                    if (! (Data.getEntry(0).hasMeasurement() && Data.getEntry(0).hasPrediction()))
                            throw new Exception("ERROR: Dataset must contain both measured and predicted class variables");
                    evaluate(Data);
                    System.out.println("\tCalculated statistics from " + Data.NEntries() + " entries");
                } break;
           default:
                throw new Exception("ERROR: Statistics command " + Action
                        + " not recognized.");
        }
        return null;
    }

    @Override
    public String saveCommand(String Basename, String Format) throws Exception {
        if (Format.equalsIgnoreCase("data")) {
            String filename = Basename + ".csv";
            savePerformanceData(filename);
            return filename;
        }
        throw new Exception("Format not supported: " + Format);
    }

    /**
     * Write out measured and predicted class variables used to compute statistics.
     * Prints into csv format.
     * @param filename Name of output file
     */
    public void savePerformanceData(String filename) throws Exception {
        if (NumberTested == 0) {
            throw new Exception("No performance data");
        }
        PrintWriter fp = new PrintWriter(filename);
        fp.println("measured,predicted");
        for (int i=0; i<Measured.length; i++) {
            fp.format("%.7e,%.7e\n", Measured[i], Predicted[i]);
        }
        fp.close();
    }
    
    /**
     * Retrieve list of statistics stored in this class.
     * @return Map of statistic name to value.
     */
    abstract public Map<String, Double> getStatistics();

    /**
     * Get statistics, except for values that are NaNs or infinite
     *
     * @return
     */
    public Map<String, Double> getStatisticsNoNaNs() {
        Map<String, Double> output = getStatistics();
        Iterator<Map.Entry<String, Double>> iter = output.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Double> next = iter.next();
            if (Double.isNaN(next.getValue()) || Double.isInfinite(next.getValue())) {
                iter.remove();
            }
        }
        return output;
    }
}
