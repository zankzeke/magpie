package magpie.statistics.performance;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class RegressionStatisticsTest {
    @Test
    public void getStatistics() throws Exception {
        // Create test set
        double[] measured = new double[]{1, 2, 3};
        double[] predicted = new double[]{0.9, 2, 3.2};
        Dataset data = new Dataset();
        for (int i=0; i<3; i++) {
            BaseEntry entry = new BaseEntry();
            entry.setMeasuredClass(measured[i]);
            entry.setPredictedClass(predicted[i]);
            data.addEntry(entry);
        }

        // Compute statistics
        RegressionStatistics stats = new RegressionStatistics();
        stats.evaluate(data);

        //  Make sure statistics match up
        assertEquals(0.1, stats.MAE, 1e-6);
        assertEquals(1.0/18, stats.MRE, 1e-6);
        assertEquals(0.129099445, stats.RMSE, 1e-6);
        assertEquals(0.999685089, stats.R, 1e-6);
        assertEquals(1.0, stats.Rho, 1e-6);
        assertEquals(1.0, stats.Tau, 1e-6);
        assertEquals(1.0, stats.ROC_AUC, 1e-6);

        //  Make sure the population statistics match up
        assertEquals(0.816497, stats.MeasuredStdDev, 1e-6);
        assertEquals(2./3, stats.MeasuredMAD, 1e-6);
        assertEquals(2, stats.MeasuredRange, 1e-6);
        assertEquals(2, stats.MeasuredMedian, 1e-6);

        // Test the printing
        List<String> command = new LinkedList<>();
        command.add("baseline");
        System.out.println(stats.printCommand(command));
    }

}