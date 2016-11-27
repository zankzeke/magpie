package magpie.user.server.operations;

import magpie.data.MultiPropertyDataset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Logan Ward
 */
public class SearchRunnerTest {

    @Test
    public void getEntries() throws Exception {
        SearchRunner sr = new SearchRunner();

        // Generate some entries
        JSONObject searchDescription = new JSONObject()
                .put("datasetType", "materials.CompositionDataset")
                .put("entryGenerator", new JSONArray().put("IonicCompoundGenerator")
                        .put(1).put(2).put(5).put("Na").put("Cl"));
        MultiPropertyDataset data = sr.getEntries(searchDescription);
        assertEquals(1, data.NEntries());
        assertEquals("NaCl", data.getEntry(0).toString());
    }

}