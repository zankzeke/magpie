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
	/**
	 * Test of generateAlloyCompositions method, of class PhaseDiagramCompositionEntryGenerator.
	 */
	@Test
	public void testGenerateAlloyCompositions() {
		PhaseDiagramCompositionEntryGenerator generator = new PhaseDiagramCompositionEntryGenerator();
		generator.setEvenSpacing(true);
		try {
			generator.setSize(5);
			generator.setOrder(3);
		} catch (Exception e) {
			throw new Error(e);
		}
		Map<Integer, List<double[]>> comps = generator.generateAlloyCompositions();
		assertEquals(1, comps.get(1).size());
		assertEquals(3, comps.get(2).size());
		assertEquals(3, comps.get(3).size());
	}

	/**
	 * Test of generateCrystalCompositions method, of class PhaseDiagramCompositionEntryGenerator.
	 */
	@Test
	public void testGenerateCrystalComposition() {
		PhaseDiagramCompositionEntryGenerator generator = new PhaseDiagramCompositionEntryGenerator();
		generator.setEvenSpacing(false);
		try {
			generator.setSize(4);
			generator.setOrder(3);
		} catch (Exception e) {
			throw new Error(e);
		}
		Map<Integer, List<double[]>> comps = generator.generateCrystalCompositions();
		assertEquals(1, comps.get(1).size());
		assertEquals(5, comps.get(2).size());
		assertEquals(4, comps.get(3).size());
	}
}
