package magpie.optimization.rankers;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Logan Ward
 */
public class BaseEntryRankerTest {

    /**
     * Make an example dataset
     *
     * @return Test dataset
     */
    public Dataset generateDataset() {
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());

        // Add example dataset
        data.setMeasuredClasses(new double[]{0, 1, 2});
        data.setPredictedClasses(new double[]{2, 1, 0});

        return data;
    }

    @Test
    public void sortByRanking() throws Exception {
        Dataset data = generateDataset();

        // Make a ranker
        SimpleEntryRanker ranker = new SimpleEntryRanker();
        ranker.setMaximizeFunction(false);
        ranker.setUseMeasured(false);

        // Test the ranking
        assertArrayEquals(new double[]{0, 1, 2}, data.getMeasuredClassArray(), 1e-6);
        ranker.sortByRanking(data);
        assertArrayEquals(new double[]{2, 1, 0}, data.getMeasuredClassArray(), 1e-6);
    }
}