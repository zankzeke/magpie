package magpie.data.materials;

import magpie.utility.DistinctPermutationGenerator;
import magpie.utility.MathUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionEntryTest {

    @Test
    public void testParsing() throws Exception {
        CompositionEntry entry;
        
        entry = new CompositionEntry("Fe");
        assertEquals(1, entry.Element.length);
        assertEquals(1.0, entry.getElementFraction("Fe"), 1e-6);
        
        entry = new CompositionEntry("FeO0");
        assertEquals(1, entry.Element.length);
        assertEquals(1.0, entry.getElementFraction("Fe"), 1e-6);
        
        entry = new CompositionEntry("FeCl3");
        assertEquals(2, entry.Element.length);
        assertEquals(0.25, entry.getElementFraction("Fe"), 1e-6);
        assertEquals(0.75, entry.getElementFraction("Cl"), 1e-6);
        
        entry = new CompositionEntry("Fe1Cl_3");
        assertEquals(2, entry.Element.length);
        assertEquals(0.25, entry.getElementFraction("Fe"), 1e-6);
        assertEquals(0.75, entry.getElementFraction("Cl"), 1e-6);
        
        entry = new CompositionEntry("(FeCl_3)");
        assertEquals(2, entry.Element.length);
        assertEquals(0.25, entry.getElementFraction("Fe"), 1e-6);
        assertEquals(0.75, entry.getElementFraction("Cl"), 1e-6);
        
        entry = new CompositionEntry("FeClCl2");
        assertEquals(2, entry.Element.length);
        assertEquals(0.25, entry.getElementFraction("Fe"), 1e-6);
        assertEquals(0.75, entry.getElementFraction("Cl"), 1e-6);
        
        entry = new CompositionEntry("Ca(NO3)2");
        assertEquals(3, entry.Element.length);
        assertEquals(1.0 / 9, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(2.0 / 9, entry.getElementFraction("N"), 1e-6);
        assertEquals(6.0 / 9, entry.getElementFraction("O"), 1e-6);
        
        entry = new CompositionEntry("Ca(N[O]3)2");
        assertEquals(3, entry.Element.length);
        assertEquals(1.0 / 9, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(2.0 / 9, entry.getElementFraction("N"), 1e-6);
        assertEquals(6.0 / 9, entry.getElementFraction("O"), 1e-6);
        
        entry = new CompositionEntry("Ca(N(O1.5)2)2");
        assertEquals(3, entry.Element.length);
        assertEquals(1.0 / 9, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(2.0 / 9, entry.getElementFraction("N"), 1e-6);
        assertEquals(6.0 / 9, entry.getElementFraction("O"), 1e-6);
        
        entry = new CompositionEntry("Ca{N{O1.5}2}2");
        assertEquals(3, entry.Element.length);
        assertEquals(1.0 / 9, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(2.0 / 9, entry.getElementFraction("N"), 1e-6);
        assertEquals(6.0 / 9, entry.getElementFraction("O"), 1e-6);
        
        entry = new CompositionEntry("CaO-0.01Ni");
        assertEquals(3, entry.Element.length);
        assertEquals(1.0 / 2.01, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(0.01 / 2.01, entry.getElementFraction("Ni"), 1e-6);
        assertEquals(1.0 / 2.01, entry.getElementFraction("O"), 1e-6);
        
        entry = new CompositionEntry("CaO" + Character.toString((char) 183) + "0.01Ni");
        assertEquals(3, entry.Element.length);
        assertEquals(1.0 / 2.01, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(0.01 / 2.01, entry.getElementFraction("Ni"), 1e-6);
        assertEquals(1.0 / 2.01, entry.getElementFraction("O"), 1e-6);
        
        entry = new CompositionEntry("Ca[N[O1.5]2]2");
        assertEquals(3, entry.Element.length);
        assertEquals(1.0 / 9, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(2.0 / 9, entry.getElementFraction("N"), 1e-6);
        assertEquals(6.0 / 9, entry.getElementFraction("O"), 1e-6);
        
        entry = new CompositionEntry("Ca(N(O1.5)2)2-2H2O");
        assertEquals(4, entry.Element.length);
        assertEquals(1.0 / 15, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(2.0 / 15, entry.getElementFraction("N"), 1e-6);
        assertEquals(8.0 / 15, entry.getElementFraction("O"), 1e-6);
        assertEquals(4.0 / 15, entry.getElementFraction("H"), 1e-6);

        entry = new CompositionEntry("Ca(N(O1.5)2)2-2.1(H)2O");
        assertEquals(4, entry.Element.length);
        assertEquals(1.0 / 15.3, entry.getElementFraction("Ca"), 1e-6);
        assertEquals(2.0 / 15.3, entry.getElementFraction("N"), 1e-6);
        assertEquals(8.1 / 15.3, entry.getElementFraction("O"), 1e-6);
        assertEquals(4.2 / 15.3, entry.getElementFraction("H"), 1e-6);
        
        entry = new CompositionEntry("{[(Fe0.6Co0.4)0.75B0.2Si0.05]0.96Nb0.04}96Cr4");
        assertEquals(6, entry.Element.length);
        assertEquals(0.41472, entry.getElementFraction("Fe"), 1e-6);
        assertEquals(0.27648, entry.getElementFraction("Co"), 1e-6);
        assertEquals(0.18432, entry.getElementFraction("B"), 1e-6);
        assertEquals(0.04608, entry.getElementFraction("Si"), 1e-6);
        assertEquals(0.0384, entry.getElementFraction("Nb"), 1e-6);
        assertEquals(0.04, entry.getElementFraction("Cr"), 1e-6);

    }
    
    @Test 
    public void testSetComposition() throws Exception {
        // One element
        int[] elem = new int[]{0};
        double[] frac = new double[]{1};
        CompositionEntry entry = new CompositionEntry(elem, frac);
        assertEquals(1, entry.Element.length);
        assertEquals(1, entry.getElementFraction("H"), 1e-6);
        
        // One element, with duplicates
        elem = new int[]{0,0};
        frac = new double[]{0.5,0.5};
        entry = new CompositionEntry(elem, frac);
        assertEquals(1, entry.Element.length);
        assertEquals(1, entry.getElementFraction("H"), 1e-6);
        
        // One element, with zero
        elem = new int[]{0,1};
        frac = new double[]{1,0};
        entry = new CompositionEntry(elem, frac);
        assertEquals(1, entry.Element.length);
        assertEquals(1, entry.getElementFraction("H"), 1e-6);
        
        // Two elements
        elem = new int[]{16,10};
        frac = new double[]{1,1};
        entry = new CompositionEntry(elem, frac);
        assertEquals(2, entry.Element.length);
        assertEquals(0.5, entry.getElementFraction("Na"), 1e-6);
        assertEquals(0.5, entry.getElementFraction("Cl"), 1e-6);
        assertArrayEquals(new int[]{10,16}, entry.Element);
        assertArrayEquals(new double[]{0.5,0.5}, entry.Fraction, 1e-6);
        assertEquals(2, entry.NumberInCell, 1e-6);
        
        // Two elements, with duplicates
        elem = new int[]{11,16,16};
        frac = new double[]{1,1,1};
        entry = new CompositionEntry(elem, frac);
        assertEquals(2, entry.Element.length);
        assertEquals(1f/3, entry.getElementFraction("Mg"), 1e-6);
        assertEquals(2f/3, entry.getElementFraction("Cl"), 1e-6);
        assertArrayEquals(new int[]{11,16}, entry.Element);
        assertArrayEquals(new double[]{1f/3,2f/3}, entry.Fraction, 1e-6);
        assertEquals(3, entry.NumberInCell, 1e-6);
        
        // Two elements, with zero
        elem = new int[]{11,16,16};
        frac = new double[]{1,2,0};
        entry = new CompositionEntry(elem, frac);
        assertEquals(2, entry.Element.length);
        assertEquals(1f/3, entry.getElementFraction("Mg"), 1e-6);
        assertEquals(2f/3, entry.getElementFraction("Cl"), 1e-6);
        assertEquals(0, entry.getElementFraction("Na"), 1e-6);
        assertArrayEquals(new int[]{11,16}, entry.Element);
        assertArrayEquals(new double[]{1f/3,2f/3}, entry.Fraction, 1e-6);
        assertEquals(3, entry.NumberInCell, 1e-6);
        assertEquals("MgCl<sub>2</sub>", entry.toHTMLString());
        
    }

    @Test
    public void testRectify() throws Exception {
        // Make an example composition
        int[] elem = new int[]{1,2,3,4,5};
        double[] frac = new double[]{1.0,2.0,3.0,4.0,5.0};
        
        // Make first composition
        CompositionEntry goldEntry = new CompositionEntry(elem, frac);
        int[] goldElems = goldEntry.getElements();
        double[] goldFracs = goldEntry.getFractions();
        for (int i=0; i<5; i++) {
            assertEquals(goldFracs[i], (double) goldElems[i] / 15.0, 1e-6);
        }
        
        // Iterate through all permutations
        for (int[] perm : 
                DistinctPermutationGenerator.generatePermutations(new int[]{0,1,2,3,4})) {
            // Make a new version of elem and frac
            int[] newElem = elem.clone();
            double[] newFrac = frac.clone();
            for (int i=0; i<newElem.length; i++) {
                newElem[i] = elem[perm[i]];
                newFrac[i] = frac[perm[i]];
            }
            
            // Make sure it parses the same
            CompositionEntry newEntry = new CompositionEntry(newElem, newFrac);
            assertEquals(newEntry, goldEntry);
            assertEquals(0, newEntry.compareTo(goldEntry));
            assertArrayEquals(goldElems, newEntry.getElements());
            assertArrayEquals(goldFracs, newEntry.getFractions(), 1e-6);
        }
    }
}
