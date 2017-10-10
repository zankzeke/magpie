package magpie.data.utilities.output;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

/**
 * Output data into JSON format.
 *
 * @author Logan Ward
 */
public class JSONOutput extends BaseDatasetOutput {
    /** Whether the entry output phase has been started */
    protected boolean EntriesStarted = false;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (!Options.isEmpty()) {
            throw new IllegalArgumentException(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void printHeader(Dataset data, OutputStream output) {
        // Create a buffered output
        PrintWriter fp = new PrintWriter(output, true);

        // Generate the header information for the dataset
        Dataset noEntries = data.emptyClone();
        JSONObject dataJSON = noEntries.toJSON();

        // Print the start of the JSON file
        fp.println("{");

        // Loop through all keys _except_ entries
        for (String key : dataJSON.keySet()) {
            // Skip entries
            if (key.equalsIgnoreCase("entries")) {
                continue;
            }

            // Print the key and value
            Object value = dataJSON.get(key);
            fp.print("  "); // Indentation of 2 spaces
            String newLine = new JSONStringer().object().key(key).value(value).endObject().toString();
            fp.print(newLine.substring(1, newLine.length() - 1));
            fp.println(",");
        }

        // Print the beginning of the entries class
        fp.println("  \"entries\": [");

        // Reset the "entries started" flag
        EntriesStarted = false;
    }

    @Override
    public void printEntries(Collection<BaseEntry> entries, OutputStream output) {
        // Initialize a printwriter
        PrintWriter fp = new PrintWriter(output, true);

        // Loop through entries, printing out their JSON version
        for (BaseEntry entry : entries) {
            // Make it into a JSON object
            JSONObject entryJSON = entry.toJSON();

            // If this isn't the first entry, add a ","
            if (EntriesStarted) {
                fp.print(",");
            } else {
                EntriesStarted = true;
            }

            // Append it to the array, adding additional indentation to each line
            fp.println(entryJSON);
        }
    }

    @Override
    public void printEnd(OutputStream output) {
        // Initialize a printwriter
        PrintWriter fp = new PrintWriter(output, true);

        // Close the entry block
        fp.println("  ]");

        // Close the JSON object
        fp.println("}");
    }
}
