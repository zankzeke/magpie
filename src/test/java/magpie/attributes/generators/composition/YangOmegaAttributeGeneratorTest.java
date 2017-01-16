package magpie.attributes.generators.composition;

import magpie.data.materials.CompositionDataset;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;

/**
 * @author Logan Ward
 */
public class YangOmegaAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Create a composition dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("ZrHfTiCuNi");
        data.addEntry("CuNi");
        data.addEntry("CoCrFeNiCuAl0.3");
        data.addEntry("CoCrFeNiCuAl");

        // Compute the attributes
        YangOmegaAttributeGenerator gen = new YangOmegaAttributeGenerator();

        gen.setOptions(new LinkedList<>());

        System.out.println(gen.printUsage());

        gen.addAttributes(data);

        // Check the results
        assertEquals(2, data.NAttributes());
        assertEquals(0.95, data.getEntry(0).getAttribute(0), 1e-2);
        assertEquals(0.1021, data.getEntry(0).getAttribute(1), 1e-2);
        assertEquals(2.22, data.getEntry(1).getAttribute(0), 1e-2);
        assertEquals(0.0, data.getEntry(1).getAttribute(1), 1e-2); // Miracle gives Cu+Ni the same radii

        // Unable to reproduce paper for CoCrFeNiCuAl0.3, I get exactly 1/10th the value of deltaH as reported in the paper
        //  I repeat this calculation by hand (read: Excel), and believe my result to be correct. I get the same values
        //  for deltaS and T_m. I do get the same value for CoCrFeNiCuAl, so I've concluded this is just a typo in the paper
        assertEquals(158.5, data.getEntry(2).getAttribute(0), 2e-1);
        assertEquals(0.0315, data.getEntry(2).getAttribute(1), 1e-2);

        assertEquals(5.06, data.getEntry(3).getAttribute(0), 2e-1);
        assertEquals(0.0482, data.getEntry(3).getAttribute(1), 1e-2);

        System.out.println(gen.getCitations());
    }
}