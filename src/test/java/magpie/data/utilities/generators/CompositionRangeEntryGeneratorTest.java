package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionRangeEntryGeneratorTest {
    
    @Test
    public void testUsage() {
        System.out.println(new CompositionRangeEntryGenerator().printUsage());
    }

    @Test
    public void testTrivial() throws Exception {
        // Make generator
        List<Object> options = new ArrayList<>();
        options.add("Al");
        options.add("-alloy");
        options.add("Fe");
        options.add(0);
        options.add(0.1);
        options.add(0.2);
        
        CompositionRangeEntryGenerator gen = new CompositionRangeEntryGenerator();
        gen.setOptions(options);
        
        // Test results
        List<BaseEntry> results = gen.generateEntries();
        
        assertEquals(1, results.size());
        assertTrue(results.contains(new CompositionEntry("Al")));
    }
    
    @Test
    public void testBinary() throws Exception {
        // Make generator
        List<Object> options = new ArrayList<>();
        options.add("Al");
        options.add("-alloy");
        options.add("Fe");
        options.add(0);
        options.add(1);
        options.add(0.2);
        
        CompositionRangeEntryGenerator gen = new CompositionRangeEntryGenerator();
        gen.setOptions(options);
        
        // Test results
        List<BaseEntry> results = gen.generateEntries();
        
        assertEquals(6, results.size());
    }
    
    @Test
    public void testBinaryWithMinAndMax() throws Exception {
        // Make generator
        List<Object> options = new ArrayList<>();
        options.add("Al");
        options.add("-min");
        options.add(0.1);
        options.add("-max");
        options.add(0.9);
        options.add("-alloy");
        options.add("Fe");
        options.add(0);
        options.add(1);
        options.add(0.2);
        
        CompositionRangeEntryGenerator gen = new CompositionRangeEntryGenerator();
        gen.setOptions(options);
        
        // Test results
        List<BaseEntry> results = gen.generateEntries();
        
        assertEquals(4, results.size());
    }
    
    @Test
    public void testTernaryWithMinAndMax() throws Exception {
        // Make generator
        List<Object> options = new ArrayList<>();
        options.add("Al");
        options.add("-min");
        options.add(0.1);
        options.add("-max");
        options.add(0.9);
        options.add("-alloy");
        options.add("Fe");
        options.add(0);
        options.add(1);
        options.add(1f/3);
        options.add("-alloy");
        options.add("Ni");
        options.add(0);
        options.add(1);
        options.add(1f/3);
        
        CompositionRangeEntryGenerator gen = new CompositionRangeEntryGenerator();
        gen.setOptions(options);
        
        // Test results
        List<BaseEntry> results = gen.generateEntries();
        
        assertEquals(5, results.size());
    }
    
    @Test
    public void testBig() throws Exception {
        // Make generator
        List<Object> options = new ArrayList<>();
        options.add("Al");
        options.add("-min");
        options.add(0.45);
        options.add("-max");
        options.add(0.65);
        options.add("-alloy");
        options.add("Fe");
        options.add(0);
        options.add(1);
        options.add(0.03);
        options.add("-alloy");
        options.add("Ni");
        options.add(0);
        options.add(1);
        options.add(0.03);
        options.add("-alloy");
        options.add("Np");
        options.add(0);
        options.add(1);
        options.add(0.03);
        options.add("-alloy");
        options.add("Zr");
        options.add(0);
        options.add(0.5);
        options.add(0.03);
        
        CompositionRangeEntryGenerator gen = new CompositionRangeEntryGenerator();
        gen.setOptions(options);
        
        // Test results
        List<BaseEntry> results = gen.generateEntries();
        
        // Make sure all are within bounds
        for (BaseEntry entry : results) {
            CompositionEntry comp = (CompositionEntry) entry;
            assertTrue(StatUtils.min(comp.getFractions()) > 0);
            assertFalse(comp.getElementFraction("Al") < 0.449999); // Argh, floats!
            assertFalse(comp.getElementFraction("Al") > 0.650001);
            assertFalse(comp.getElementFraction("Zr") > 0.650001);
        }
    }
    
}
