package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.Atom;
import vassal.data.Cell;

/**
 *
 * @author Logan Ward
 */
public class LocalPropertyDifferenceAttributeGeneratorTest 
        extends CoordinationNumberAttributeGeneratorTest {

    @Override
    public BaseAttributeGenerator getGenerator() throws Exception {
        LocalPropertyDifferenceAttributeGenerator gen = new LocalPropertyDifferenceAttributeGenerator();
        
        // Set options
        List options = new ArrayList();
        options.add(1);
        options.add(2);
        gen.setOptions(options);
        
        return gen;
    }

    @Override
    public int expectedCount() {
        return 10;
    }
    
    @Test
    public void testResults() throws Exception {
        // Create a blank dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        data.addElementalProperty("Number");
        
        // Create a B1-HHe structure
        Cell strc = new Cell();
        double[][] basis = new double[3][];
        basis[0] = new double[]{0.0,0.5,0.5};
        basis[1] = new double[]{0.5,0.0,0.5};
        basis[2] = new double[]{0.5,0.5,0.0};
        strc.setBasis(basis);
		Atom atom = new Atom(new double[]{0,0,0}, 0);
        strc.addAtom(atom);
		atom = new Atom(new double[]{0.5,0.5,0.5}, 1);
        strc.addAtom(atom);
        strc.setTypeName(0, "H");
        strc.setTypeName(1, "He");
        
        // Add it to dataset
        data.addEntry(new CrystalStructureEntry(strc, "B1-HHe", null));
        
        // Run the attribute generator
        LocalPropertyDifferenceAttributeGenerator gen = new LocalPropertyDifferenceAttributeGenerator();
        gen.clearShells();
        gen.addShell(1);
        gen.addShell(2);
        gen.addAttributes(data);
        
        // Test out the results
        assertEquals(10, data.NAttributes());
        BaseEntry entry = data.getEntry(0);
        assertEquals(data.getAttributeName(0), 1, entry.getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 0, entry.getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 1, entry.getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 1, entry.getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 0, entry.getAttribute(4), 1e-6);
        assertEquals(data.getAttributeName(5), 0, entry.getAttribute(5), 1e-6);
        assertEquals(data.getAttributeName(6), 0, entry.getAttribute(6), 1e-6);
        assertEquals(data.getAttributeName(7), 0, entry.getAttribute(7), 1e-6);
        assertEquals(data.getAttributeName(8), 0, entry.getAttribute(8), 1e-6);
        assertEquals(data.getAttributeName(9), 0, entry.getAttribute(9), 1e-6);
    }
}
