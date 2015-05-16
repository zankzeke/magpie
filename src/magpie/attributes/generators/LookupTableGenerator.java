package magpie.attributes.generators;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Tool to add precomputed attributes from a lookup table. Use this if you'd like
 * to compute attributes externally and them to a dataset.
 * 
 * <p>Lookup table should follow the format:
 * 
 * <p>entry attributeName1 attributeName2 [...]
 * [string describing entry #1] [attribute 1 value] [...]
 * 
 * <p>When supplied with a dataset, the dataset is first used to parse
 * the strings describing each entry. These are then match to appropriate entries 
 * in that dataset. If no matching entry in the lookup table is found for an
 * entry in the dataset, its attributes are set to NaN.
 * 
 * <usage><p><b>Usage</b>: &lt;path&gt;
 * <br><pr><i>path</i>: Path to lookup table</usage>
 * @author Logan Ward
 */
public class LookupTableGenerator extends BaseAttributeGenerator {
    /** 
     * Lookup table for attributes. Key is a string describing an entry,
     * value is a list of attributes to be added.
     */
    private List<Pair<String, double[]>> LookupTable = new LinkedList<>();
    /** New attribute names */
    private List<String> NewNames = new LinkedList<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String path;
        if (Options.size() != 1) {
            throw new Exception(printUsage());
        }
        path = Options.get(0).toString();
        readAttributeTable(path);
    }

    @Override
    public String printUsage() {
        return "Usage: <path to lookup table>";
    }
    
    /**
     * Read in lookup table containing attribute names and values for selected entries.
     * @param path Path to lookup table
     * @throws IOException 
     */
    public void readAttributeTable(String path) throws IOException {
        LookupTable.clear();
        
        // Open the file
        BufferedReader fp = new BufferedReader(new FileReader(path));
        
        // Read the header
        String line = fp.readLine();
        String[] words = line.split("[ \t]");
        NewNames = new ArrayList<>(words.length - 1);
        for (int i=1; i<words.length; i++) {
            NewNames.add(words[i]);
        }
        
        // Read in the entries
        while (true) {
            line = fp.readLine();
            if (line == null) {
                break;
            }
            words = line.split("[ \t]");
            String name = words[0];
            double[] attributes = new double[words.length - 1];
            for (int i=1; i<words.length; i++) {
                try {
                    attributes[i-1] = Double.parseDouble(words[i]);
                } catch (NumberFormatException e) {
                    attributes[i-1] = Double.NaN;
                }
            }
            if (attributes.length != NewNames.size()) {
                System.err.format("Wrong number of attributes for entry \"%s\": %d != %d",
                        name, attributes.length, NewNames.size());
                continue;
            }
            LookupTable.add(new ImmutablePair<>(name, attributes));
        }
        
        // Close the file
        fp.close();
    }

    @Override
    public void addAttributes(Dataset data) {
        // Step #1: Parse all of the entries
        Dataset tempDataset = data.emptyClone();
        Map<BaseEntry,double[]> entryLookup = new TreeMap<>();
        for (Pair<String,double[]> val : LookupTable) {
            try {
                tempDataset.addEntry(val.getKey());
                entryLookup.put(tempDataset.getEntry(0), val.getRight());
                tempDataset.clearData();
            } catch (Exception e) {
                // Do nothing
            }
        }
        
        // Step #2: Add new attributes
        data.addAttributes(NewNames);
        double[] blanks = new double[NewNames.size()];
        Arrays.fill(blanks, Double.NaN);
        for (BaseEntry entry : data.getEntries()) {
            double[] newAttr = entryLookup.get(entry);
            if (newAttr == null) {
                entry.addAttributes(blanks);
            } else {
                entry.addAttributes(newAttr);
            }
        }
    }
    
}
