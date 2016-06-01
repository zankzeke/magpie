package magpie.data.utilities.output;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;

/**
 * Write attributes and properties out to a CSV file.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class PropertiesOutput extends BaseDatasetOutput {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (!Options.isEmpty()) {
            throw new Exception();
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void printHeader(Dataset data, OutputStream output) {
        if (! (data instanceof MultiPropertyDataset)) {
            throw new RuntimeException("Data must fulfill MultiPropertyDataset");
        }
        
        // Get a buffered version of the output
        PrintWriter fp = new PrintWriter(output, true);
        
        // Print out attribute names
        printAttributeNames(data, fp);
        
        // Print class
        fp.print(",class_measured,class_predicted");
        
        // Print properties
        for (String name : ((MultiPropertyDataset) data).getPropertyNames()) {
            name = name.replace(",", "-");
            fp.print("," + name + "_measured");
            fp.print("," + name + "_predicted");
        }
        fp.println();
    }

    /**
     * Print out attribute names
     * @param data Dataset being printed
     * @param fp Output stream
     */
    protected void printAttributeNames(Dataset data, PrintWriter fp) {
        // Get the attribute names
        String[] attributeNames = data.getAttributeNames();
        boolean started = false;
        for (String name : attributeNames) {
            // Clean the name
            name = name.replace(",", "-");
            
            // Print the name
            if (! started) {
                fp.print(name);
                started = true;
            } else {
                fp.print(","+name);
            }
        }
    }

    @Override
    public void printEntries(Collection<BaseEntry> entries, OutputStream output) {
        // Get a buffered version of the output
        PrintWriter fp = new PrintWriter(output, true);
        
        // Print each entry
        boolean started;
        for (BaseEntry entryPtr : entries) {
            // Get MultiPropertyEntry reference
            MultiPropertyEntry entry = (MultiPropertyEntry) entryPtr;
            
            // Print out attributes
            printEntryAttributes(entry, fp);
            
            // Print out properties
            printEntryProperties(entry, fp);
            
            // Print newline
            fp.println();
        }
    }

    /**
     * Print out measured and predicted properties of an entry
     * @param entry Entry to be printed
     * @param fp Output stream
     */
    protected void printEntryProperties(MultiPropertyEntry entry, PrintWriter fp) {
        // Store previous target
        int target = entry.getTargetProperty();
        
        // Print the measured and predicted class, if available
        if (entry.hasMeasurement()) {
            fp.print(",");
            fp.print(entry.getMeasuredClass());
        } else {
            fp.print(",None");
        }
        if (entry.hasPrediction()) {
            fp.print(",");
            fp.print(entry.getPredictedClass());
        } else {
            fp.print(",None");
        }
        
        // Print out each property
        for (int p=0; p<entry.NProperties(); p++) {
            if (entry.hasMeasuredProperty(p)) {
                fp.print(",");
                fp.print(entry.getMeasuredProperty(p));
            } else {
                fp.print(",None");
            }
            if (entry.hasPredictedProperty(p)) {
                fp.print(",");
                fp.print(entry.getPredictedProperty(p));
            } else {
                fp.print(",None");
            }
        }
        
        // Restore target property
        entry.setTargetProperty(target);
    }
    

    /**
     * Print out attributes of an entry
     * @param entry Entry to be printed
     * @param fp Output stream
     */
    protected void printEntryAttributes(MultiPropertyEntry entry, PrintWriter fp) {
        boolean started = false;
        for (double x : entry.getAttributes()) {
            if (started) {
                fp.print(",");
                fp.print(x);
            } else {
                started = true;
                fp.print(x);
            }
        }
    }

    @Override
    public void printEnd(OutputStream output) {
        // Nothing
    }

}
