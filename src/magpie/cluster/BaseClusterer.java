package magpie.cluster;

import java.io.Serializable;
import java.util.List;
import magpie.analytics.ClustererStatistics;
import magpie.attributes.selectors.BaseAttributeSelector;
import magpie.data.Dataset;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.*;

/**
 * Abstract model for all clustering algorithms. Splits dataset into multiple sets based
 * on its attributes. 
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * <command><p><b>train $&lt;dataset></b> - Train a clustering algorithm
 * <br><pr><i>dataset</i>: {@linkplain Dataset} to use for training</command>
 * 
 * <command><p><b>&lt;output> = get &lt;num> $&lt;input></b> - Get a certain cluster from a dataset
 * <br><pr><i>num</i>: Index of cluster to extract
 * <br><pr><i>input</i>: {@linkplain Dataset} containing entries to cluster
 * <br><pr><i>output</i>: {@linkplain Dataset} Single cluster from the input dataset.</command>
 * 
 * <command><p><b>selector $&lt;selector></b> - Define an attribute selector to use before clustering
 * <br><pr><i>selector</i>: {@linkplain BaseAttributeSelector} to use for selection</command>
 * 
 * <command><p><b>split $&lt;dataset> &lt;filename> [&lt;format>]</b> - Split dataset into several clusters, save them
 * <br><pr><i>dataset</i>: {@linkplain Dataset} to be partitioned
 * <br><pr><i>filename</i>: Base filename to use (actual file name will be &lt;filename>&lt;cluster #>.&lt;format>)
 * <br><pr><i>format</i>: Optional: Format to use when saving clusters</command>
 * 
 * <p><b><u>Implemented Print Commands:</u></b>
 * 
 * <print><p><b>selector</b> - Print out attributes used to generate cluster, if an {@linkplain BaseAttributeSelector} was provided</print>
 * 
 * <print><p><b>stats [&lt;command>]</b> - Print out statistics about clustering
 * <br><pr><i>command</i>: Command to be passed to and run by internal {@linkplain ClustererStatistics} class.</print>
 * 
 * <p><b><u>Implementation guide:</u></b>
 * <p>In order to create a new instance of this class, one must implement:
 * <ul>
 * <li>{@link #train_protected} - Train the clusterer</li>
 * <li>{@link #label_protected} - Label entries based on their predicted cluster</li>
 * <li>{@link #NClusters_protected} - Return the number of possible clusters</li>
 * <li>{@link #setOptions} - Define options for this subset given options</li>
 * <li>{@link #clone} - Make sure no clones overwrite data in original</li>
 * </ul>
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseClusterer implements Options, Printable, 
        Serializable, Cloneable, Commandable {
    /** Whether this clusterer has been trained */
    private boolean trained=false;
    /** BaseAttributeSelector used to screen attributes during training */
    private BaseAttributeSelector AttributeSelector = null;
    /** Stores statistics about this clusterer */
    private final ClustererStatistics Statistics = new ClustererStatistics();

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public BaseClusterer clone() {
        BaseClusterer x;
        try {
            x = (BaseClusterer) super.clone(); 
        } catch (CloneNotSupportedException c) {
            throw new Error(c);
        }
        if (AttributeSelector != null) {
            x.AttributeSelector = AttributeSelector.clone();
        }
        return x;
    }

    /**
     * Define an attribute selector to be used before clustering
     * @param AttributeSelector Instantiated Attribute Selector
     */
    public void setAttributeSelector(BaseAttributeSelector AttributeSelector) {
        this.AttributeSelector = AttributeSelector.clone();
    }
    
    
    /**
     * @return Whether this clusterer has been trained
     */
    public boolean isTrained() {
        return trained;
    }
    
    /**
     * Train the clusterer 
     * @param TrainingData Training data 
     */
    public void train(Dataset TrainingData) {
        // Perform attribute selection, if needed
        Dataset Data = TrainingData;
        if (AttributeSelector != null) {
            Data = TrainingData.clone();
            AttributeSelector.train(Data);
            AttributeSelector.run(Data);
        }
        
        // Train the clusterer
        train_protected(Data);
        trained=true;
        
        // Evaluate this clusterer
        Statistics.evaluate(Data.partition(label_protected(Data)));
    }
    
    /** 
     * Perform the actual training.
     * @param Data Dataset to use for training
     */
    abstract protected void train_protected(Dataset Data);
    
    /** 
     * Mark the most likely cluster for each entry
     * @param Data Dataset to label
     * @return List of most likely cluster for each entry
     */
    public int[] label(Dataset Data){
        if (!isTrained())
            throw new Error("Clusterer has not yet been trained");
        
        Dataset ActualData;
        if (AttributeSelector != null) {
            ActualData = Data.clone();
            AttributeSelector.run(ActualData);
        } else 
            ActualData = Data;
        return label_protected(ActualData);
    }
    
    /**
     * Actual implementation of {@link #label(magpie.data.Dataset)}.
     * @param Data Dataset to be labeled
     * @return List of most likely cluster for each entry
     */
    abstract protected int[] label_protected(Dataset Data);
    
    /**
     * How many different clusters data may be partitioned into
     * @return How many clusters are defined
     */
    public int NClusters() {
        if (!isTrained())
            throw new Error("Clusterer has not yet been trained");
        return NClusters_protected();
    }
    
    /**
     * Actual implementation of {@link #NClusters()}.
     * @return How many different clusters are defined
     */
    abstract protected int NClusters_protected();
    
    /**
     * Separate the Dataset based on predicted classes. Does not alter input 
     * @param Data Dataset to be partitioned (does not get altered)s
     * @return Array of datasets containing only entries classified into certain clusters
     */
    public Dataset[] partitionDataset(Dataset Data) {
        return Data.partition(label(Data), NClusters());
    }
    
    /**
     * Retrieve a specific cluster from a dataset. Input dataset is not changed
     * @param From Dataset to be clustered
     * @param number Cluster number to extract
     * @return Dataset containing only entries 
     */
    public Dataset getCluster(Dataset From, int number) {
        Dataset output = From.emptyClone();
        int label[] = label(From);
        for (int i=0; i < From.NEntries(); i++)
            if (label[i] == number)
                output.addEntry(From.getEntry(i));
        return output;
    }

    @Override
    public String about() {
        String Output = "Trained: " + trained;
        if (isTrained())
            Output += " - Number of clusters: " + NClusters();
        return Output;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + "\n";
        
        // Add HTML indentation
        if (htmlFormat) {
            output += "<div style=\"margin-left: 25px\">\n";
        }
        
        // Get model details
        List<String> details = getClustererDetails(htmlFormat);
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
     * Get any user-specified options of this clusterer. 
     * Used with {@linkplain #printDescription(boolean) }
     * @return List of clusterer options
     */
    abstract protected List<String> getClustererDetails(boolean htmlFormat);
    
    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return about();
        switch (Command.get(0).toLowerCase()) {
            case "selector":
                if (AttributeSelector == null)
                    return "No attribute selector used";
                else return AttributeSelector.printSelections();
            case "stats": {
                List<String> SubCommand = Command.subList(1, Command.size());
                if (SubCommand.isEmpty())
                    return Statistics.about();
                else
                    return Statistics.printCommand(SubCommand);
            }
            default:
                throw new Exception("ERROR: Print command \"" + Command.get(0)
                        + "\" not recognized");

        }
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println(about());
            return null;
        }
        String Action = Command.get(0).toString();
        switch (Action) {
            case "get" : {
                // Usage: get <cluster #> $<dataset> = <dataset>
                // Retrieve a specific cluster from a dataset
                Dataset FromData; int ClusterNumber;
                try {
                    if (Command.get(1) instanceof Integer) {
                        ClusterNumber = (Integer) Command.get(1);
                    } else {
                        ClusterNumber = Integer.parseInt(Command.get(1).toString());
                    }
                    FromData = (Dataset) Command.get(2);
                } catch (Exception e) {
                    throw new Exception("get <cluster #> $<input data> = <output data>");
                }
                Dataset ToData = getCluster(FromData, ClusterNumber);
                System.out.println("\tRetreived cluster " + ClusterNumber + ". Entry count: "
                        + ToData.NEntries() 
                        + String.format(" (%.2f%%)", (double) ToData.NEntries() / (double) FromData.NEntries() * 100.0 ));
                return ToData;
            }
            case "selector": {
                // Usage: selector $<selector>
                // Define an attribute selector to use before clustering
                BaseAttributeSelector selector;
                try {
                    selector = (BaseAttributeSelector) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: selector $<selector>");
                }
                setAttributeSelector(selector);
            } break;
            case "split": {
                // Usage: split $<dataset> <filename> [<format>]
                //  Save a dataset into multiple files
                String baseFilename, format;
                Dataset data; 
                try {
                    data = (Dataset) Command.get(1);
                    baseFilename = (String) Command.get(2);
                    if (Command.size() == 4) 
                        format = (String) Command.get(3);
                    else 
                        format = "serialized";
                } catch (Exception e) {
                    throw new Exception("Usage: split $<dataset> <base filename> [<format>]");
                }
                // Split dataset
                Dataset[] subsets = partitionDataset(data);
                for (int i=0; i<subsets.length; i++) {
                    String filename = baseFilename + i;
                    if (format.equalsIgnoreCase("serialized"))
                        UtilityOperations.saveState(subsets[i], filename + ".obj");
                    else
                        subsets[i].saveCommand(filename, format);
                }
                System.out.println("\tSplit data into " + NClusters() 
                        + " clusters and saved in " + format + " format in files named " + baseFilename);
            } break;
            case "train": {
                // Usage: train $<dataset>
                // Train the clusterer on some data
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: <clusterer> train <dataset>");
                }
                train(Data);
                System.out.println("\tTrained clusterer on " + Data.NEntries() + " entries"
                        + ". Number of clusters found: " + NClusters());
            } break;
            default: 
                throw new Exception("ERROR: Clusterer command " + Action
                        + " not recognized.");
        }
        return null;
    }

}
