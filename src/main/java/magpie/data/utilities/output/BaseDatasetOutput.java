package magpie.data.utilities.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.interfaces.Options;

/**
 * Base class for tools that output a dataset. 
 * 
 * <p>To best user this class, call the appropriate {@linkplain #writeDataset} command
 * for your desired output method (e.g., generic stream, file). This will
 * automatically write the entire file. Alternatively, you can call the commands
 * to print the header, entries, and end separately. This might be a be a preferred
 * route if you are, for example, reading entries from a stream and do not want 
 * to hold the entire dataset in memory at the same time. But, it is otherwise 
 * recommended to use the basic interface.
 * 
 * <p>Developer's Notes: This class is designed around 
 * the idea that data files are composed of two segments: a header describing 
 * data format, the body containing the data, and some text marking the end of 
 * a file. Consequently, any implementation of these class must fulfill 3 operations:
 * 
 * <ol>
 * <li>{@linkplain #printHeader(magpie.data.Dataset, java.io.OutputStream) }
 * <li>{@linkplain #printEntries(java.util.Collection, java.io.OutputStream) }
 * <li>{@linkplain #printEnd(java.io.OutputStream) }
 * </ol>
 * 
 * @author Logan Ward
 */
abstract public class BaseDatasetOutput implements Options {
    
    /**
     * Write a dataset to disk
     * @param data Dataset to be output
     * @param filename Path to output file
     * @throws java.io.IOException
     */
    public void writeDataset(Dataset data, String filename) throws IOException {
        File file = new File(filename);
        writeDataset(data, file);
    }
    
    /**
     * Write dataset to disk
     * @param data Dataset to be output
     * @param file Output file
     * @throws java.io.IOException
     */
    public void writeDataset(Dataset data, File file) throws IOException {
        OutputStream output = new FileOutputStream(file);
        writeDataset(data, output);
        output.close();
    }
    
    /**
     * Write a dataset to a output stream. Writes both header and body
     * @param data Dataset to be output
     * @param output Output stream for data
     */
    public void writeDataset(Dataset data, OutputStream output) throws IOException {
        printHeader(data, output);
        printEntries(data.getEntries(), output);
        printEnd(output);
    }
    
    /**
     * Write header to output. This generally describes the names of attributes
     * @param data Dataset to be written 
     * @param output Output stream
     */
    abstract public void printHeader(Dataset data, OutputStream output);
    
    /**
     * Write out entries to file. This should only write information about 
     * entries. If the entry block of a file format has a header, this 
     * should be written out in print header.
     * @param entries Entries to be written
     * @param output Output stream
     */
    abstract public void printEntries(Collection<BaseEntry> entries, OutputStream output);
    
    /**
     * Print the end to a data file
     * @param output Output stream
     */
    abstract public void printEnd(OutputStream output);
}
