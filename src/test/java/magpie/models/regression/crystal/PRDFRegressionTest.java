package magpie.models.regression.crystal;

import java.util.ArrayList;
import java.util.List;
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
    
    @Test
    public void testSimilarityMatcher() throws Exception {
        // Make a dataset with B2 AlNi, FeNi, and FeZr
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        Cell strc = new Cell();
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Al");
        strc.setTypeName(1, "Ni");
        AtomicStructureEntry entry = new AtomicStructureEntry(strc, "AlNi", null);
        data.addEntry(entry);
        
        strc = strc.clone();
        strc.setTypeName(0, "Fe");
        entry = new AtomicStructureEntry(strc, "FeNi", null);
        data.addEntry(entry);
        
        strc = strc.clone();
        strc.setTypeName(1, "Zr");
        entry = new AtomicStructureEntry(strc, "FeZr", null);
        data.addEntry(entry);
        
        // Assign fake class values
        data.setMeasuredClasses(new double[]{-1,1,2});
        
        // Train PRDF model
        PRDFRegression prdf = new PRDFRegression();
        prdf.setCutoff(2.0);
        prdf.train(data);
        
        // Find the closest entries to FeZr
        List<String> matches = prdf.findClosestEntries(entry, 2);
        
        // Test results
        assertEquals(2, matches.size());
        assertEquals("FeZr", matches.get(0));
        assertEquals("FeNi", matches.get(1));
        
        // Find the closest entries to AlNi
        System.out.println(data.getEntry(0).toString());
        matches = prdf.findClosestEntries(data.getEntry(0), 2);
        
        // Test results
        assertEquals(2, matches.size());
        assertEquals("AlNi", matches.get(0));
        assertEquals("FeNi", matches.get(1));
        
        // Run through the command line interface
        List<Object> command = new ArrayList<>();
        command.add("match");
        command.add(data);
        command.add(2);
        
        prdf.runCommand(command);
    }
    
}
