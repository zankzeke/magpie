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
        
        // Run Voronoi tessellation
        CuZr.computeVoronoiTessellation();
        
        // Make B2-NiZr
        Map<String,String> changes = new TreeMap<>();
        changes.put("Cu", "Ni");
        AtomicStructureEntry NiZr = CuZr.replaceElements(changes);
        
        // Make sure the tessellation object did not change
        assertSame(CuZr.computeVoronoiTessellation(), NiZr.computeVoronoiTessellation());
        
        // Make sure the two are still unchaged
        assertEquals(0.5, CuZr.getElementFraction("Cu"), 1e-6);
        assertEquals(0.0, CuZr.getElementFraction("Ni"), 1e-6);
        assertEquals(0.0, NiZr.getElementFraction("Cu"), 1e-6);
        assertEquals(0.5, NiZr.getElementFraction("Ni"), 1e-6);
        
        // Now, change the structure such that it has fewer types
        changes.put("Ni", "Zr");
        
        AtomicStructureEntry bccZr = NiZr.replaceElements(changes);
        
        // Make sure the structure only has one type
        assertEquals(1.0, bccZr.getElementFraction("Zr"), 1e-6);
        assertEquals(1, bccZr.getStructure().nTypes());
        
        assertNotSame(NiZr.computeVoronoiTessellation(), bccZr.computeVoronoiTessellation());
    }
    
    @Test
    public void testToString() throws Exception {
        // Make B2-CuZr
        Cell cell = new Cell();
        cell.addAtom(new Atom(new double[]{0,0,0}, 0));
        cell.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        cell.setTypeName(0, "Cu");
        cell.setTypeName(1, "Zr");
        AtomicStructureEntry CuZr = new AtomicStructureEntry(cell, "B2", null);
        
        // Get name
        String name = CuZr.toString();
        System.out.println(name);
        assertTrue(name.contains("CuZr"));
        assertTrue(name.contains("B2"));
    }
}
