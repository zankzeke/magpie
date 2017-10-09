package magpie.csp.diagramdata;

import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 *
 */
public class OnTheFlyPhaseDiagramStatisticsTest {

    @Test
    public void testGenerateStatistics() throws Exception {
        // Make a fake phase diagram
        Map<CompositionEntry, String> phaseDiagrams = new TreeMap<>();
        phaseDiagrams.put(new CompositionEntry("Al"), "fcc");
        phaseDiagrams.put(new CompositionEntry("Ni"), "fcc");
        phaseDiagrams.put(new CompositionEntry("Zr"), "hcp");
        phaseDiagrams.put(new CompositionEntry("AlNi"), "B2");
        phaseDiagrams.put(new CompositionEntry("AlZr"), "B1");
        phaseDiagrams.put(new CompositionEntry("Al2Zr3"), "C1");
        phaseDiagrams.put(new CompositionEntry("Al3Zr2"), "C2");
        phaseDiagrams.put(new CompositionEntry("Al1Ni9"), "D1");
        phaseDiagrams.put(new CompositionEntry("AlNiZr"), "E1");


        OnTheFlyPhaseDiagramStatistics stats = new OnTheFlyPhaseDiagramStatistics();
        // Test a binary system where we store all stoichiometries
        stats.importKnownCompounds(phaseDiagrams, 2, new int[]{20});
        assertEquals(stats.CommonCompositions.size(), 7); // B, A, AB9, A2B3, AB, A3B2, A9B
        assertArrayEquals(stats.CommonCompositions.get(0).getKey(), new double[]{1,0,1}, 1e-6);
        assertArrayEquals(stats.getCompoundVector(new int[]{0,1}), new int[7]);
        // Index of FCC
        int fcc = stats.CommonCompositions.get(0).getRight().indexOf("fcc") + 1; // "0" is "no compound"
        int hcp = stats.CommonCompositions.get(0).getRight().indexOf("hcp") + 1;
        int b2 = stats.CommonCompositions.get(4).getRight().indexOf("B2") + 1;
        int d1 = stats.CommonCompositions.get(2).getRight().indexOf("D1") + 1;
        assertArrayEquals(new int[]{fcc, fcc, d1, 0, b2, 0, 0}, stats.getCompoundVector(new int[]{12, 27}));
        assertArrayEquals(new int[]{fcc, fcc, 0, 0, b2, 0, d1}, stats.getCompoundVector(new int[]{27, 12}));

        // Test a binary system where we do not store all stoichiometries
        stats.importKnownCompounds(phaseDiagrams, 2, new int[]{2});
        assertEquals(stats.CommonCompositions.size(), 5); // B, A, A2B3, AB, A3B2
        assertArrayEquals(stats.CommonCompositions.get(0).getKey(), new double[]{1,0,1}, 1e-6);
    }
}