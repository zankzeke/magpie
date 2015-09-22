package magpie.attributes.generators.composition;

import java.util.*;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class APEAttributeGeneratorTest {

    @Test
    public void testAPECalculator() {
        // Test an ideal icosahedron
        assertEquals(1.0, 
                APEAttributeGenerator.computeAPE(12, 0.902113, 1.0),
                1e-6);
        assertEquals(1.0, 
                APEAttributeGenerator.computeAPE(new double[]{0.902113,1.0},
                        0, new int[]{0,12}),
                1e-6);
        
        // Make sure overpacked is less than 1 
        //  (using the conventional from 10.1038/ncomms9123
        assertTrue(APEAttributeGenerator.computeAPE(new double[]{0.902113,1.0},
                        0, new int[]{1,11}) < 1.0);        
    }
    
    @Test
    public void testClusterFinder() {
        // Test unary system
        List<List<int[]>> clusters = APEAttributeGenerator.
                findEfficientlyPackedClusters(new double[]{1.0}, 0.05);
        assertEquals(1, clusters.size());
        assertEquals(2, clusters.get(0).size());
        assertEquals(13, clusters.get(0).get(0)[0]);
        assertEquals(14, clusters.get(0).get(1)[0]);
        
        // Test binary system
        double[] radii = new double[]{1.0, 0.902113};
        clusters = APEAttributeGenerator.
                findEfficientlyPackedClusters(radii, 0.01);
        assertEquals(2, clusters.size());
        
        //   Make sure all clusters actually have an |APE-1| below 0.05
        for (int ct = 0; ct < radii.length; ct++) {
            for (int[] shell : clusters.get(ct)) {
                double ape = APEAttributeGenerator.computeAPE(radii, ct, shell);
                assertTrue(Math.abs(ape - 1) < 0.01);
            }
        }
        
        // Test quinary system
        radii = new double[]{1.0, 0.902113, 1.1, 1.2, 0.7};
        clusters = APEAttributeGenerator.
                findEfficientlyPackedClusters(radii, 0.01);
        assertEquals(5, clusters.size());
        
        //   Make sure all clusters actually have an |APE-1| below 0.05
        for (int ct = 0; ct < radii.length; ct++) {
            for (int[] shell : clusters.get(ct)) {
                double ape = APEAttributeGenerator.computeAPE(radii, ct, shell);
                assertTrue(Math.abs(ape - 1) < 0.01);
            }
        }
    }
    
    @Test 
    public void testComposition() throws Exception {
        // Make a fake cluster output
        List<List<int[]>> clusters = new ArrayList<>(2);
        int[] elements = new int[]{0,1};
        
        //   Central type = 0
        List<int[]> temp = new ArrayList<>();
        temp.add(new int[]{12,0});
        temp.add(new int[]{5,5});
        clusters.add(temp);
        
        //   Central type = 1
        temp = new ArrayList<>();
        temp.add(new int[]{2,5});
        clusters.add(temp);
        
        // Run the coversion
        List<CompositionEntry> comps = APEAttributeGenerator.computeClusterCompositions(elements, clusters);
        
        // Check output
        assertEquals(3, comps.size());
        assertTrue(comps.contains(new CompositionEntry("H")));
        assertTrue(comps.contains(new CompositionEntry("H6He5")));
        assertTrue(comps.contains(new CompositionEntry("H2He6")));
    }
    
    @Test
    public void testOptimalAPESolver() throws Exception {
        // Get radii lookup table
        CompositionDataset data = new CompositionDataset();
        double[] radii = data.getPropertyLookupTable("MiracleRadius");
        
        // Find the best Cu cluster
        CompositionEntry entry = new CompositionEntry("Cu");
        double ape = APEAttributeGenerator.determineOptimalAPE(28, entry, radii);
        assertEquals(0.976006 / 1.0, ape, 1e-6);
        
        // Find the best Cu-centered Cu64.3Zr35.7
        entry = new CompositionEntry("Cu64.3Zr35.7");
        ape = APEAttributeGenerator.determineOptimalAPE(28, entry, radii);
        assertEquals(0.902113 / 0.916870416, ape, 1e-6);
    }
    
    @Test
    public void testAttributeGeneration() throws Exception {
        // Make a fake dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Cu64.3Zr35.7");
        
        // Make generator
        APEAttributeGenerator gen = new APEAttributeGenerator();
        gen.setNNearestToEval(Arrays.asList(new Integer[]{1,3}));
        gen.setPackingThreshold(0.01);
        gen.setRadiusProperty("MiracleRadius");
        
        // Compute attributes
        gen.addAttributes(data);
        
        // Test results
        assertEquals(4, data.NAttributes());
        assertEquals(data.NAttributes(), data.getEntry(0).NAttributes());
        assertTrue(data.getEntry(0).getAttribute(0) < 
                data.getEntry(0).getAttribute(1));
        assertEquals(0.979277669, data.getEntry(0).getAttribute(2), 1e-6);
        assertEquals(0.020722331, data.getEntry(0).getAttribute(3), 1e-6);
        
        // Print description
        System.out.println(gen.printDescription(false));
    }
}
