package magpie.data.materials;

import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.Atom;
import vassal.data.Cell;

/**
 * 
 * @author Logan Ward
 */
public class AtomicStructureEntryTest {
    
    @Test
    public void testReplacement() throws Exception {
        // Make B2-CuZr
        Cell cell = new Cell();
        cell.addAtom(new Atom(new double[]{0,0,0}, 0));
        cell.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        cell.setTypeName(0, "Cu");
        cell.setTypeName(1, "Zr");
        AtomicStructureEntry CuZr = new AtomicStructureEntry(cell, "CuZr", null);
        
        // Make B2-NiZr
        Map<String,String> changes = new TreeMap<>();
        changes.put("Cu", "Ni");
        AtomicStructureEntry NiZr = CuZr.replaceElements(changes);
        
        // Make sure the two are still unchaged
        assertEquals(0.5, CuZr.getElementFraction("Cu"), 1e-6);
        assertEquals(0.0, CuZr.getElementFraction("Ni"), 1e-6);
        assertEquals(0.0, NiZr.getElementFraction("Cu"), 1e-6);
        assertEquals(0.5, NiZr.getElementFraction("Ni"), 1e-6);
    }
    
}
