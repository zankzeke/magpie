package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.Atom;
import vassal.data.Cell;

/**
 *
 * @author Logan Ward
 */
public class PRDFAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Make a test dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        //   Make two simple structures
        Cell strc1 = new Cell();
        strc1.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc1.setTypeName(0, "Al");
        AtomicStructureEntry entry1 = new AtomicStructureEntry(strc1, "Al", null);
        data.addEntry(entry1);
        
        Cell strc2 = new Cell();
        strc2.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc2.setTypeName(0, "Ni");
        strc2.addAtom(new Atom(new double[]{0,0.5,0}, 1));
        strc2.setTypeName(1, "Al");
        strc2.addAtom(new Atom(new double[]{0,0,0.5}, 1));
        AtomicStructureEntry entry2 = new AtomicStructureEntry(strc2, "NiAl2", null);
        data.addEntry(entry2);
        
        // Create attribute generator
        PRDFAttributeGenerator gen = new PRDFAttributeGenerator();
        
        List<Object> options = new ArrayList<>();
        options.add(3);
        options.add(5);
        options.add(data);
        gen.setOptions(options);
        
        System.out.println(gen.printUsage());
        
        // Add an extra element
        gen.addElement("H");
        
        // Generate attributes
        gen.addAttributes(data);
        
        // Test results
        assertEquals(3*3*5, data.NAttributes());

        assertEquals(3*3*5, entry1.NAttributes());
        assertEquals(0, StatUtils.sum(Arrays.copyOfRange(entry1.getAttributes(), 0, 4*5)), 1e-6); // First 4 PRDFs are H-X
        assertTrue(StatUtils.max(Arrays.copyOfRange(entry1.getAttributes(), 4*5, 5*5)) > 0);
        assertEquals(0, StatUtils.sum(Arrays.copyOfRange(entry1.getAttributes(), 6*5, 9*5)), 1e-6); // Only Al instructure
        
        assertEquals(3*3*5, entry1.NAttributes());
        assertEquals(0, StatUtils.sum(Arrays.copyOfRange(entry2.getAttributes(), 0, 4*5)), 1e-6); // First 4 PRDFs are H-X
        assertTrue(StatUtils.max(Arrays.copyOfRange(entry2.getAttributes(), 4*5, 5*5)) > 0);
        assertTrue(StatUtils.max(Arrays.copyOfRange(entry2.getAttributes(), 5*5, 6*5)) > 0);
        assertEquals(0, StatUtils.sum(Arrays.copyOfRange(entry2.getAttributes(), 6*5, 7*5)), 1e-6);
        assertTrue(StatUtils.max(Arrays.copyOfRange(entry2.getAttributes(), 7*5, 8*5)) > 0);
        assertTrue(StatUtils.max(Arrays.copyOfRange(entry2.getAttributes(), 8*5, 9*5)) > 0);
        
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
