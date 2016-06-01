package magpie.data.utilities.output;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Write dataset to Weka's ARFF Format.
 * 
 * <p>Notes: 
 * <ol>
 * <li>Weka ARFF importer crashes with names that contain a comma, these will
 * be replaced with underscores
 * <li>This class does support missing class variables
 * </ol>
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * @author Logan Ward
 */
public class ARFFOutput extends BaseDatasetOutput {
    /** Dataset being output. Must be cached for {@linkplain #printEntries(java.util.Collection, java.io.OutputStream)} */
    private Dataset Data;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void printHeader(Dataset data, OutputStream output) {
        PrintWriter fp = new PrintWriter(output, true);
        
        // Print out header block
        fp.write("@RELATION \'Auto-generated arff\'\n\n");
        
        // Print out attribute information
        for (int i = 0; i < data.NAttributes(); i++) {
            fp.format("@ATTRIBUTE %s NUMERIC\n", data.getAttributeName(i).replace(",","_"));
        }
        
        // Print out class information
        if (data.NClasses() == 1) {
            fp.write("@ATTRIBUTE class NUMERIC\n");
        } else {
            String[] ClassNames = data.getClassNames();
            fp.format("@ATTRIBUTE class {%s", ClassNames[0]);
            for (int i = 1; i < data.NClasses(); i++) {
                fp.format(", %s", ClassNames[i].replace(",","_"));
            }
            fp.write("}\n");
        }
        
        // Start the attribute block
        fp.println("@DATA");
        
        // Cache dataset for later use
        Data = data;
    }

    @Override
    public void printEntries(Collection<BaseEntry> entries, OutputStream output) {
        PrintWriter fp = new PrintWriter(output, true);
        
        // Check if printHeader has been run 
        if (Data == null) {
            throw new RuntimeException("printHeader must be run first.");
        }
        
        for (BaseEntry entry : entries) {
            
            // Print out attributes
            for (int a = 0; a < entry.NAttributes(); a++) {
                fp.format("%.6e,", entry.getAttribute(a));
            }
            
            // Print out class value, if applicable
            if (entry.hasMeasurement()) {
                if (Data.NClasses() == 1) {
                    fp.format("%.6e\n", entry.getMeasuredClass());
                } else {
                    fp.format("%s\n", Data.getClassName((int) entry.getMeasuredClass()));
                }
            } else {
                fp.println("?");
            }
        }
    }

    @Override
    public void printEnd(OutputStream output) {
        // Uncache Data
        Data = null;
    }
}
