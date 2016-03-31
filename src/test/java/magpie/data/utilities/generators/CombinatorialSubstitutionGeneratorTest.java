package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.Atom;
import vassal.data.Cell;

/**
 *
 * @author Logan Ward
 */
public class CombinatorialSubstitutionGeneratorTest {
    
    @Test
    public void test() throws Exception {
        // Create an example dataset with B2-CuZr
        Cell strc = new Cell();
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Cu");
        strc.setTypeName(1, "Zr");
        AtomicStructureEntry entry = new AtomicStructureEntry(strc, "B2-CuZr", null);
        CrystalStructureDataset data = new CrystalStructureDataset();
        data.addEntry(entry);
        
        // Prepare generator
        CombinatorialSubstitutionGenerator gen;
        gen = new CombinatorialSubstitutionGenerator();
        gen.setComputeVoronoi(true);
        gen.setPrototypes(data);
        List<String> elems = new ArrayList();
        elems.add("Ni");
        elems.add("Ti");
        gen.setElementsByAbbreviation(elems);
        
        // Create entries
        List<BaseEntry> newEntries = gen.generateEntries();
        assertEquals(4, newEntries.size());
        assertNotEquals(newEntries.get(0), newEntries.get(1));
    }
    
}
