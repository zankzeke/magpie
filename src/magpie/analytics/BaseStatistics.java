/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.analytics;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;
import magpie.utility.interfaces.Printable;
import org.apache.commons.math3.stat.StatUtils;

/**
 *
 * This class stores basic statistics about model performance and contains methods 
 * to calculate each of these statistics.
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
 * @author Logan Ward
 * @version 1.0
 */

abstract public class BaseStatistics extends java.lang.Object implements
        java.io.Serializable, java.lang.Cloneable, Printable, Options, Commandable {
    
    // Statistics useful regardless the type of model
    /** Number of entries evaluated */
    public int NumberTested=0;  
    /** Receiver operating characteristic curve*/
    public double[][] ROC;
    /** Area under receiver operating characteristic curve */
    public double ROC_AUC;

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
    
    /** Generates statistics about the performance on a model.
     * @param Results Dataset containing both measured and predicted classes.
     */
    abstract public void evaluate(Dataset Results);
    
    /** 
     * Generate the receiver operating characteristic curve based on the measured
     * and predicted variables for many instances.
     * 
     * @param measured Measured class variable (is returned sorted)
     * @param predicted Predicted class variable (is returned sorted)
     * @param NSteps Number of steps in curve
     */
    protected void getROCCurve(double[] measured, double[] predicted, int NSteps) {
        // Initialize the ROC curve
        ROC = new double[NSteps+2][3];
        double min = Math.min(StatUtils.min(measured),StatUtils.min(predicted)),
                max = Math.max(StatUtils.max(measured),StatUtils.max(predicted));
        ROC[0][0] = min;
        ROC[NSteps+1][0] = max;
        ROC[NSteps+1][1]=1; ROC[NSteps+1][2]=1;
        
        // Will step along fractions of the array
        double[] predicted_sorted = Arrays.copyOf(predicted, predicted.length);
        Arrays.sort(predicted_sorted);
        int step_size = measured.length / (NSteps + 1);
        for (int i=1; i<=NSteps; i++) {
            double cutoff = predicted_sorted[step_size * i];
            double TP=0, P=0, FP=0, N=0;
            for (int j=0; j<measured.length; j++) {
                TP += measured[j]<=cutoff && predicted[j]<=cutoff ? 1 : 0;
                FP += measured[j]>cutoff && predicted[j]<=cutoff ? 1 : 0;
                if (measured[j]<=cutoff) P++; else N++;
            }
            ROC[i][0]=cutoff;
            ROC[i][1]= N > 0 ? FP/N : 1.0; // False positive rate
            ROC[i][2]= P > 0 ? TP/P : 0.0; // Sensitivity
        }
        
        ROC_AUC = integrateROCCurve(ROC);
    }
    
     /** Integrate area between ROC curve and random guessing. Note that random guessing is
     * defined by the line where FPR = Sensitivity.
     * @param ROC Receiver operating characteristic curve data (see getROCCurve)
     * @return Area between ROC Curve and random guessing
     */
    public static double integrateROCCurve(double[][] ROC) {
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
                Sensitivity.set(FPR.size()-1, ROC[i][2]);
        }
        // Do simple trapizoid integration from the minimimum (0) to the max (1)
        for (int i = 0; i < FPR.size() - 1; i++) {
            value += (Sensitivity.get(i) + Sensitivity.get(i + 1)) / 2.0 * (FPR.get(i + 1) - FPR.get(i));
        }
        return (value - 0.5) / 0.5;
    }
    
    /** 
     * Print the ROC Curve as a plottable string. Format for each row:<br><br>
     * [Cutoff] [False Positive Rate] [Sensitivity]
     * @return ROC curve for model. 
     */
    public String printROCCurve() {
        if (ROC == null) { throw new Error("ROC Curve not yet calculated"); }
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
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return about();
        switch(Command.get(0).toLowerCase()) {
            case "stats":
                return toString();
            case "roc":
                return printROCCurve();
            default:
                throw new Exception("ERROR: Print command \"" + Command.get(0)
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
}
