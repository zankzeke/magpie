package magpie.data.materials;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.Atom;
import vassal.data.Cell;

/**
 * 
 * @author Logan Ward
 */
public class CrystalStructureEntryTest {
    
    @Test
    public void testReplacement() throws Exception {
        // Make B2-CuZr
        Cell cell = new Cell();
        cell.addAtom(new Atom(new double[]{0,0,0}, 0));
        cell.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        cell.setTypeName(0, "Cu");
        cell.setTypeName(1, "Zr");
        CrystalStructureEntry CuZr = new CrystalStructureEntry(cell, "CuZr", null);
        
        // Run Voronoi tessellation
        CuZr.computeVoronoiTessellation();
        
        // Make B2-NiZr
        Map<String,String> changes = new TreeMap<>();
        changes.put("Cu", "Ni");
        CrystalStructureEntry NiZr = CuZr.replaceElements(changes);
        
        // Make sure the tessellation object did not change
        assertSame(CuZr.computeVoronoiTessellation(), NiZr.computeVoronoiTessellation());
        
        // Make sure the two are still unchaged
        assertEquals(0.5, CuZr.getElementFraction("Cu"), 1e-6);
        assertEquals(0.0, CuZr.getElementFraction("Ni"), 1e-6);
        assertEquals(0.0, NiZr.getElementFraction("Cu"), 1e-6);
        assertEquals(0.5, NiZr.getElementFraction("Ni"), 1e-6);
        
        // Now, change the structure such that it has fewer types
        changes.put("Ni", "Zr");
        
        CrystalStructureEntry bccZr = NiZr.replaceElements(changes);
        
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
        CrystalStructureEntry CuZr = new CrystalStructureEntry(cell, "B2", null);
        
        // Get name
        String name = CuZr.toString();
        System.out.println(name);
        assertTrue(name.contains("CuZr"));
        assertTrue(name.contains("B2"));
    }
    
        
    @Test
    public void testToJSON() throws Exception {
        // Make B2-CuZr
        Cell cell = new Cell();
        cell.addAtom(new Atom(new double[]{0,0,0}, 0));
        cell.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        cell.setTypeName(0, "Cu");
        cell.setTypeName(1, "Zr");
        CrystalStructureEntry CuZr = new CrystalStructureEntry(cell, "B2", null);
        
        // Get name
        JSONObject json = CuZr.toJSON();
        System.out.println(json.toString(2));
        assertTrue(json.getString("name").contains("B2"));
        assertTrue(json.has("poscar"));
    }

    @Test
    public void testRedectorate() throws Exception {
        // Create a B2 structure
        Cell cell = new Cell();
        cell.addAtom(new Atom(new double[]{0,0,0}, 0));
        cell.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        cell.setTypeName(0, "Cu");
        cell.setTypeName(1, "Zr");
        CrystalStructureEntry CuZr = new CrystalStructureEntry(cell, "B2", null);

        // Create all versions with CsCl composition
        List<CrystalStructureEntry> newStrcs = CuZr.changeComposition(new CompositionEntry("CsCl"));
        assertEquals(2, newStrcs.size());
        assertFalse(newStrcs.get(0).equals(newStrcs.get(1)));
        assertEquals(newStrcs.get(1).getComposition(), new CompositionEntry("CsCl"));

        // Test with a harder structure
        cell = new Cell();
        cell.addAtom(new Atom(new double[]{0,0,0}, 0));
        cell.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        cell.addAtom(new Atom(new double[]{0.5,0.5,0.0}, 2));
        cell.addAtom(new Atom(new double[]{0.5,0.0,0.5}, 2));
        cell.addAtom(new Atom(new double[]{0.0,0.5,0.5}, 3));
        cell.addAtom(new Atom(new double[]{0.25,0.25,0.25}, 3));
        cell.setTypeName(0, "Cu");
        cell.setTypeName(1, "Zr");
        cell.setTypeName(2, "Fe");
        cell.setTypeName(3, "Ti");
        CrystalStructureEntry strc2 = new CrystalStructureEntry(cell, "Test", null);

        newStrcs = strc2.changeComposition(new CompositionEntry("HHeLi2Be2"));
        assertEquals(4, newStrcs.size());
        assertFalse(newStrcs.get(0).equals(newStrcs.get(1)));
        assertEquals(newStrcs.get(1).getComposition(), new CompositionEntry("HHeLi2Be2"));

        // Test a failure case
        assertEquals(0, CuZr.changeComposition(new CompositionEntry("HHeLi2Be2")).size());
    }
}
