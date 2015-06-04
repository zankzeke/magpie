package magpie.models.regression.crystal;

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
public class PRDFRegressionTest {

    @Test
    public void testDistance() throws Exception {
        // Make two simple structures
        Cell strc1 = new Cell();
        strc1.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc1.setTypeName(0, "Al");
        Cell strc2 = new Cell();
        strc2.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc2.setTypeName(0, "Ni");
        
        PRDFRegression r = new PRDFRegression();
        // Compute representations of each structure
        Object rep1 = r.computeRepresentation(strc1);
        Object rep2 = r.computeRepresentation(strc2);
        
        // Check that similarity between identical structures is 1.0
        assertEquals(1.0, r.computeSimiliarity(rep1, rep1), 1e-6);
        assertEquals(1.0, r.computeSimiliarity(rep2, rep2), 1e-6);
        
        // Check symmetry
        assertEquals(r.computeSimiliarity(rep2, rep1),
                r.computeSimiliarity(rep1, rep2), 1e-6);
        
        // Check that similiary between these structures is less than 1.0
        assertTrue(r.computeSimiliarity(rep2, rep1) < 1.0);
        
        // Checkt that the similarity is, in fact, 0
        assertEquals(0, r.computeSimiliarity(rep2, rep1), 1e-6);
    }
    
    @Test
    public void testModel() throws Exception {
        // Make an example entries
        Cell strc1 = new Cell();
        strc1.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc1.setTypeName(0, "Al");
        AtomicStructureEntry entry1 = new AtomicStructureEntry(strc1, "test", null);
        entry1.setMeasuredClass(1.0);
        
        Cell strc2 = new Cell();
        strc2.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc2.setTypeName(0, "Ni");
        AtomicStructureEntry entry2 = new AtomicStructureEntry(strc2, "test", null);
        entry2.setMeasuredClass(0.5);
        
        
        // Make an example dataset
        CrystalStructureDataset dataset = new CrystalStructureDataset();
        dataset.addEntry(entry1);
        dataset.addEntry(entry2);
        
        // Train a model
        PRDFRegression r = new PRDFRegression();
        r.train(dataset);
    }
    
}
