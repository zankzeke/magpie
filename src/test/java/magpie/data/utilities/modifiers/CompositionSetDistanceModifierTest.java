package magpie.data.utilities.modifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.generators.PhaseDiagramCompositionEntryGenerator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionSetDistanceModifierTest {    

    @Test
    public void basicTest() throws Exception {
        // Make a sample CompositioDataset
        CompositionDataset toMeasure = new CompositionDataset();
        toMeasure.addEntry("Na0.55Cl0.45");
        
        CompositionDataset comparisonSet = new CompositionDataset();
        comparisonSet.addEntry("Na2Cl");
        
        // Make the filter
        CompositionSetDistanceModifier mdfr = new CompositionSetDistanceModifier();
        
        List<Object> options = new ArrayList<>();
        options.add(comparisonSet);
        options.add(2);
        
        mdfr.setOptions(options);
        
        // Print out usage
        System.out.println(mdfr.printUsage());
        
        // Run the modifier
        mdfr.transform(toMeasure);
        
        // Check number of properties
        assertTrue(toMeasure.hasProperty("compdistance"));
        assertEquals(1, toMeasure.NProperties());
        assertEquals(1, toMeasure.NEntries());
        assertEquals(1, toMeasure.getEntry(0).NProperties());
        assertTrue(toMeasure.getEntry(0).hasMeasuredProperty(0));
        assertTrue(toMeasure.getEntry(0).hasPredictedProperty(0));
        
        // Check property values
        assertEquals(Math.sqrt((1f/3 - 0.45) * (1f/3 - 0.45)  + (2f/3 - 0.55) * (2f/3 - 0.55)), 
                toMeasure.getEntry(0).getMeasuredProperty(0), 1e-6);
        
        // Change training set
        mdfr.clearCompositions();
        
        Collection<CompositionEntry> newComps = new ArrayList<>();
        newComps.add(new CompositionEntry("NaCl"));
        newComps.add(new CompositionEntry("NaCl"));
        newComps.add(new CompositionEntry("Na3Cl"));
        newComps.add(new CompositionEntry("NaCl3"));
        
        mdfr.addCompositions(newComps);
        
        assertEquals(3, mdfr.Compositions.size());
        
        // Recompute P=-1 norm
        mdfr.setP(-1);
        
        mdfr.transform(toMeasure);
        
        // Check number of properties
        assertTrue(toMeasure.hasProperty("compdistance"));
        assertEquals(1, toMeasure.NProperties());
        assertEquals(1, toMeasure.NEntries());
        assertEquals(1, toMeasure.getEntry(0).NProperties());
        assertTrue(toMeasure.getEntry(0).hasMeasuredProperty(0));
        assertTrue(toMeasure.getEntry(0).hasPredictedProperty(0));
        
        // Check property values
        assertEquals(0.05, toMeasure.getEntry(0).getMeasuredProperty(0), 1e-6);
    }
    
    @Test
    public void testParallel() throws Exception {
        // Make a huge set to be measured
        PhaseDiagramCompositionEntryGenerator gen = new PhaseDiagramCompositionEntryGenerator();
        
        Set<Integer> elems = new HashSet<>();
        for (int e=0; e<30; e++) {
            elems.add(e);
        }
        
        gen.setElementsByIndex(elems);
        gen.setEvenSpacing(false);
        gen.setOrder(3, 3);
        gen.setSize(3);
        
        CompositionDataset toMeasure = new CompositionDataset();
        gen.addEntriesToDataset(toMeasure);
        
        assertTrue(toMeasure.NEntries() > 2000);
        
        // Make the modifier
        CompositionSetDistanceModifier mdfr = new CompositionSetDistanceModifier();
        mdfr.addComposition(new CompositionEntry("NaCl"));
        
        // Run it in parallel
        Magpie.NThreads = 2;
        
        // Test adding property
        mdfr.transform(toMeasure); 
        
        //   Check number of properties
        assertTrue(toMeasure.hasProperty("compdistance"));
        assertEquals(1, toMeasure.NProperties());
        for (BaseEntry e : toMeasure.getEntries()) {
            assertEquals(1, ((CompositionEntry) e).NProperties());
            assertTrue(((CompositionEntry) e).hasMeasuredProperty(0));
            assertTrue(((CompositionEntry) e).hasPredictedProperty(0));
        }
        
        // Test updating known property
        mdfr.transform(toMeasure);
        
        assertTrue(toMeasure.hasProperty("compdistance"));
        assertEquals(1, toMeasure.NProperties());
        for (BaseEntry e : toMeasure.getEntries()) {
            assertEquals(1, ((CompositionEntry) e).NProperties());
            assertTrue(((CompositionEntry) e).hasMeasuredProperty(0));
            assertTrue(((CompositionEntry) e).hasPredictedProperty(0));
        }
        
        // Make sure we get the same result with a serial calculation
        double[] parallelResult = toMeasure.getPredictedPropertyArray(0);
        
        Magpie.NThreads = 1;
        
        mdfr.transform(toMeasure);
        
        double[] serialResult = toMeasure.getMeasuredPropertyArray(0);
        
        assertArrayEquals(serialResult, parallelResult, 1e-5);
    }
}
