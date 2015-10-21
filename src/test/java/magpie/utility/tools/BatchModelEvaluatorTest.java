package magpie.utility.tools;

import java.util.*;
import magpie.Magpie;
import magpie.analytics.RegressionStatistics;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.filters.AllMetalsFilter;
import magpie.data.utilities.generators.PhaseDiagramCompositionEntryGenerator;
import magpie.models.regression.WekaRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class BatchModelEvaluatorTest {

    @Test
    public void test() throws Exception {
        // Import some data
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        data.generateAttributes();
        data.setTargetProperty("delta_e", true);
        
        // Make a model
        WekaRegression weka = new WekaRegression("trees.REPTree", null);
        weka.train(data);
        
        // Clear out data and make a copy without attributes
        data.clearData();
        data.importText("datasets/small_set.txt", null);
        data.setTargetProperty("delta_e", true);
        
        // Run the model
        BatchModelEvaluator eval = new BatchModelEvaluator();
        eval.setModel(weka);
        eval.setBatchSize(100);
        eval.evaluate(data);
        
        // Make sure it gives the same results
        RegressionStatistics trainStats = (RegressionStatistics) weka.TrainingStats;
        RegressionStatistics runStats = new RegressionStatistics();
        runStats.evaluate(data);
        assertEquals(trainStats.MAE, runStats.MAE, 1e-6);
        
        // Run the model
        Magpie.NThreads = 2;
        eval = new BatchModelEvaluator();
        eval.setModel(weka);
        eval.setBatchSize(100);
        eval.evaluate(data);
        
        // Make sure it gives the same results
        trainStats = (RegressionStatistics) weka.TrainingStats;
        runStats = new RegressionStatistics();
        for (BaseEntry entry : data.getEntries()) {
            entry.deletePredictedClass();
        }
        runStats.evaluate(data);
        assertEquals(trainStats.MAE, runStats.MAE, 1e-6);
        
        // Test out the run and filter
        AllMetalsFilter filter = new AllMetalsFilter();
        filter.setExclude(false);
        
        PhaseDiagramCompositionEntryGenerator gen = new PhaseDiagramCompositionEntryGenerator();
        Set<String> elems = new TreeSet<>();
        elems.add("Fe");
        elems.add("Al");
        elems.add("O");
        gen.setElementsByName(elems);
        gen.setEvenSpacing(false);
        gen.setSize(2);
        gen.setOrder(2, 2);
        
        CompositionDataset outputData = data.emptyClone();
        outputData.clearData();
        
        eval.evaluate(outputData, gen, filter);
        
        assertEquals(1, outputData.NEntries());
        assertEquals(0, outputData.getEntry(0).NAttributes());
    }
    
}
