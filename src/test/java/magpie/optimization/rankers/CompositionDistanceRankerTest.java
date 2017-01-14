package magpie.optimization.rankers;

import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Logan Ward
 */
public class CompositionDistanceRankerTest {

    @Test
    public void test() throws Exception {
        // Create the set from which you should measure distance
        CompositionDataset measureSet = new CompositionDataset();
        measureSet.addEntry("Na");
        measureSet.addEntry("NaCl");

        // Create the set to be ranked
        CompositionDataset rankSet = new CompositionDataset();
        rankSet.addEntry("Na3Cl");
        rankSet.addEntry("Na5Cl4");

        // Create the ranker
        CompositionDistanceRanker ranker = new CompositionDistanceRanker();

        List<Object> options = new ArrayList<>();
        options.add(measureSet);
        options.add("manhattan");

        ranker.setOptions(options);

        // Test the distance measure
        assertEquals(0, ranker.objectiveFunction(new CompositionEntry("Na")), 1e-6);
        assertEquals(0.5, ranker.objectiveFunction(new CompositionEntry("Na3Cl")), 1e-6);
        ranker.setUseManhattan(false);
        assertEquals(Math.sqrt(0.125), ranker.objectiveFunction(new CompositionEntry("Na3Cl")), 1e-6);

        // Test the ranking
        ranker.setMaximizeFunction(true);
        int[] ranks = ranker.rankEntries(rankSet);
        assertArrayEquals(new int[]{0,1}, ranks);
    }
}