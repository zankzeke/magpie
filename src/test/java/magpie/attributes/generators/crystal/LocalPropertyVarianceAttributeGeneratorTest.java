package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
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
public class LocalPropertyVarianceAttributeGeneratorTest extends CoordinationNumberAttributeGeneratorTest {

    @Override
    public BaseAttributeGenerator getGenerator() throws Exception {
        LocalPropertyVarianceAttributeGenerator gen = new LocalPropertyVarianceAttributeGenerator();
        
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
        
        // Create a L12-H3He structure
        // Structure of  L12
        Cell strc = new Cell();
		Atom atom = new Atom(new double[]{0,0,0}, 1);
        strc.addAtom(atom);
		atom = new Atom(new double[]{0.5,0.5,0.}, 0);
		strc.addAtom(atom);
        atom = new Atom(new double[]{0.5,0.,0.5}, 0);
		strc.addAtom(atom);
        atom = new Atom(new double[]{0.,0.5,0.5}, 0);
		strc.addAtom(atom);
        strc.setTypeName(0, "H");
        strc.setTypeName(1, "He");
        
        // Add it to dataset
        data.addEntry(new AtomicStructureEntry(strc, "L12-HHe", null));
        
        // Run the attribute generator
        LocalPropertyVarianceAttributeGenerator gen = new LocalPropertyVarianceAttributeGenerator();
        gen.clearShells();
        gen.addShell(1);
        gen.addAttributes(data);
        
        // Test out the results
        assertEquals(5, data.NAttributes());
        BaseEntry entry = data.getEntry(0);
        assertEquals(data.getAttributeName(0), 0.166666667, entry.getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 0.083333333, entry.getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 0, entry.getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 2f/9, entry.getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 2f/9, entry.getAttribute(4), 1e-6);
    }
}
