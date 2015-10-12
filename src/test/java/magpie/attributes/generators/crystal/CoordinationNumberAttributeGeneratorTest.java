package magpie.attributes.generators.crystal;

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
public class CoordinationNumberAttributeGeneratorTest {

    public BaseAttributeGenerator getGenerator() {
        return new CoordinationNumberAttributeGenerator();
    }
    
    public int expectedCount() {
        return 4;
    }
    
    protected CrystalStructureDataset getDataset() {
        CrystalStructureDataset data;
        data = new CrystalStructureDataset();
        data.addElementalProperty("Electronegativity");
        return data;
    }
    
    /**
     * Ensure that attributes are invariant to scaling the volume, permuting 
     * atoms, and creating a supercell
     */
    @Test
    public void runCrystalStructureDatasetTest() throws Exception {
        // Create dataset
        CrystalStructureDataset data;
        data = getDataset();
        
        // Create primitive cell for B2-AlNi
        Cell strc = new Cell();
        strc.setBasis(new double[]{2.88, 2.88, 2.88}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Al");
        strc.setTypeName(1, "Ni");
        AtomicStructureEntry entry = new AtomicStructureEntry(strc, "Primtivie", null);
        data.addEntry(entry);
        
        // Create scaled cell
        strc = new Cell();
        strc.setBasis(new double[]{3.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Al");
        strc.setTypeName(1, "Ni");
        entry = new AtomicStructureEntry(strc, "Scaled", null);
        data.addEntry(entry);
        
        // Create a cell where A & B are swapped
        strc = new Cell();
        strc.setBasis(new double[]{3.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Ni");
        strc.setTypeName(1, "Al");
        entry = new AtomicStructureEntry(strc, "Primtivie", null);
        data.addEntry(entry);
        
        // Create a 2x1x1 supercell
        strc = new Cell();
        strc.setBasis(new double[]{6.0, 3.0, 3.0}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.25,0.5,0.5}, 1));
        strc.addAtom(new Atom(new double[]{0.75,0.5,0.5}, 1));
        strc.setTypeName(0, "Ni");
        strc.setTypeName(1, "Al");
        entry = new AtomicStructureEntry(strc, "Primtivie", null);
        data.addEntry(entry);
        
        // Generate attributes
        BaseAttributeGenerator gen = getGenerator();
        gen.addAttributes(data);
        
        // Make sure the correct number were generated
        assertEquals(expectedCount(), data.NAttributes());
        for (BaseEntry e : data.getEntries()) {
            assertEquals(expectedCount(), e.NAttributes());
        }
        
        // Make sure scaling doesn't effect it
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a) + " fails scaling",
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(1).getAttribute(a),
                    1e-6);
        }
        
        // Make sure its permutationally-invariant
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a) + " fails permutation",
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(2).getAttribute(a),
                    1e-6);
        }
        
        // Make sure it passes supercell
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a) + " fails supercell",
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(3).getAttribute(a),
                    1e-6);
        }
        
        // Print description
        System.out.println(gen.printDescription(true));
    }
    
}
