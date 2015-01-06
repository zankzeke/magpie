package magpie.analytics;

import java.util.Arrays;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.*;

/**
 * <p>This class handles the calculation of statistics for classifiers. It implements the
 * "class cutoff" feature described in BaseClassifier. 
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class ClassificationStatistics extends BaseStatistics {
    /** For binary cases: Probability below above which an entry has class = 0 */
    protected double class_cutoff=0.5;
    /** Whether class variable should be treated as discrete */
    protected boolean discrete_class=false;
    
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
    
    @Override public void evaluate(Dataset Data) {
        NumberTested = Data.NEntries(); NumberCorrect=0;
        double[] measured = Data.getMeasuredClassArray(),
                predicted = Data.getPredictedClassArray();
        double[] predicted_discrete = applyClassCutoff(predicted, Data.NClasses());
        
        // Build a contigency table
        ContingencyTable = new int[Data.NClasses()][Data.NClasses()];
        for (int i=0; i<Data.NEntries(); i++) {
            ContingencyTable[(int)measured[i]][(int)predicted_discrete[i]]++;
            if (measured[i]==predicted_discrete[i])
                NumberCorrect++;
        }
        FractionCorrect = (double) NumberCorrect / (double) NumberTested;
        Kappa = 1 - (1-FractionCorrect) / (1-StatUtils.max(Data.getDistribution()));
        
        // Build a binary contingency table
        if (Data.NClasses()==2)
            ConfusionMatrix = ContingencyTable;
        else {
            ConfusionMatrix = new int[2][2];
            int actual, pred;
            for (int i=0; i<Data.NEntries(); i++) {
                actual = (measured[i] == 0) ? 0 : 1;
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
        FPR = (double) FP/ (double) (FP+TN);
        Accuracy = (double) (TP+FN) / (double) Data.NEntries();
        Specificity = 1.0 - FPR;
        PPV = (double) TP / (double) (TP + FP);
        NPV = (double) TN / (double) (TN + FN);
        FDR = (double) FP / (double) (FP + TP);
        MCC = (double) (TP*TN - FP*FN) / Math.sqrt((double) (TP+FN)*(FP+TN)
                *(TP+FP)*(FN+TN));
        F1 = (double) (2 * TP) / (double) (2*TP + FP + FN);
        
        // Calculate the reciever-operating characteristic curve
        if (classIsDiscrete()) {
            // Most probable cases have a probability of class 1 close to one
            double classprobs[][], ranking[]; 
            ranking = new double[NumberTested];
            classprobs = Data.getClassProbabilityArray();
            for (int i=0; i<NumberTested; i++)
                ranking[i]=1.0-classprobs[i][0];
            getROCCurve(measured, ranking, 50);
        } else {
            double[] predicted_adj = Arrays.copyOf(predicted, Data.NEntries());
            for (int i=0; i<measured.length; i++) 
                if (predicted_adj[i] < 0) predicted_adj[i] = 0.0;
                else if (predicted_adj[i] > Data.NClasses() - 1)
                    predicted_adj[i] = Data.NClasses() - 1;
            getROCCurve(measured, predicted_adj, 50);
        }
        ROC_AUC = integrateROCCurve(ROC);
    }
    
    /** 
     * Set the class cutoff used when calculating statistics. 
     * @param x Class cutoff (0 <= x <= 1)
     */
    public void setClassCutoff(double x) {
        this.class_cutoff = x;
    }
    /**
     * @return Class cutoff used during discretization  
     */
    public double getClassCutoff() { return class_cutoff; }
    /** 
     * @return Whether the class variable is discrete 
     */
    public boolean classIsDiscrete() { return discrete_class; }
    /** Set that the class variable is discrete */
    public void setClassDiscrete() { discrete_class = true; }
    /** Set that the class variable is continuous */
    public void setClassContinuous() { discrete_class = false; }
    
    /** Takes a continuous class variable and converts it to discrete values that range between
     * [0,nclass). For an instance in x that ranges between 0 and  <code>nclasses-1</code>, the following rule 
     * is applied: <br>
     * <p><code> x[i]-floor(x[i]) > cutoff ? ceil(x[i]) : floor(x[i]) </code></p>
     * <p>If that value is below 0, it is set to be zero. And, correspondingly, if it is greater than 
     * <code>nclasses-1</code> it is set to <code>nclasses-1</code>. The cutoff is 
     * taken from the instance of the ClassificationStatistics.</p>
     * @param x Array of continuous class variables to be adjusted
     * @param nclasses Number of discrete classes known
     * @return Class variables adjusted to exist only on integer values (see above)
     */
    public double[] applyClassCutoff(double[] x, int nclasses) {
        double[] y = new double[x.length];
        for (int i=0; i<x.length; i++)
            if (x[i]<0) y[i]=0;
            else if (x[i]>nclasses-1) y[i]=(double) nclasses - 1;
            else {
                y[i] = x[i]%1>class_cutoff ? Math.ceil(x[i]) : Math.floor(x[i]);
            }
        return y;
    }
    
    @Override public Object clone() throws CloneNotSupportedException {
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
                +String.format("ROC AUC: %.4f\n", ROC_AUC);
        return out;
    }
}
