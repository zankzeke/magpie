package magpie.data.utilities.splitters;

import java.util.ArrayList;
import org.apache.commons.lang3.math.NumberUtils;
import java.util.Iterator;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;
import magpie.utility.interfaces.Printable;

/**
 * This class provides an interface to Dataset splitting operations. Any implementation 
 * must fill the following operations:
 * <ul>
 * <li>{@link #label} - Returns a list of integers that specify which subset each entry belongs to</li>
 * <li>{@link #setOptions} - Define settings for this splitter</li>
 * <li>{@link #train} - Train a Dataset splitter, if needed</li>
 * </ul>
 * 
 * <p><b><u>Implemented Commands</u></b>
 * 
 * <command><p><b>&lt;output&gt; = get &lt;partition number&gt; $&lt;dataset&gt;</b> - 
 *  Get a certain partition from a dataset
 * <br><pr><i>partition number</i>: Index (starting at 0) of desired partition
 * <br><pr><i>dataset</i>: Dataset to be split</command>
 * 
 * <command><p><b>save $&lt;data&gt; &lt;filename&gt; [&lt;format&gt;]</b> -
 *  Split a dataset, then print it out to files
 * <br><pr><i>data</i>: {@linkplain Dataset}to be printed
 * <br><pr><i>filename</i>: Names of files. Output will be this + split number for
 * each split.
 * <br><pr><i>format</i>: Optional: Format in which to write data (default=serialized)</command>
 * 
 * <command><p><b>train $&lt;data&gt;</b> - Train splitter
 * <br><pr><i>data</i>: {@linkplain Dataset} used to train splitter</command>
 * 
 * <p><b><u>Print Commands</u></b>
 * 
 * <print><b>details</b> - Description of this splitter</print>
 * 
 * <print><b>names</b> - Names of the splits</print>
 *
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseDatasetSplitter implements 
        java.io.Serializable, Options, Commandable, Cloneable, Printable {

    @Override
    public BaseDatasetSplitter clone() {
        try {
            BaseDatasetSplitter spltr = (BaseDatasetSplitter) super.clone();
            return spltr;
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }
    
    /** 
     * Given a dataset, determine which subset each entry should belong to. Please
     * ensure that D remains unaltered (clone if you must)
     * @param D Dataset to be labeled
     * @return A list of nonnegative integers marking which subset to split each entry into
     */
    abstract public int[] label(Dataset D);
    
    /** 
     * Splits a dataset into several partitions.
     * @param data Dataset to be split. Returns empty from this operation
     * @return List of classes where each entry is a different partition
     */
    public List<Dataset> split(Dataset data) {
        return split(data, false);
    }
    
    /**
     * Split a dataset into several partitions. 
     * @param data Dataset to be split
     * @param toRetain Whether to keep entries in the original dataset
     * @return List of classes where each entry is a different partition
     */
    public List<Dataset> split(Dataset data, boolean toRetain) {
        int[] labels = label(data);
        List<Dataset> output = new ArrayList<>(NumberUtils.max(labels)+1);
        for (int i=0; i<=NumberUtils.max(labels); i++)
            output.add(data.emptyClone());
        
        // Split it up
        Iterator<BaseEntry> iter = data.getEntries().iterator();
        int i=0; 
        while (iter.hasNext()) {
            BaseEntry E = iter.next();
            output.get(labels[i]).addEntry(E);
            i++; 
        }
        
        // If not to retain, clear input data
        if (! toRetain) {
            data.clearData();
        }
        
        return output;
    }
    
    /**
     * Train a dataset splitter, if necessary
     * @param TrainingSet Dataset to use for training
     */
    abstract public void train(Dataset TrainingSet);

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println("This is a: " + this.getClass().getSimpleName());
            return null;
        }
        String action = Command.get(0).toString().toLowerCase(); 
        switch (action) {
            case "get": {
                // Usage: get <partition number> $<data>
                int index;
                Dataset data;
                try {
                    index = Integer.parseInt(Command.get(1).toString());
                    data = (Dataset) Command.get(2);
                } catch (Exception e) {
                    throw new Exception("Usage: <output> = get <index> $<dataset>");
                }
                List<Dataset> splits = split(data, true);
                if (splits.size() < index) {
                    throw new Exception("Only " + splits.size() + " partitions." +
                            " Requested #" + index);
                }
                System.out.println("\tRetrieved partition " + index + ".");
                return splits.get(index);
            }
            case "save": {
                // Usage: save $<dataset> <filename> [<format>]
                Dataset data;
                String filename, format;
                try {
                    data = (Dataset) Command.get(1);
                    filename = Command.get(2).toString();
                    if (Command.size() > 3) {
                        format = Command.get(3).toString();
                    } else {
                        format = null;
                    }
                } catch (Exception e) {
                    throw new Exception("Usage: save $<data> <filename> [<format>]");
                }
                List<Dataset> splits = split(data, true);
                for (int s=0; s<splits.size(); s++) {
                    String name = filename + s;
                    if (format == null) {
                        splits.get(s).saveState(name);
                    } else {
                        splits.get(s).saveCommand(name, format);
                    }
                }
                System.out.format("\tSaved %d partitions to files starting with %s%s.\n",
                        splits.size(),
                        filename,
                        format == null ? "" : " in " + format + " format");
                return null;
            }
            case "train": {
                // Usage: train $<data>
                Dataset data;
                try {
                    if (Command.size() != 2) {
                        throw new Exception();
                    }
                    data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: train $<data>");
                }
                train(data);
                System.out.println("\tTrained splitter using " + data.NEntries());
                return null;
            } 
            default:
                throw new Exception("Command not recognized: " + action);
        }
    }

    @Override
    public String about() {
        return "Splits dataset into " + getSplitNames().size() + " groups";
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + "\n";
        
        // Get splitter details
        List<String> details = getSplitterDetails(htmlFormat);
        
        // Shortcut
        if (details.isEmpty()) {
            return output;
        }
        
        // Add HTML indentation
        if (htmlFormat) {
            output += "<div style=\"margin-left: 25px\">\n";
        }
        
        boolean started = false;
        String lastLine = "";
        for (String line : details) {
            output += "\t";
            
            // Add <br> where appropriate
            if (started && // Not for the first line int the block
                    htmlFormat // Only for HTML-formatted output
                    // Not on lines for the "<div>" tags
                    && ! (line.contains("<div") || line.contains("</div>")) 
                    // Not immediately after <div> tags
                    && ! (lastLine.contains("<div") || lastLine.contains("</div>")) 
                    // Not if the line already has a break
                    && ! line.contains("<br>")) {
                output += "<br>";
            }
            
            // Add line to ouput
            output += line + "\n";
            
            // Update loop variables
            started = true;
            lastLine = line;
        }
        
        // Deindent
        if (htmlFormat) {
            output += "</div>\n";
        }
        
        return output;
    }
    
    /**
     * Get details of splitter. Returns a list of options that can be set by the user.
     * Used with {@linkplain #printDescription(boolean) }.
     * @param htmlFormat Whether to format results with HTML
     * @return List of options describing the model
     */
    abstract protected List<String> getSplitterDetails(boolean htmlFormat);

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) {
            return about();
        }
        
        String action = Command.get(0).toLowerCase();
        switch (action) {
            case "details":
                return printDescription(false);
            case "names":
                String output = "Split Names:\n";
                List<String> names = getSplitNames();
                int maxLength = Integer.MIN_VALUE;
                for (String name : names) {
                    maxLength = Math.max(maxLength, name.length());
                }
                for (int i=0; i<names.size(); i++) {
                    output += String.format("    %" + maxLength + "s", names.get(i));
                    if (i % 2 == 1) {
                        output += "\n";
                    }
                }
                return output;
            default:
                throw new Exception("Print command not recognized: " + action);
        }
    }
    
    /**
     * Get the names of the splits this splitter creates
     * @return Names of each group
     */
    abstract public List<String> getSplitNames();
    
}
