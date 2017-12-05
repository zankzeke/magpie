package magpie.data.utilities.generators;

import magpie.data.BaseEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Logan Ward
 */
public class IonicCompoundGeneratorTest {

    @Test
    public void testGenerator() throws Exception {
        // Make options
        List<Object> options = new LinkedList<>();
        options.add(1);
        options.add(2);
        options.add(10);
        options.add("Na");
        options.add("Li");
        options.add("Cl");

        // Create the generator
        IonicCompoundGenerator gen = new IonicCompoundGenerator();

        gen.setOptions(options);

        // Train and run it
        CompositionDataset data = new CompositionDataset();
        gen.train(data);

        List<BaseEntry> output = IteratorUtils.toList(gen.iterator());
        assertEquals(2, output.size());
        assertTrue(output.contains(new CompositionEntry("NaCl")));
        assertTrue(output.contains(new CompositionEntry("LiCl")));
    }
}