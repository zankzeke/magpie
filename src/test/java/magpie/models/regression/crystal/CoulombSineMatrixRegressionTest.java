
package magpie.models.regression.crystal;

import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.StatUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import vassal.data.Atom;
import vassal.data.Cell;

/**
 *
 * @author Logan Ward
 */
public class CoulombSineMatrixRegressionTest {

    @Test
    public void testMatrix() throws Exception {
        // Make a simple structure
        Cell strc = new Cell();
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.setTypeName(0, "Al");
        
        // Compute the sine matrix
        CoulombSineMatrixRegression r = new CoulombSineMatrixRegression();
        RealMatrix mat = r.computeCoulombMatrix(strc);
        assertEquals(1, mat.getRowDimension());
        assertEquals(1, mat.getColumnDimension());
        assertEquals(0.5 * Math.pow(13, 2.4), mat.getEntry(0, 0), 1e-6);
        
        // Add another atom and retest
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 0));
        mat = r.computeCoulombMatrix(strc);
        assertEquals(2, mat.getRowDimension());
        assertEquals(2, mat.getColumnDimension());
        
        // Test: Is it insensitive to basis changes
        double[][] newBasis = strc.getBasis();
        newBasis[1][0] = 12;
        strc.setBasis(newBasis);
        assertEquals(1.0, strc.volume(), 1e-6);
        RealMatrix mat2 = r.computeCoulombMatrix(strc);
        if (mat.subtract(mat2).getFrobeniusNorm() > 1e-6) {
            System.err.println("WARNING: Not insensitive to basis change");
        }
    }
    
    @Test
    public void testRepresentation() throws Exception {
        // Make a simple structure
        Cell strc = new Cell();
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.setTypeName(0, "Al");
        
        // Compute the sine matrix
        CoulombSineMatrixRegression r = new CoulombSineMatrixRegression();
        double[] mat = (double[]) r.computeRepresentation(strc);
        assertEquals(1, mat.length);
        
        // Add another atom and retest
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 0));
        mat = (double[]) r.computeRepresentation(strc);
        assertEquals(2, mat.length);
        
        // Test: Is it insensitive to basis changes
        double[][] newBasis = strc.getBasis();
        newBasis[1][0] = 12;
        strc.setBasis(newBasis);
        assertEquals(1.0, strc.volume(), 1e-6);
        double[] mat2 = (double[]) r.computeRepresentation(strc);
        if (StatUtils.meanDifference(mat, mat2) > 1e-6) {
            System.err.format("Warning: Not insensistive to basis changes", mat, mat2, 1e-6);
        }
    }
    
    @Test
    public void testDistance() throws Exception {
        // Make two simple structures
        Cell strc1 = new Cell();
        strc1.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc1.setTypeName(0, "Al");
        Cell strc2 = new Cell();
        strc2.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc2.setTypeName(0, "Ni");
        
        CoulombSineMatrixRegression r = new CoulombSineMatrixRegression();
        // Compute representations of each structure
        double[] rep1 = (double[]) r.computeRepresentation(strc1);
        double[] rep2 = (double[]) r.computeRepresentation(strc2);
        
        // Check that similarity between identical structures is 1.0
        assertEquals(1.0, r.computeSimiliarity(rep1, rep1), 1e-6);
        assertEquals(1.0, r.computeSimiliarity(rep2, rep2), 1e-6);
        
        // Check symmetry
        assertEquals(r.computeSimiliarity(rep2, rep1),
                r.computeSimiliarity(rep1, rep2), 1e-6);
        
        // Check that similiary between these structures is less than 1.0
        assertTrue(r.computeSimiliarity(rep2, rep1) < 1.0);
    }
    
    @Test
    public void testModel() throws Exception {
        // Make an example entries
        Cell strc1 = new Cell();
        strc1.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc1.setTypeName(0, "Al");
        CrystalStructureEntry entry1 = new CrystalStructureEntry(strc1, "test", null);
        entry1.setMeasuredClass(1.0);
        
        Cell strc2 = new Cell();
        strc2.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc2.setTypeName(0, "Ni");
        CrystalStructureEntry entry2 = new CrystalStructureEntry(strc2, "test", null);
        entry2.setMeasuredClass(0.5);
        
        
        // Make an example dataset
        CrystalStructureDataset dataset = new CrystalStructureDataset();
        dataset.addEntry(entry1);
        dataset.addEntry(entry2);
        
        // Train a model
        CoulombSineMatrixRegression r = new CoulombSineMatrixRegression();
        r.train(dataset);
        
        // Print citations
        System.out.println(r.getCitations());
    }
    
}
