package magpie.attributes.generators.composition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class NearbyCompoundAttributeGeneratorTest {
    
    @Test
    public void testClosestCompounds() throws Exception {
        // Make a fake list of compunds
        List<CompositionEntry> others = new ArrayList<>();
        others.add(new CompositionEntry("H0.9He0.1"));
        others.add(new CompositionEntry("H0.8He0.2"));
        others.add(new CompositionEntry("H0.7He0.3"));
        others.add(new CompositionEntry("H0.6He0.4"));
        Collections.shuffle(others);
        
        // Find the 2 closest to H
        List<CompositionEntry> closest = 
                NearbyCompoundAttributeGenerator.getClosestCompositions(
                        new CompositionEntry("H"),
                        others,
                        2,
                        2);
        
        // Check results
        assertEquals(2, closest.size());
        assertEquals(new CompositionEntry("H0.9He0.1"), closest.get(0));
        assertEquals(new CompositionEntry("H0.8He0.2"), closest.get(1));
    }
    
}
