package magpie.statistics.performance;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Logan Ward
 */
public class ClassificationStatisticsTest {

    @Test
    public void testLogLoss() throws Exception {
        // Make a test dataset for binary case
        int[] measured = new int[]{0, 0, 1};
        double[][] probs = new double[][]{new double[]{1, 0}, new double[]{0.5, 0.5}, new double[]{0.6, 0.4}};

        double logLoss = ClassificationStatistics.computeLogLoss(measured, probs);
        assertEquals(0.536479304144, logLoss, 1e-6);

        // Make a test case for ternary
        measured = new int[]{0, 0, 1, 2};
        probs = new double[][]{new double[]{1, 0, 0}, new double[]{0.4, 0.3, 0.3},
                new double[]{0.2, 0.2, 0.6}, new double[]{0.05, 0.05, 0.9}};
        logLoss = ClassificationStatistics.computeLogLoss(measured, probs);
        assertEquals(0.65777228999, logLoss, 1e-6);
    }

    @Test
    public void testStatistics() {
        // Create the dataset
        int[] measured = new int[]{0, 0, 1, 1};
        double[][] probs = new double[][]{new double[]{1, 0}, new double[]{0.6, 0.4}, new double[]{0.6, 0.4},
            new double[]{0.2, 0.8}};
        Dataset data = new Dataset();
        data.setClassNames(new String[]{"Yes", "No"});
        for (int i=0; i<measured.length; i++) {
            BaseEntry entry = new BaseEntry();
            entry.setMeasuredClass(measured[i]);
            entry.setClassProbabilities(probs[i]);
            data.addEntry(entry);
        }

        // Test the basic class statistics
        ClassificationStatistics stats = new ClassificationStatistics();
        stats.evaluate(data);
        assertEquals(3, stats.NumberCorrect);
        assertEquals(3./4, stats.FractionCorrect, 1e-6);
        assertEquals(0.5, stats.Kappa, 1e-6);
        assertEquals(2, stats.ContingencyTable[0][0]);
        assertEquals(1, stats.ContingencyTable[1][0]);
        assertEquals(0, stats.ContingencyTable[0][1]);
        assertEquals(1, stats.ContingencyTable[1][1]);
        assertArrayEquals(stats.ConfusionMatrix[0], stats.ContingencyTable[0]);
        assertEquals(2, stats.TP);
        assertEquals(1, stats.FP);
        assertEquals(0, stats.FN);
        assertEquals(1, stats.TN);
        assertEquals(1, stats.Sensitivity, 1e-6);
        assertEquals(0.5, stats.FPR, 1e-6);
        assertEquals(0.5, stats.Specificity, 1e-6);
        assertEquals(2./3, stats.PPV, 1e-6);
        assertEquals(1, stats.NPV, 1e-6);
        assertEquals(1./3, stats.FDR, 1e-6);
        assertEquals(0.57735026919, stats.MCC, 1e-6);
        assertEquals(0.8, stats.F1, 1e-6);
        assertEquals(0.875, stats.ROC_AUC, 1e-3);
        assertEquals(0.412564, stats.LogLoss, 1e-6);

        // Test the population statistics
        assertEquals(0.5, stats.MeasuredLargestClassFraction, 1e-6);
        assertEquals(0.6931471, stats.BaselineLogLoss, 1e-6);
    }
}