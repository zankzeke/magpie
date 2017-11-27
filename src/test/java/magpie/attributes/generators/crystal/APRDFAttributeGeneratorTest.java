package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
public class APRDFAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Create dataset
        CrystalStructureDataset data = new CrystalStructureDataset();
        
        Cell strc = new Cell();
        strc.setBasis(new double[]{3.2,3.2,3.2}, new double[]{90,90,90});
        strc.addAtom(new Atom(new double[]{0,0,0}, 0));
        strc.addAtom(new Atom(new double[]{0.5,0.5,0.5}, 1));
        strc.setTypeName(0, "Ni");
        strc.setTypeName(1, "Al");
        
        CrystalStructureEntry entry = new CrystalStructureEntry(strc, "B2-NiAl", null);
        data.addEntry(entry);
        
        // Create entry generator
        APRDFAttributeGenerator gen = new APRDFAttributeGenerator();
        
        // Set the options
        List<Object> options = new LinkedList<>();
        options.add(3.2);
        options.add(2);
        options.add(100);
        options.add("Number");
        
        gen.setOptions(options);
        System.out.println(gen.printUsage());
        
        // Generate the attributes
        gen.addAttributes(data);
        
        // Test results
        assertEquals(2, data.NAttributes());
        System.out.println(gen.printDescription(true));
        System.out.println(gen.getCitations());
        
        //    Get contributions to AP-RDF
        List<double[]> contrs = new ArrayList<>();
        contrs.add(new double[]{2*8*13*28, 3.2*Math.sqrt(3)/2}); // A-B 1st NN
        contrs.add(new double[]{6*13*13, 3.2*1}); // A-A 2nd NN
        contrs.add(new double[]{6*28*28, 3.2*1}); // B-B 2nd NN
        contrs.add(new double[]{8*13*13, 3.2*Math.sqrt(3)}); // A-A 3rd NN
        contrs.add(new double[]{8*28*28, 3.2*Math.sqrt(3)}); // B-B 3rd NN
        
        //    Compute expected values
        double[] evalDist = new double[]{1.6,3.2};
        double[] expectedAPRDF = new double[]{0,0};
        for (int r=0; r<evalDist.length; r++) {
            for (double[] contr : contrs) {
                expectedAPRDF[r] += contr[0] * Math.exp(-100 * Math.pow(contr[1] - evalDist[r],2));
            }
            expectedAPRDF[r] /= 2;
        }
        assertArrayEquals(expectedAPRDF, data.getEntry(0).getAttributes(), 1e-6);
    }
    
}
