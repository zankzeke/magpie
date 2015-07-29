package magpie.data.materials;

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

    }
    
}
