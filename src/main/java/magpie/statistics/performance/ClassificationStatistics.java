package magpie.statistics.performance;

import magpie.data.Dataset;
import org.apache.commons.math3.stat.StatUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>This class handles the calculation of statistics for classifiers. It implements the
 * "class cutoff" feature described in BaseClassifier. 
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class ClassificationStatistics extends BaseStatistics {
    /** Number of entries assigned to the correct class */
    public int NumberCorrect;
    /** Fraction of entries assigned to correct class */
    public double FractionCorrect;
    /** Fraction correct modified such that 0 is random guessing and 1 is perfect classification */
    public double Kappa;
    /** Contingency table (for binary cases) */
    public int[][] ContingencyTable;
    /** Description of how often instances are assigned to each class */
    public int[][] ConfusionMatrix;
    /** Number of True Positives */
    public int TP;
    /** Number of False Positives */
    public int FP;
    /** Number of False Negatives */
    public int FN;
    /** Number of True Negatives */
    public int TN;
    /** Sensitivity (aka True Positive Rate */
    public double Sensitivity;
    /** False Positive Rate */
    public double FPR;
    /** Accuracy */
    public double Accuracy;
    /** Specificity (True Negative Rate) */
    public double Specificity;
    /** Positive Predictive Value */
    public double PPV;
    /** Negative Predictive Value */
    public double NPV;
    /** False Detection Rate */
    public double FDR;
    /** Matthews Correlation Coefficient */
    public double MCC;
    /** F1 Score */
    public double F1;
    /**
     * Log-loss
     */
    public double LogLoss;
    /** Names of classes (used when printing) */
    public String[] ClassNames;

    /**
     * Takes a continuous class variable and converts it to discrete values that range between
     * [0,nclass).
     * @param x Array of continuous class variables to be adjusted
     * @param n_classes Number of discrete classes known
     * @return Class variables adjusted to exist only on integer values (see above)
     */
    static public int[] discretize(double[] x, int n_classes) {
        int[] y = new int[x.length];
        for (int i = 0; i < x.length; i++)
            if (x[i] < 0) y[i] = 0;
            else if (x[i] > n_classes - 1) y[i] = n_classes - 1;
            else {
                y[i] = (int) Math.round(x[i]);
            }
        return y;
    }

    /**
     * Compute the log-loss for the classifier.
     * @param trueLabels True labels for these predictions
     * @param probabilites Probability of membership in each class
     * @return Mean log loss over all entries
     */
    static protected double computeLogLoss(int[] trueLabels, double[][] probabilites) {
        double output = 0;
        for (int i = 0; i < trueLabels.length; i++) {
            int label = trueLabels[i];
            double prob = probabilites[i][label];
            output += Math.log(Math.max(prob, 1e-12)) / trueLabels.length;
        }
        return -1 * output;
    }

    @Override
    protected void evaluate_protected(Dataset data) {
        // Store basic statistics
        NumberTested = data.NEntries();
        NumberCorrect = 0;
        Measured = data.getMeasuredClassArray();
        Predicted = data.getPredictedClassArray();
        int[] predicted_discrete = discretize(Predicted, data.NClasses());
        ClassNames = data.getClassNames();

        // Build a contigency table
        ContingencyTable = new int[data.NClasses()][data.NClasses()];
        for (int i = 0; i < data.NEntries(); i++) {
            ContingencyTable[(int) Measured[i]][predicted_discrete[i]]++;
            if (Measured[i] == predicted_discrete[i]) {
                NumberCorrect++;
            }
        }
        FractionCorrect = (double) NumberCorrect / (double) NumberTested;
        Kappa = 1 - (1 - FractionCorrect) / (1 - StatUtils.max(data.getDistribution()));

        // Build a binary contingency table
        if (data.NClasses() == 2)
            ConfusionMatrix = ContingencyTable;
        else {
            ConfusionMatrix = new int[2][2];
            int actual, pred;
            for (int i = 0; i < data.NEntries(); i++) {
                actual = (Measured[i] == 0) ? 0 : 1;
                pred = (predicted_discrete[i] == 0) ? 0 : 1;
                ConfusionMatrix[pred][actual]++;
            }
        }

        // Use it to generate other statistics
        // See: http://en.wikipedia.org/wiki/Receiver_operating_characteristic
        TP = ConfusionMatrix[0][0];
        FP = ConfusionMatrix[0][1];
        FN = ConfusionMatrix[1][0];
        TN = ConfusionMatrix[1][1];
        Sensitivity = (double) TP/ (double) (TP+FN);
        FPR = (double) FP / (double) (FP + TN);
        Accuracy = (double) (TP + TN) / (double) data.NEntries();
        Specificity = 1.0 - FPR;
        PPV = (double) TP / (double) (TP + FP);
        NPV = (double) TN / (double) (TN + FN);
        FDR = (double) FP / (double) (FP + TP);
        MCC = (double) (TP*TN - FP*FN) / Math.sqrt((double) (TP+FN)*(FP+TN)
                *(TP+FP)*(FN+TN));
        F1 = (double) (2 * TP) / (double) (2*TP + FP + FN);

        // Compute the log-loss
        double[][] classProbs = data.getClassProbabilityArray();
        LogLoss = computeLogLoss(discretize(Measured, data.NClasses()), classProbs);

        // Calculate the reciever-operating characteristic curve
        double ranking[];
        ranking = new double[NumberTested];
        for (int i = 0; i < NumberTested; i++)
            ranking[i] = 1.0 - classProbs[i][0];
        getROCCurve(Measured, ranking, 50);
        ROC_AUC = integrateROCCurve(ROC);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ClassificationStatistics x = (ClassificationStatistics) super.clone();
        x.Accuracy = Accuracy;
        if (ConfusionMatrix != null) {
            x.ConfusionMatrix = new int[ConfusionMatrix.length][];
            for (int i=0; i<x.ConfusionMatrix.length; i++)
                x.ConfusionMatrix[i] = ConfusionMatrix[i].clone();
            x.ContingencyTable = new int[ContingencyTable.length][];
            for (int i=0; i<x.ContingencyTable.length; i++)
                x.ContingencyTable[i] = ContingencyTable[i].clone();
        }
        x.F1 = F1; x.FDR = FDR; x.FN = FN; x.FP = FP; x.FPR = FPR;
        x.FractionCorrect = FractionCorrect; x.Kappa = Kappa;
        x.MCC = MCC; x.NPV = NPV; x.NumberCorrect = NumberCorrect;
        x.PPV = PPV;
        x.LogLoss = LogLoss;
        return x;
    }
    
    @Override public String toString() {
        String out = String.format("Number Tested: %d\n", NumberTested);
        out=out+String.format("Number correct: %d (%.3f%%)\n",NumberCorrect,FractionCorrect*100)
                +String.format("Kappa: %.4f\n",Kappa)
                +String.format("Sensitivity: %.4f\n", Sensitivity)
                +String.format("FPR: %.4f\n", FPR)
                +String.format("Accuracy: %.4f\n", Accuracy)
                +String.format("PPV: %.4f\n", PPV)
                +String.format("NPV: %.4f\n", NPV)
                +String.format("FDR: %.4f\n", FDR)
                +String.format("MCC: %.4f\n", MCC)
                +String.format("F1: %.4f\n", F1)
                + String.format("Log-loss: %.4f\n", LogLoss)
                +String.format("ROC AUC: %.4f\n", ROC_AUC);
        return out;
    }

    @Override
    public Map<String, Double> getStatistics() {
        Map<String, Double> output = new TreeMap<>();
        
        output.put("NEvaluated", (double) NumberTested);
        output.put("NCorrect", (double) NumberCorrect);
        output.put("Kappa", Kappa);
        output.put("Sensitivity", Sensitivity);
        output.put("FPR", FPR);
        output.put("Accuracy", Accuracy);
        output.put("PPV", PPV);
        output.put("NPV", NPV);
        output.put("FDR", FDR);
        output.put("MCC", MCC);
        output.put("F1", F1);
        output.put("LogLoss", LogLoss);
        output.put("ROCAUC", ROC_AUC);
        
        return output;
    }
    
    /**
     * Print out the contingency table 
     * @return Formatted contingency table
     */
    public String printContingencyTable() {
        // Determine the width of fields
        int maxNameLength = ClassNames[0].length();
        for (int i=1; i<ClassNames.length; i++) {
            maxNameLength = Math.max(ClassNames[i].length(), maxNameLength);
        }
        for (int[] row : ContingencyTable) {
            for (int num : row) {
                maxNameLength = Math.max(maxNameLength,
                        Integer.toString(num).length());
            }
        }
        
        // Print out header
        String fieldStart = "%" + (maxNameLength + 1);
        String output = String.format(fieldStart + "s\tPredicted Class\n", " ");
        output += String.format(fieldStart + "s", "");
        for (String name : ClassNames) {
            output += String.format(fieldStart + "s", name);
        }
        output += "\n";
        
        // Print out data
        for (int i=0; i<ClassNames.length; i++) {
            output += String.format(fieldStart + "s", ClassNames[i]);
            for (int j=0; j<ClassNames.length; j++) {
                output += String.format(fieldStart + "d", ContingencyTable[i][j]);
            }
            output += "\n";
        }
        
        return output;
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) {
            return super.printCommand(Command);
        }
        
        // Get the action
        String action = Command.get(0).toLowerCase();
        switch (action) {
            case "contingency":
                return printContingencyTable();
            default:
                return super.printCommand(Command);
        }
    }
}
