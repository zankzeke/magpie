package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.Atom;
import vassal.data.Cell;

/**
 *
 * @author Logan Ward
 */
public class CoulombMatrixAttributeGeneratorTest {
    
    @Test
    public void test() throws Exception {
        // Make a test dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        //   Make two simple structures
        Cell strc1 = new Cell();
        strc1.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc1.setTypeName(0, "Al");
        CrystalStructureEntry entry1 = new CrystalStructureEntry(strc1, "Al", null);
        data.addEntry(entry1);
        
        Cell strc2 = new Cell();
        strc2.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc2.setTypeName(0, "Ni");
        strc2.addAtom(new Atom(new double[]{0,0.5,0}, 1));
        strc2.setTypeName(1, "Al");
        strc2.addAtom(new Atom(new double[]{0,0,0.5}, 1));
        CrystalStructureEntry entry2 = new CrystalStructureEntry(strc2, "NiAl2", null);
        data.addEntry(entry2);
        
        // Create attribute generator
        CoulombMatrixAttributeGenerator gen = new CoulombMatrixAttributeGenerator();
        
        List<Object> options = new ArrayList<>();
        options.add(10);
        gen.setOptions(options);
        
        System.out.println(gen.printUsage());
        
        // Generate attributes
        gen.addAttributes(data);
        
        // Test results
        assertEquals(10, data.NAttributes());
        assertEquals(10, entry1.NAttributes());
        assertEquals(10, entry2.NAttributes());
        
        assertNotEquals(0, entry1.getAttribute(0), 1e-6);
        for (int i=1; i<10; i++) {
            assertEquals(0, entry1.getAttribute(i), 1e-6);
        }
        
        assertNotEquals(0, entry2.getAttribute(0), 1e-6);
        assertNotEquals(0, entry2.getAttribute(1), 1e-6);
        assertNotEquals(0, entry2.getAttribute(2), 1e-6);
        for (int i=3; i<10; i++) {
            assertEquals(0, entry2.getAttribute(i), 1e-6);
        }
        
        // Test description
        System.out.println(gen.printDescription(true));
        System.out.println(gen.printDescription(false));
        
        // Test citation information
        List<Pair<String, Citation>> citations = gen.getCitations();
        for (Pair<String, Citation> citation : citations) {
            System.out.println("Reason: " + citation.getKey());
            System.out.println("Citation: " + citation.getRight().toString());
        }
    }
    
    
}
