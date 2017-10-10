package magpie.data.utilities.output;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.utility.tools.BatchAttributeGenerator;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;

/**
 * @author Logan Ward
 */
public class JSONOutputTest {

    @Test
    public void testOutput() throws Exception {
        // Make a simple dataset
        Dataset data = new Dataset();
        data.addEntry(new BaseEntry());
        data.addEntry(new BaseEntry());

        data.addAttribute("x1", new double[]{0, 1});
        data.addAttribute("x2", new double[]{1, 0});

        data.getEntry(0).setMeasuredClass(1.0);

        // Turn it into JSON
        JSONOutput output = new JSONOutput();
        output.setOptions(new LinkedList<>());

        File testFile = new File("test.json");
        testFile.deleteOnExit();

        output.writeDataset(data, testFile);

        // Parse the JSON file
        JSONObject result = new JSONObject(new JSONTokener(new FileInputStream(testFile)));
        System.out.println(result.toString(2));
    }

    @Test
    public void testBatchOutput() throws Exception {
        // Read in the composition dataset
        CompositionDataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);

        // Create the batch generator
        BatchAttributeGenerator generator = new BatchAttributeGenerator();
        generator.setBatchSize(10);

        // Make the JSON output
        JSONOutput output = new JSONOutput();
        generator.setOutputter(output);

        // Run it
        File testFile = new File("test.json");
        testFile.deleteOnExit();
        generator.output(data, testFile);

        // Make sure it works
        JSONObject result = new JSONObject(new JSONTokener(new FileInputStream(testFile)));
    }
}