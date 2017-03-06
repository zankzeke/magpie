package magpie.data.utilities.splitters;

import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.filters.AllMetalsFilter;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Logan Ward
 */
public class FilterSplitterTest {

    @Test
    public void test() throws Exception {
        // Make a splitter
        AllMetalsFilter filter = new AllMetalsFilter();

        List<Object> options = new LinkedList<>();
        options.add(filter);

        FilterSplitter splitter = new FilterSplitter();
        splitter.setOptions(options);

        // Make a fake dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Na");

        // Run the splitter
        splitter.train(data);
        List<Dataset> splits = splitter.split(data);

        // Test the results
        assertEquals(2, splits.size());
        assertEquals(1, splits.get(0).NEntries());
        assertEquals("Na", splits.get(0).getEntry(0).toString());
        assertEquals(1, splits.get(1).NEntries());
        assertEquals("NaCl", splits.get(1).getEntry(0).toString());

        // Print out the description
        String description = splitter.printDescription(true);
        assertTrue(description.contains("AllMetalsFilter"));
        System.out.println(description);
    }
}