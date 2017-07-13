package magpie.statistics.performance;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Logan Ward
 */
public class ClassificationStatisticsTest {
    @Test
    public void computeLogLoss() throws Exception {
        // Make a test dataset for binary case
        int[] mesaured = new int[]{0, 0, 1};
        double[][] probs = new double[][]{new double[]{1, 0}, new double[]{0.5, 0.5}, new double[]{0.6, 0.4}};

        double logLoss = ClassificationStatistics.computeLogLoss(mesaured, probs);
        assertEquals(0.536479304144, logLoss, 1e-6);

        // Make a test case for ternary
        mesaured = new int[]{0, 0, 1, 2};
        probs = new double[][]{new double[]{1, 0, 0}, new double[]{0.4, 0.3, 0.3},
                new double[]{0.2, 0.2, 0.6}, new double[]{0.05, 0.05, 0.9}};
        logLoss = ClassificationStatistics.computeLogLoss(mesaured, probs);
        assertEquals(0.65777228999, logLoss, 1e-6);
    }

}