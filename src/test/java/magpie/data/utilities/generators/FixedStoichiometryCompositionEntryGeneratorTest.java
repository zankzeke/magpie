package magpie.data.utilities.generators;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class FixedStoichiometryCompositionEntryGeneratorTest {
    
    @Test
    public void testParseStoich() {
        // Add in some examples
        FixedStoichiometryCompositionEntryGenerator gen = 
                new FixedStoichiometryCompositionEntryGenerator();
        gen.addStoichiometry("NaCl");
        gen.addStoichiometry("A2B");
        gen.addStoichiometry("A2.5BCe");
        
        // Check results
        List<double[]> results = gen.getStiochiometries();
        assertEquals(3, results.size());
        assertArrayEquals(new double[]{1,1}, results.get(0), 1e-6);
        assertArrayEquals(new double[]{2,1}, results.get(1), 1e-6);
        assertArrayEquals(new double[]{2.5,1,1}, results.get(2), 1e-6);
    }
    
    @Test
    public void testAddElement() {
        // Add in some elements
        FixedStoichiometryCompositionEntryGenerator gen = 
                new FixedStoichiometryCompositionEntryGenerator();
        gen.addElement("Al");
        gen.addElement("Fe");
        gen.addElement("Ti");
        
        // Check results
        Set<String> results = gen.getElements();
        assertEquals(3, results.size());
        assertTrue(results.contains("Al"));
        assertTrue(results.contains("Fe"));
        assertTrue(results.contains("Ti"));
    }
    
    @Test
    public void testGeneration() throws Exception {
        // Make the generator
        FixedStoichiometryCompositionEntryGenerator gen = 
                new FixedStoichiometryCompositionEntryGenerator();
        
        List<Object> options = new ArrayList<>();
        options.add("AB");
        options.add("A2BC");
        options.add("-elems");
        options.add("Al");
        options.add("Ti");
        options.add("Zr");
        
        gen.setOptions(options);
        
        // Print the usage statement
        System.out.println(gen.printUsage());
        
        // Check results
        List<BaseEntry> lists = gen.generateEntries();
        assertEquals(3 + 3, lists.size());
        assertTrue(lists.contains(new CompositionEntry("TiAl")));
        assertTrue(lists.contains(new CompositionEntry("ZrAl")));
        assertTrue(lists.contains(new CompositionEntry("ZrTi")));
        assertTrue(lists.contains(new CompositionEntry("Al2TiZr")));
        assertTrue(lists.contains(new CompositionEntry("AlTi2Zr")));
        assertTrue(lists.contains(new CompositionEntry("AlTiZr2")));
    }
}
