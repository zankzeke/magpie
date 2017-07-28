package magpie.optimization.rankers;

import magpie.data.BaseEntry;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author
 */
public class AdaptiveScalarizingEntryRankerTest {

    @Test
    public void test() throws Exception {
        MultiPropertyDataset data = new MultiPropertyDataset();
        for (int i = 0; i < 3; i++) {
            data.addEntry(new MultiPropertyEntry());
        }

        // Add in the properties
        data.addProperty("x");
        data.addProperty("y");
        for (BaseEntry e : data.getEntries()) {
            ((MultiPropertyEntry) e).addProperty();
            ((MultiPropertyEntry) e).addProperty();
        }

        data.setTargetProperty(0, true);
        data.setMeasuredClasses(new double[]{0, 1, 2});
        data.setPredictedClasses(new double[]{2, 1, 0});

        data.setTargetProperty(1, true);
        data.setMeasuredClasses(new double[]{2, 1, 0});
        data.setPredictedClasses(new double[]{0, 1, 2});

        // Generate the ranker where the two objectives are the same
        AdaptiveScalarizingEntryRanker ranker = new AdaptiveScalarizingEntryRanker();

        System.out.println(ranker.printUsage());

        List<Object> options = new LinkedList<>();
        options.add(0); // p - weight factor

        options.add("-opt");
        options.add("maximize");
        options.add("x");
        options.add("SimpleEntryRanker");

        options.add("-opt");
        options.add("minimize");
        options.add("y");
        options.add("SimpleEntryRanker");

        ranker.setOptions(options);

        // Test it (making sure the predicted / measured works)
        ranker.setUseMeasured(true);
        ranker.train(data);
        assertArrayEquals(new int[]{2, 1, 0}, ranker.rankEntries(data));

        ranker.setUseMeasured(false);
        ranker.train(data);
        assertArrayEquals(new int[]{0, 1, 2}, ranker.rankEntries(data));

        // Make the objectives contradictory (p = 0)
        options.set(2, "minimize");
        ranker.setOptions(options);

        ranker.train(data);
        assertEquals(1, ranker.rankEntries(data)[0]); // 0/2 are equally bad

        // Make p=1, check the values
        options.set(0, 4);
        ranker.setOptions(options);

        ranker.train(data);
        assertEquals(1f + 4f / 2f * 1, ranker.objectiveFunction(data.getEntry(0)), 1e-6);
        assertEquals(0.5 + 4f / 2f * 1, ranker.objectiveFunction(data.getEntry(1)), 1e-6);
        assertEquals(1 + 4f / 2f * 1, ranker.objectiveFunction(data.getEntry(2)), 1e-6);
    }
}