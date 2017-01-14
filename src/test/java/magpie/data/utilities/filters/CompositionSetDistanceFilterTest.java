package magpie.data.utilities.filters;

import java.util.ArrayList;
import java.util.List;
import magpie.Magpie;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import weka.core.Instance;

import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionSetDistanceFilterTest {

    @Test
    public void test() throws Exception {
        // Test distance from set
        List<CompositionEntry> entrySet = new ArrayList<>();
        entrySet.add(new CompositionEntry("NaCl"));
        entrySet.add(new CompositionEntry("Fe"));
        entrySet.add(new CompositionEntry("FeO"));

        // Make the filterer
        CompositionSetDistanceFilter filter = new CompositionSetDistanceFilter();
        filter.setUseManhattan();
        filter.addCompositions(entrySet);
        assertEquals(0, filter.computeDistance(new CompositionEntry("Fe")), 1e-6);
        filter.setUseEuclidean();
        assertEquals(Math.sqrt(2 * Math.pow(0.1, 2)), filter.computeDistance(new CompositionEntry("Fe2O3")), 1e-6);
        
        CompositionDataset toMeasureFrom = new CompositionDataset();
        toMeasureFrom.addEntries(entrySet);
        
        // Create filter
        List<Object> options = new ArrayList<>();
        options.add(toMeasureFrom);
        options.add("-euclidean");
        options.add(0.2);
        
        filter.setOptions(options);
        
        // Create test dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Al3N2");
        data.addEntry("Fe2O3");
        
        // Run filter with p == 2
        filter.setDistanceThreshold(0.2);
        assertFalse(filter.label(data)[0]);
        assertTrue(filter.label(data)[1]);
        assertFalse(filter.label(data)[2]);
        
        filter.setDistanceThreshold(0.1);
        assertFalse(filter.label(data)[0]);
        assertTrue(filter.label(data)[1]);
        assertTrue(filter.label(data)[2]);
        
        // Test parallelism
        boolean[] serialLabels = filter.label(data);
        Magpie.NThreads = 2;
        boolean[] parallelLabels = filter.parallelLabel(data);
        assertEquals(serialLabels.length, parallelLabels.length);
        for (int i=0; i<serialLabels.length; i++) {
            assertTrue(serialLabels[i] == parallelLabels[i]);
        }
    }

    @Test
    public void testEntryConversion() throws Exception {
        // Test the conversion of entry to Weka instance
        Instance inst = CompositionSetDistanceFilter.convertCompositionToInstance(new CompositionEntry("H"));
        assertEquals(1.0, inst.value(0), 1e-6);

        inst = CompositionSetDistanceFilter.convertCompositionToInstance(new CompositionEntry("UH3"));
        assertEquals(0.75, inst.value(0), 1e-6);
        assertEquals(0, inst.value(1), 1e-6);
        assertEquals(0.25, inst.value(91), 1e-6);
    }
    
}
