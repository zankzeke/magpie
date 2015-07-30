package magpie.data.utilities.filters;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionSetDistanceFilterTest {

    @Test
    public void test() throws Exception {
        // Test binary distance
        assertEquals(0.0, 
                CompositionSetDistanceFilter.computeDistance(
                        new CompositionEntry("Fe"),
                        new CompositionEntry("Fe"), -1),
                1e-6);
        assertEquals(0.0, 
                CompositionSetDistanceFilter.computeDistance(
                        new CompositionEntry("Fe"),
                        new CompositionEntry("Fe"), 0),
                1e-6);
        assertEquals(0.0, 
                CompositionSetDistanceFilter.computeDistance(
                        new CompositionEntry("Fe"),
                        new CompositionEntry("Fe"), 2),
                1e-6);
        assertEquals(0.5, 
                CompositionSetDistanceFilter.computeDistance(
                        new CompositionEntry("FeO"),
                        new CompositionEntry("Fe"), -1),
                1e-6);
        assertEquals(2.0, 
                CompositionSetDistanceFilter.computeDistance(
                        new CompositionEntry("FeO"),
                        new CompositionEntry("Fe"), 0),
                1e-6);
        assertEquals(Math.sqrt(0.5), 
                CompositionSetDistanceFilter.computeDistance(
                        new CompositionEntry("FeO"),
                        new CompositionEntry("Fe"), 2),
                1e-6);
        
        // Test distance from set
        List<CompositionEntry> entrySet = new ArrayList<>();
        entrySet.add(new CompositionEntry("NaCl"));
        entrySet.add(new CompositionEntry("Fe"));
        entrySet.add(new CompositionEntry("FeO"));
        assertEquals(0, CompositionSetDistanceFilter.computeDistance(entrySet,
                new CompositionEntry("Fe"), 0), 1e-6);
        assertEquals(2.0, CompositionSetDistanceFilter.computeDistance(entrySet,
                new CompositionEntry("Fe2O3"), 0), 1e-6);
        assertEquals(0.1, CompositionSetDistanceFilter.computeDistance(entrySet,
                new CompositionEntry("Fe2O3"), -1), 1e-6);
        assertEquals(Math.sqrt(0.02), CompositionSetDistanceFilter.computeDistance(entrySet,
                new CompositionEntry("Fe2O3"), 2), 1e-6);
        
        // Create filter
        CompositionSetDistanceFilter filter = new CompositionSetDistanceFilter();
        filter.addCompositions(entrySet);
        filter.setP(2);
        
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
    }
    
}
