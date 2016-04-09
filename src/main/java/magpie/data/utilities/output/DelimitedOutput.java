package magpie.data.utilities.output;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Write data file out to a delimited file (ex: comma separated values). The first
 * row of this datafile includes names of the attributes and the name of the class 
 * variable. This header is then followed by a row containing the attributes 
 * and measured class (if available, None otherwise) for each entry.
 * 
 * <p>Note: If the name of a variable contains the text to use as the delimiter,
 * it will be replaced with a "_". If "_" is the delimiter, "_" will be replaced
 * with a "-"
 * 
 * <usage><p><b>Usage</b>: &lt;delimiter&gt;
 * <br><pr><i>delimiter</i>: Type of delimeter. Put whitespace inside of quotation
 * marks. This command accepts escaped characters</usage>
 * 
 * @author Logan Ward
 */
public class DelimitedOutput extends BaseDatasetOutput {
    /** Text used as delimeter. Default = "," */
    protected String Delimiter = ",";

    public DelimitedOutput() {
    }

    /**
     * Create a file writer with a certain delimiter
     * @param delimiter Desired delimiter
     */
    public DelimitedOutput(String delimiter) {
        this.Delimiter = delimiter;
    }
    
    

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() != 1) {
            throw new Exception(printUsage());
        }
        
        // Get delimiter
        String delim = Options.get(0).toString();
        delim = StringEscapeUtils.unescapeJava(delim);
        setDelimiter(delim);
    }

    @Override
    public String printUsage() {
        return "Usage: <delimiter>";
    }

    /**
     * Set the delimiter used to separate words
     * @param Delimiter 
     */
    public void setDelimiter(String Delimiter) {
        this.Delimiter = Delimiter;
    }

    /**
     * Get string used to separate words in output file
     * @return The delimiter
     */
    public String getDelimiter() {
        return Delimiter;
    }
    
    @Override
    public void printHeader(Dataset data, OutputStream output) {
        // Get a buffered version of the output
        PrintWriter fp = new PrintWriter(output, true);
        
        // Get the attribute names
        String[] attributeNames = data.getAttributeNames();
        boolean started = false;
        for (String name : attributeNames) {
            // Clean the name
            if (name.contains(Delimiter)) {
                name = replaceDelimiter(name);
            }
            
            // Print the name
            if (! started) {
                fp.print(name);
                started = true;
            } else {
                fp.print(Delimiter+name);
            }
        }
        
        // Get the class name
        String name = data.getClassName(0);
        if (name.contains(Delimiter)) {
            name = replaceDelimiter(name);
        }
        fp.println(Delimiter+name);
    }
    
    /**
     * Replace a delimiter in a string with something else. Unless the delimiter
     * is "_", the delimiter will be replaced with a "_". In that case, 
     * it will be replaced with a ",".
     * @param string String to be cleaned
     * @return String with the desired delimiter
     */
    protected String replaceDelimiter(String string) {
        if ("_".equals(Delimiter)) {
            return string.replaceAll("_", ",");
        } else {
            return string.replaceAll(Delimiter, "_");
        }
    }

    @Override
    public void printEntries(Collection<BaseEntry> entries, OutputStream output) {
        // Get a buffered version of the output
        PrintWriter fp = new PrintWriter(output, true);
        
        // Print each entry
        boolean started;
        for (BaseEntry entry : entries) {
            started = false;
            
            // Print each attribute
            for (double x : entry.getAttributes()) {
                if (started) {
                    fp.print(Delimiter);
                    fp.print(x);
                } else {
                    started = true;
                    fp.print(x);
                }
            }
            
            // Print the measured class, if available
            if (entry.hasMeasurement()) {
                fp.print(Delimiter);
                fp.println(entry.getMeasuredClass());
            } else {
                fp.println(Delimiter + "None");
            }
        }
    }

    @Override
    public void printEnd(OutputStream output) {
        // Do nothing
    }
}
