package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.LinkedList;
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
        // Create an example dataset with B2-CuZr and SC-Cu
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        Cell strc = new Cell();
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.setTypeName(0, "Cu");
        AtomicStructureEntry entry = new AtomicStructureEntry(strc.clone(), "SC-Cu", null);
        data.addEntry(entry);
        
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Cu");
        strc.setTypeName(1, "Zr");
        entry = new AtomicStructureEntry(strc, "B2-CuZr", null);
        data.addEntry(entry);
        
        // Prepare generator
        CombinatorialSubstitutionGenerator gen;
        gen = new CombinatorialSubstitutionGenerator();
        
        List<Object> options = new LinkedList<>();
        options.add("-voro");
        options.add(data);
        options.add("Ni");
        options.add("Ti");
        
        gen.setOptions(options);
        
        // Create entries
        List<BaseEntry> newEntries = gen.generateEntries();
        assertEquals(6, newEntries.size());
        assertNotEquals(newEntries.get(0), newEntries.get(1));
    }
    
    @Test
    public void testIgnore() throws Exception {
        // Create a perovskite
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        Cell strc = new Cell();
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.0}, 2));
        strc.addAtom(new Atom(new double[]{0.5,0.0,0.5}, 2));
        strc.addAtom(new Atom(new double[]{0.0,0.5,0.5}, 2));
        strc.setTypeName(0, "Sr");
        strc.setTypeName(1, "Ti");
        strc.setTypeName(2, "O");
        
        AtomicStructureEntry entry = new AtomicStructureEntry(strc, "SrTiO3-perov", null);
        data.addEntry(entry);
        
        // Prepare generator
        CombinatorialSubstitutionGenerator gen;
        gen = new CombinatorialSubstitutionGenerator();
        
        List<Object> options = new LinkedList<>();
        options.add("-ignore");
        options.add("O");
        options.add(data);
        options.add("Ni");
        options.add("Fe");
        options.add("Cu");
        
        gen.setOptions(options);
        
        // Create entries
        List<BaseEntry> newEntries = gen.generateEntries();
        assertEquals(9, newEntries.size());
        assertNotEquals(newEntries.get(0), newEntries.get(1));
        
        // Also ignore Sr        
        options.add(1, "Sr");
        
        gen.setOptions(options);
        
        newEntries = gen.generateEntries();
        assertEquals(3, newEntries.size());
        assertNotEquals(newEntries.get(0), newEntries.get(1));
    }    
}
