package magpie.statistics.performance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.cluster.BaseClusterer;
import magpie.data.Dataset;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.Printable;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Characterize the results from a clusterer. For now, this class will measure:
 * <ul>
 * <li>How many entries where clustered.</li>
 * <li>Number of attributes used during clustering</li>
 * <li>Number of clusters formed</li>
 * <li>Number of entries in each cluster</li>
 * <li>Mean in each attribute for each cluster, sorted by which attribute has the greatest deviation between classes</li>
 * </ul> 
 * 
 * <p>Developer's Note: This class is not yet {@linkplain Commandable}
 * 
 * <p><b><u>Implemented Print Commands:</u></b>
 * 
 * <print><p><b>dist</b> - Print fraction of entries entries in each cluster</print>
 * 
 * <print><p><b>attr [&lt;number>]</b> - Prints out clustering power of attributes (ordered by power)
 * <br><pr><i>number</i>: Number of top attributes to print (default = all of them)</print>
 * 
 * <print><p><b>clust [&lt;number attr>]</b> - Print out the mean of each attribute of all entries in each cluster
 * <br><pr><i>number attr</i>: Number of attributes to print (default = all of them)</print>
 * 
 * @author Logan Ward
 * @version 0.1
 * @see BaseClusterer
 */
public class ClustererStatistics implements Serializable, Printable {
    /** Whether any clusterer has been evaluated */
    private boolean trained = false;
    /** How many entries were considered */
    private int NEntries;
    /** List of attribute used during clustering */
    private List<String> AttributesUsed;
    /** Number of clusters formed */
    private int NClusters;
    /** Number of entries in each cluster */
    private int[] ClusterSize;
    /** Mean of each attribute for each cluster */
    private double[][] ClusterMean;
    /** Power of each attribute */
    private double[] AttributePower;
    /** Ranks for each attribute based on deviation between clusters. The first
     entry in this array is the index of the most powerful attribute. Note that this
     gets sorted in ascending order. */
    private int[] AttributeRanks;

    /**
     * @return Whether this object has evaluated at Clusterer.
     */
    public boolean isTrained() {
        return trained;
    }
    
    /**
     * Characterize the performance of a cluster
     * @param Clusters Clusters formed by clusterer during training
     */
    public void evaluate(Dataset[] Clusters) {
        if (Clusters.length == 0)
            throw new Error("No clusters were formed");
        trained = true;
        // Evaluate the number of entries in each cluster
        NEntries = 0; NClusters = Clusters.length;
        ClusterSize = new int[NClusters];
        for (int i=0; i<NClusters; i++) {
            ClusterSize[i] = Clusters[i].NEntries();
            NEntries += ClusterSize[i]; 
        }
        if (NEntries == 0)
            throw new Error("All clusters were empty");
        // Evaluate each attribute
        AttributesUsed = new ArrayList<>(Arrays.asList(Clusters[0].getAttributeNames()));
        ClusterMean = new double[NClusters][AttributesUsed.size()];
        for (int c=0; c<NClusters; c++) {
            if (Clusters[c].NAttributes() != AttributesUsed.size())
                throw new Error("Cluster " + c + " has different number of attributes");
            for (int a=0; a<AttributesUsed.size(); a++) {
                double[] attributes = Clusters[c].getSingleAttributeArray(a);
                ClusterMean[c][a] = StatUtils.mean(attributes);
            }
        }
        // Rank attributes accordining deviation between clusters 
        AttributePower = rankAttributes(Clusters);
        AttributeRanks = UtilityOperations.sortAndGetRanks(AttributePower, true);
    }
    
