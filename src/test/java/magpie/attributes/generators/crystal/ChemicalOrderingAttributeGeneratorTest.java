package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
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
public class ChemicalOrderingAttributeGeneratorTest 
        extends CoordinationNumberAttributeGeneratorTest {

    @Override
    public BaseAttributeGenerator getGenerator() {
        return new ChemicalOrderingAttributeGenerator();
    }

    @Override
    public int expectedCount() {
        return 3;
    }
    
    @Test
    public void testResults() throws Exception {
        // Structure of B2 crystal
        Cell strc = new Cell();
		Atom atom = new Atom(new double[]{0,0,0}, 0);
        strc.addAtom(atom);
		atom = new Atom(new double[]{0.5,0.5,0.5}, 1);
		strc.addAtom(atom);
        strc.setTypeName(0, "Al");
        strc.setTypeName(1, "Ni");
        
        // Add it to a dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        data.addEntry(new CrystalStructureEntry(strc, "B2", null));
        
        // Make attribute generator
        ChemicalOrderingAttributeGenerator gen = new ChemicalOrderingAttributeGenerator();
        
        // Set options as unweighted
        List<Object> options = new ArrayList<>();
        options.add("-unWeighted");
        options.add(1);
        options.add(2);
        
        gen.setOptions(options);
        System.out.println(gen.printUsage());
        
        // Compute attributes
        gen.addAttributes(data);
        
        // Test results
        assertEquals(0.142857, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(0.04, data.getEntry(0).getAttribute(1), 1e-6);
        
        // Now, with weights
        options.remove(0);
        gen.setOptions(options);
        
        // Compute attributes
        data.clearAttributes();
        gen.addAttributes(data);
        
        // Test results
        assertEquals(0.551982, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(0.253856, data.getEntry(0).getAttribute(1), 1e-6);
    }
    
}
