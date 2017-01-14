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
public class CompositionDistanceFilterTest {

    @Test
    public void test() throws Exception {
        // Make a sample dataset
        CompositionDataset toLabel = new CompositionDataset();
        toLabel.addEntry("Fe50Al50");
        toLabel.addEntry("Fe55Al45");
        toLabel.addEntry("Fe25Al75");
        
        // Create filter
        CompositionDistanceFilter filter = new CompositionDistanceFilter();
        
        List<Object> options = new ArrayList<>();
        options.add("Fe49Al51");
        options.add(1.1);
        filter.setOptions(options);
        
        // Test #1
        boolean[] res = filter.label(toLabel);
        
        assertTrue(res[0]);
        assertFalse(res[1]);
        assertFalse(res[2]);
        
        // Test #2
        filter.setDistanceThreshold(7);
        
        res = filter.label(toLabel);
        
        assertTrue(res[0]);
        assertTrue(res[1]);
        assertFalse(res[2]);
    }

    @Test
    public void testDistances() throws Exception {
        // Test binary distance
        assertEquals(0.0,
                CompositionDistanceFilter.computeDistance(
                        new CompositionEntry("Fe"),
                        new CompositionEntry("Fe"), -1),
                1e-6);
        assertEquals(0.0,
                CompositionDistanceFilter.computeDistance(
                        new CompositionEntry("Fe"),
                        new CompositionEntry("Fe"), 0),
                1e-6);
        assertEquals(0.0,
                CompositionDistanceFilter.computeDistance(
                        new CompositionEntry("Fe"),
                        new CompositionEntry("Fe"), 2),
                1e-6);
        assertEquals(0.5,
                CompositionDistanceFilter.computeDistance(
                        new CompositionEntry("FeO"),
                        new CompositionEntry("Fe"), -1),
                1e-6);
        assertEquals(2.0,
                CompositionDistanceFilter.computeDistance(
                        new CompositionEntry("FeO"),
                        new CompositionEntry("Fe"), 0),
                1e-6);
        assertEquals(Math.sqrt(0.5),
                CompositionDistanceFilter.computeDistance(
                        new CompositionEntry("FeO"),
                        new CompositionEntry("Fe"), 2),
                1e-6);
    }
    
}