    /**
     * Provide some ranking for how well data is clustered based on attributes. 
     * For now, compares deviation between the mean of an attribute between clusters 
     * to mean standard deviation of an attribute within each cluster. 
     * <p>This implementation assumes that {@link #ClusterMean} has already been computed
     * @param Clusters Clusters supplied during training
     * @return Clustering power of each attribute
     */
    private double[] rankAttributes(Dataset[] Clusters) {
        double[] power = new double[AttributesUsed.size()];
        for (int a=0; a<power.length; a++) {
            // Step 1: Calculate the standard deviation within each cluster
            double[] deviationWithin = new double[NClusters];
            for (int c=0; c<NClusters; c++) {
                deviationWithin[c] = StatUtils.variance(Clusters[c].getSingleAttributeArray(a));
                deviationWithin[c] = Math.sqrt(deviationWithin[c]);
            }
            // Step 2: Calculate the mean standard deviation over all clusters
            double meanDeviationWithin = StatUtils.mean(deviationWithin);
            // Step 3: Calculate the standard deviation in the means of each cluster
            double deviationBetween = 0, meanBetween = 0;
            for (int c=0; c<NClusters; c++)
                meanBetween += ClusterSize[c] * ClusterMean[c][a];
            meanBetween /= NEntries;
            for (int c=0; c<NClusters; c++)
                deviationBetween += ClusterSize[c] * Math.pow(meanBetween - ClusterMean[c][a], 2.0);
            deviationBetween /= NEntries;
            // Step 4: Calculate the power
            if (meanDeviationWithin == 0 && deviationBetween > 0)
                power[a] = Double.POSITIVE_INFINITY;
            else if (meanDeviationWithin == 0 && deviationBetween == 0)
                power[a] = 0.0;
            else
                power[a] = deviationBetween / meanDeviationWithin;
        }
        return power;
    }

    @Override
    public String about() {
        if (! isTrained())
            return "Not trained yet";
        else
            return String.format("Number of clusters = %d - Number of entries = %d", ClusterSize.length, NEntries);
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Print the distribution between each cluster
     * @return String accounting for distribution between each cluster
     */
    public String printDistribution() {
        String output = "Distribution between clusters:\n";
        for (int c=0; c < NClusters; c++) {
            output += String.format("\tCluster #%d: %d (%.2f%%)\n", c, ClusterSize[c], 
                    ((double) ClusterSize[c]) / NEntries * 100.0);
        }
        return output;
    }
    
    /**
     * Print the name and separation power of all attributes used during clustering
     * @return Table listing <i>all</i> of that information
     */
    public String printAttributes() {
        return printAttributes(AttributesUsed.size());
    }
    
    /**
     * Print the name and separation power of a certain number of attributes
     * @param number Number to print
     * @return Table listing all of that information
     */
    public String printAttributes(int number) {
        number = Math.min(number, AttributesUsed.size());
        String output = String.format("%4s\t%32s\t%10s\n","","Attribute Name","Power");
        for (int i=0; i<number; i++)
            output += String.format("#%3d\t%32s\t%10.4e\n", i, AttributesUsed.get(AttributeRanks[i]),
                    AttributePower[i]);
        return output;
    }
  
    /**
     * Print out attributes name, separation power, and mean for each cluster for 
     * all attributes used in clustering
     * @return Table with <i>all</i> of this information
     */
    public String printClusters() {
        return printClusters(AttributesUsed.size());
    }
    
    /**
     * Print out attributes name, separation power, and mean for each cluster for a certain 
     * number of attributes
     * @param numAttributesToPrint Number of attributes to print (storing with most powerful)
     * @return Table with all of this information
     */
    public String printClusters(int numAttributesToPrint) {
        numAttributesToPrint = Math.min(numAttributesToPrint, AttributesUsed.size());
        // Print out header
        String output = String.format("%32s  %10s", "Attribute Name", "Power");
        for (int i=0; i<NClusters; i++)
            output += String.format("  %10s", "Cluster #" + i);
        output += "\n";
        // Print out body
        for (int a=0; a<numAttributesToPrint; a++) {
            output += String.format("%32s  %10.4e",AttributesUsed.get(AttributeRanks[a]),
                    AttributePower[a]);
            for (int c=0; c<NClusters; c++)
                output += String.format("  %10.4e", ClusterMean[c][AttributeRanks[a]]);
            output += "\n";
        }
        return output;
    }
    
    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return about();
        switch (Command.get(0).toLowerCase()) {
            case "dist":
                return printDistribution();
            case "attr":
                if (Command.size() == 1)
                    return printAttributes();
                if (! UtilityOperations.isInteger(Command.get(1)))
                    throw new Exception("Usage: attr [<number>]");
                return printAttributes(Integer.parseInt(Command.get(1)));
            case "clust":
                if (Command.size() == 1)
                    return printClusters();
                if (! UtilityOperations.isInteger(Command.get(1)))
                    throw new Exception("Expected integer for third option");
                return printClusters(Integer.parseInt(Command.get(1)));
            default:
                throw new Exception("ERROR: Print command \"" + Command.get(0).toLowerCase()
                        + "\" not recognized");
        }
    }
    
    
}
