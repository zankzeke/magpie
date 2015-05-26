package magpie.data.utilities.generators;

import org.junit.BeforeClass;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Test phase diagram generator
 * @author Logan Ward
 */
public class PhaseDiagramCompositionEntryGeneratorTest {
    
	@Test
	public void testGenerateAlloyCompositions() throws Exception {
		PhaseDiagramCompositionEntryGenerator generator = new PhaseDiagramCompositionEntryGenerator();
		generator.setEvenSpacing(true);
        generator.setSize(5);
        generator.setOrder(1, 3);
		Map<Integer, List<double[]>> comps = generator.generateAlloyCompositions();
		assertEquals(1, comps.get(1).size());
		assertEquals(3, comps.get(2).size());
		assertEquals(3, comps.get(3).size());
        generator.setOrder(3, 3);
        comps = generator.generateAlloyCompositions();
        assertEquals(1, comps.size());
	}

	/**
	 * Test of generateCrystalCompositions method, of class PhaseDiagramCompositionEntryGenerator.
	 */
	@Test
	public void testGenerateCrystalComposition() throws Exception {
		PhaseDiagramCompositionEntryGenerator generator = new PhaseDiagramCompositionEntryGenerator();
		generator.setEvenSpacing(false);
        generator.setSize(4);
        generator.setOrder(1, 3);
		Map<Integer, List<double[]>> comps = generator.generateCrystalCompositions();
		assertEquals(1, comps.get(1).size());
		assertEquals(5, comps.get(2).size());
		assertEquals(4, comps.get(3).size());
        generator.setOrder(2, 3);
        comps = generator.generateAlloyCompositions();
        assertEquals(2, comps.size());
	}
}
