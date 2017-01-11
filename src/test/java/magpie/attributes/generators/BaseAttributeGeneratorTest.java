package magpie.attributes.generators;

import magpie.attributes.generators.composition.StoichiometricAttributeGenerator;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Logan Ward
 */
public class BaseAttributeGeneratorTest {

    @Test
    public void runCommand() throws Exception {
        // Create a simple CompositionDataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("Fe");

        // Create a StoichiometricAttributeGenerator
        StoichiometricAttributeGenerator gen = new StoichiometricAttributeGenerator();
        gen.addPNorm(2);

        // Run it
        List<Object> command = new LinkedList<>();
        command.add("run");
        command.add(data);

        gen.runCommand(command);

        // Check that it created 1 attribute
        assertEquals(2, data.NAttributes());
    }

}