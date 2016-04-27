package magpie.attributes.generators.composition;

import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.filters.CompositionSetDistanceFilter;
import magpie.optimization.algorithms.OptimizationHelper;
import magpie.utility.EqualSumCombinations;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Compute attributes using Atomic Packing Efficiency (APE) of nearby clusters.
 * The atomic packing efficiency, as defined by <a href="http://www.nature.com/doifinder/10.1038/ncomms9123">
 * Laws <i>et al.</i></a> is determined based on the ideal and actual ratio 
 * between the central and shell atoms of an atomic cluster with
 * a certain number atoms. Often, the packing efficiency is described as a 
 * ratio between these two quantities:
 * 
 * <p>[Packing efficiency] = [Ideal radius ratio] / [Actual radius ratio]
 * 
 * <p>The ideal ratio is determined based on the ratio between the size of
 * a central atom and the neighboring atoms such that packing around the central
 * atom is maximized. These optimal ratios for clusters of different numbers
 * of atoms have been tabulated by <a href="http://dx.doi.org/10.2320/matertrans.47.1737">
 * Miracle <i>et al.</i></a>. 
 * 
 * <p>The actual ratio is computed by dividing the radius of the central atom 
 * by the average radius of the central atoms. 
 * 
 * <p>We currently use this framework to create two types of attributes:
 * 
 * <ol>
 * <li>Distance to nearest clusters with a packing efficiency better than
 * a certain threshold. If there are fewer than a request number of efficiently
 * packed clusters in an alloy system, the average is taken to be the average
 * distance to all of the clusters. These attributes are designed to measure
 * the availability of efficiently-packed atomic configurations in the liquid.
 * <li>Mean packing efficiency of system assuming that the composition of the
 * first nearest neighbor shell is equal to the composition of the system. Each
 * atom type is surrounded by the number of atoms that maximizes the packing efficiency.
 * As shown in recent work by <a href="http://www.nature.com/doifinder/10.1038/ncomms9123">
 * Laws <i>et al.</i></a>, bulk metallic glasses are known to form when the clusters
 * around all types of atom have the same composition as the alloy and
 * are efficiently packed. We compute the average APE for each atom in the system,
 * under this assumption, and the average deviation from perfect packing.
 * </ol>
 * 
 * <usage><p><b>Usage</b>: &lt;packing threshold&gt; -neighbors &lt;neighbors to evaluate&gt;
 * <pr><br><i>packing threshold</i>: Threshold at which to define a cluster
 * as "efficiently-packed" (suggestion = 0.01)
 * <pr><br><i>neighbors to evaluate</i>: List of number of nearest neighbors
 * to consider when generating attributes (suggestion = 1 3 5)</usage>
 * 
 * @author Logan Ward
 */
public class APEAttributeGenerator extends BaseAttributeGenerator {
    /** 
     * Threshold at which to define a cluster as efficiently packed.
     * Packing efficiency is defined by |APE - 1|. Default value for this 
     * parameter is 0.01;
     */
    private double PackingThreshold = 0.01;
    /** 
     * Number of nearest clusters to assess. 
     * <p>Dev Note: NavigableSet ensures that the iterators always goes 
     * in the same order. So, you can use this set to iterate w/o needing
     * to first transfer to a list.
     */
    private NavigableSet<Integer> NNearestToEval = new TreeSet<>();
    /** 
     * Name of elemental property to use as atomic radius. By default, uses
     * the radii from doi: 10.1179/095066010X12646898728200.
     */
    private String RadiusProperty = "MiracleRadius";

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        double pEff;
        List<Integer> neighs = new ArrayList<>();
        
        try {
            pEff = Double.parseDouble(Options.get(0).toString());
            if (pEff >= 1) {
                System.err.println("WARNING: Expecting a value between "
                        + " 0 and 100 for packing efficiency.");
            }
            if (! Options.get(1).toString().equalsIgnoreCase("-neighbors")) {
                throw new Exception();
            }
            for (Object opt : Options.subList(2, Options.size())) {
                neighs.add(Integer.valueOf(opt.toString()));
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        setPackingThreshold(pEff);
        setNNearestToEval(neighs);
    }

    @Override
    public String printUsage() {
        return "Usage: <threshold packing efficiency> -neighbors <number of neighbors to consider...>";
    }

    /**
     * Define the threshold at which to define a cluster as efficiently packed.
     * Packing efficiency is defined as |APE - 1|.
     * @param threshold Desired threshold (default = 0.01)
     * @throws Exception 
     */
    public void setPackingThreshold(double threshold) throws Exception {
        if (threshold < 0) {
            throw new Exception("Threshold must be positive.");
        }
        this.PackingThreshold = threshold;
    }

    /**
     * Set the number of nearest clusters to evaluate when computing attributes.
     * @param toEval Collection containing a set of numbers of nearest clusters
     * to evaluate when computing attributes. Default: {1, 3, 5}
     */
    public void setNNearestToEval(Collection<Integer> toEval) {
        this.NNearestToEval = new TreeSet<>(toEval);
    }

    /**
     * Set the name of the elemental property used to define radii. By default,
     * uses the "MiracleRadius" property, which is from an assessment by 
     * <a href="">Miracle <i>et al.</i>., 2010</a>.
     * @param property Name of property used to define radii
     */
    public void setRadiusProperty(String property) {
        this.RadiusProperty = property;
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check that dataset is a composition dataset
        if (! (data instanceof CompositionDataset)) {
            throw new Exception("Dataset must be a CompositionDataset");
        }
        
        // Get the atomic radii
        CompositionDataset dPtr = (CompositionDataset) data;
        double[] radiiLookup = dPtr.getPropertyLookupTable(RadiusProperty);
        
        // Create attribute names
        List<String> newNames = new ArrayList<>();
        for (Integer n : NNearestToEval) {
            newNames.add(String.format("APE_Nearest%s_Below%f",
                    n, PackingThreshold));
        }
        newNames.add("APE_SystemAverage");
        newNames.add("APE_SystemAverageDeviaton");
        data.addAttributes(newNames);
        
        // Find the largest number of clusters to be considered
        int largestN = NNearestToEval.floor(Integer.MAX_VALUE);
        
        // Get the entries, sort so that alloys for the same system are grouped
        //  together (this happens by default for the CompositionEntry)
        List<BaseEntry> entries = new ArrayList<>(data.getEntries());
        Collections.sort(entries);
        
        // Compute attributes for each entry
        double[] attrs = new double[newNames.size()];
        int[] lastElements = new int[0];
        List<CompositionEntry> clusters = new ArrayList<>(1);
        for (BaseEntry ptr : entries) {
            // Cast entry as a CompositionEntry
            CompositionEntry entry = (CompositionEntry) ptr;
            
            // Get list of elements
            int[] curElems = entry.getElements();
            
            // Get radii of those elements
            double[] radii = new double[curElems.length];
            for (int i=0; i<radii.length; i++) {
                radii[i] = radiiLookup[curElems[i]];
            }
            
            // If this list doesn't equal lastElements, recompute list of nearest clusters
            if (! Arrays.equals(curElems, lastElements)) {
                List<List<int[]>> temp = findEfficientlyPackedClusters(radii, PackingThreshold);
                clusters = computeClusterCompositions(curElems, temp);
                lastElements = curElems;
            }
            
            // Find the closest clusters
            List<CompositionEntry> closestClusters = NearbyCompoundAttributeGenerator.
                    getClosestCompositions(entry, clusters, largestN, 2);
            
            // Compute the distance of our cluser to each of those clusters
            double[] distances = new double[closestClusters.size()];
            for (int i=0; i<distances.length; i++) {
                distances[i] = CompositionSetDistanceFilter.computeDistance(entry, 
                        closestClusters.get(i), 2);
            }
            
            // Compute the averages 
            int pos=0;
            if (distances.length == 0) {
                for (Integer n : NNearestToEval) {
                    attrs[pos++] = 1000.0; // Artificially-high number
                }
            } else {
                for (Integer n : NNearestToEval) {
                    // Average the nearest distances
                    attrs[pos++] = StatUtils.mean(distances, 0, Math.min(n, distances.length - 1));
                }
            }
            
            // Compute the packing effiency of clusters around each atom, 
            //  assuming that the composition first nearest-neighbor
            //  shell is equal to the composition of the alloy
            int[] entryElems = entry.getElements();
            double[] entryFracs = entry.getFractions();
            double[] clusterAPEs = new double[entryElems.length];
            for (int t=0; t<entryElems.length; t++) {
                clusterAPEs[t] = determineOptimalAPE(entryElems[t], entry, radiiLookup);
            }
            
            // Compute the composition-weighted average and average deviation from 1
            double ave = 0.0, aveDev = 0.0;
            for (int i=0; i<entryElems.length; i++) {
                ave += entryFracs[i] * clusterAPEs[i];
                aveDev += entryFracs[i] * Math.abs(1.0 - clusterAPEs[i]);
            }
            attrs[pos++] = ave;
            attrs[pos++] = aveDev;
            
            // Add attributes to entry
            entry.addAttributes(attrs);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Get list clusters to evaluate as text
        List<Integer> nToEval = new ArrayList<>(NNearestToEval);
        Collections.sort(nToEval);
        String clusters;
        clusters = nToEval.get(0).toString();
        if (nToEval.size() > 1) {
            for (int i=1; i<nToEval.size() - 1; i++) {
                clusters += ", " + nToEval.get(i).toString();
            }
            clusters += " and " + nToEval.get(nToEval.size() - 1).toString();
        }
        
        // Print description
        output += "(" + (NNearestToEval.size() + 2) + ")";
        output += " Attributes based on the estimated packing efficiency of "
                + "individual atomic clusters using the Atomic Packing Efficiency "
                + "approach of Laws et al. Includes the estimated average packing "
                + "efficiency and deviation from perfect packing of each atom, "
                + "assuming that the nearest neighbor shell composition is equal "
                + "to the alloy composition. Additionally, the distance to the "
                + clusters + " nearest clusters with a packing efficiency "
                + "within 1 +/- " + PackingThreshold + ".";
        
        return output;
    }
    
    /**
     * Compute the optimal APE for a cluster with a certain atom type 
     * in the center and composition in the cell. This algorithm finds
     * the number of atoms in the shell such that the APE of the cluster 
     * is closest to 1. Note: This calculation assumes that sites in the 
     * first nearest-neighbor shell can be partially-occupied.
     * @param centralAtomType Element ID (Z - 1) of central atom
     * @param shellComposition Composition of nearest neighbor shell
     * @param radii Lookup table of elemental radii
     * @return 
     */
    static public double determineOptimalAPE(int centralAtomType, 
            CompositionEntry shellComposition, double[] radii) {
        // Initialize output 
        double output = Double.MAX_VALUE;
        
        // Get radius of center, mean radius of outside
        double centerR = radii[centralAtomType];
        double shellR = shellComposition.getMean(radii);
        
        // Loop through all atom sizes
        for (int z=3; z<24; z++) {
            double ape = computeAPE(z, centerR, shellR);
            if (Math.abs(ape - 1) < Math.abs(output - 1)) {
                output = ape;
            } else {
                break;
            }
        }
        
        return output;
    }
    
    /**
     * Compute the compositions of a list of atomic clusters. The composition
     * includes both atoms in the first nearest neighbor shell and the atom
     * in the center of the cluster.
     * 
     * @param elements Elements from which clusters are composed. 
     * @param clusters Clusters to convert. List of the identity of shell compositions
     * for each type of central atom. Example: clusters[1][2] Is a array defining
     * the number of atoms of each type for clusters with an atom of type 1 in 
     * the center. 
     * @return List of the compositions of these clusters
     * @see #findEfficientlyPackedClusters(double[], double) 
     */
    static public List<CompositionEntry> computeClusterCompositions(
            int[] elements, List<List<int[]>> clusters) {
        // Initialize output
        List<CompositionEntry> output = new ArrayList<>();
        
        // Array to store the number of each atom type as a double
        double[] fractions = new double[elements.length];
        
        // Loop through clusters with each type of atom at the center
        for (int ct=0; ct<clusters.size(); ct++) {
            
            // Loop through each cluster
            for (int[] shell : clusters.get(ct)) {
                
                // Store the number of atoms of each type in the shell
                for (int i=0; i<fractions.length; i++) {
                    fractions[i] = shell[i];
                }
                
                // Acount for the central atom
                fractions[ct]++;
                
                // Create a new composition entry, store it
                try {
                    output.add(new CompositionEntry(elements, fractions));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }
        
        return output;
    }
    
    /**
     * Provided a list of atomic radii, find all clusters with better than a certain
     * packing efficiency threshold. The packing efficiency is determined 
     * by abs(1 - APE)
     * @param radii Radii of elements
     * @param packingThreshold Desired packing limit threshold. A "default" 
     * choice would be 0.05
     * @return List of efficiently packed structures for each atom type as
     * the central atom. Ex: x[0][1] is the 2nd efficiently packed cluster
     * with atom type 0 as the central atom.
     * @see #computeAPE(double[], int, int[]) 
     */
    static public List<List<int[]>> findEfficientlyPackedClusters(double[] radii,
            double packingThreshold) {
        // Initialize output array
        List<List<int[]>> output = new ArrayList<>(radii.length);
        
        // Special case: Only one atom type
        if (radii.length == 1) {
            // Create storage array
            List<int[]> clusters = new ArrayList<>();
            
            // Loop through all cluster sizes
            for (int clusterSize=3; clusterSize<24; clusterSize++) {
                double ape = computeAPE(clusterSize, 1.0, 1.0);
                if (Math.abs(ape - 1) < 0.05) {
                    clusters.add(new int[]{clusterSize});
                }
            }
            
            // Add to output
            output.add(clusters);
            
            return output;
        }
        
        // Determine minimum and maximum cluster sizes
        Pair<Integer, Integer> extremeSizes = getClusterRange(radii, packingThreshold);
        
        // Loop through each atom as the central type
        for (int centralType = 0; centralType < radii.length; centralType++) {
            // Make storage array
            List<int[]> clusters = new ArrayList<>();
            
            // Loop over possible ranges of cluster sizes (determined from
            //   radii)
            for (int clusterSize=extremeSizes.getLeft(); 
                    clusterSize<extremeSizes.getRight(); clusterSize++) {
                
                // Loop through all combinations of atom types in the first shell
                for (int[] shell : new EqualSumCombinations(clusterSize, radii.length)) {
                    
                    // Get the APE of the cluster
                    double ape = computeAPE(radii, centralType, shell);
                    
                    // Add if below the packing threshold
                    if (Math.abs(ape - 1) < packingThreshold) {
                        clusters.add(shell);
                    }
                }
            }
            
            // Add that to output
            output.add(clusters);
        }
        
        return output;
    }
    
    /**
     * Compute the APE of a cluster, provided the identities of the central and 
     * 1st neighbor atoms. 
     * 
     * @param radii Radius of each atom type
     * @param centerType Type of atom in the center
     * @param shellTypes Number of atoms of each type in the outer shell. Must be
     * the same length as radii
     * @return APE of the cluster
     */
    static public double computeAPE(double[] radii, int centerType, int[] shellTypes) {
        // Get the radius of the cetnral atom
        double center_radius = radii[centerType];
        
        // Get the mean radius of the 1st neighbor shell
        double neighbor_radius = 0;
        double n_neighbors = 0;
        for (int i = 0; i < shellTypes.length; i++) {
            n_neighbors += shellTypes[i];
            neighbor_radius += shellTypes[i] * radii[i];
        }
        neighbor_radius /= n_neighbors;
        
        return computeAPE((int) n_neighbors, center_radius, neighbor_radius);
    }
    
    /**
     * Compute the APE provided the number of neighbors and radii.
     * 
     * <p>Here, we follow the formulation given by
     * <a href="http://www.nature.com/ncomms/2015/150915/ncomms9123/full/ncomms9123.html">
     * Laws <i>et al.</i></a>, where:
     * 
     * <br>APE = &lt;ideal radius ratio&gt; / (&lt;radius of central atom&gt; /
     * &lt;effective radius of nearest neighbors&gt;)
     * 
     * <p>The ideal ratio is computed using {@linkplain #getIdealRadiusRatio(int) }
     * @param NNeighbors Number of 1st nearest neighbors in the cluster
     * @param centerRadius Radius of central atom
     * @param neighEffRadius Effective radius of 1st shell. Usually computed
     * as the average radius of all atoms in the shell.
     * @return APE of the cluster.
     */
    static public double computeAPE(int NNeighbors, double centerRadius, 
            double neighEffRadius) {
        double idealRatio = getIdealRadiusRatio(NNeighbors);
        double actualRatio = centerRadius / neighEffRadius;
        return idealRatio / actualRatio;
    }
    
    /**
     * Get the ideal radius ratio for a cluster of a certain size. 
     * 
     * <p>Note: The ideal radius ratio is only known for clusters with between
     * 3 and 24 (inclusive) neighbors. If you request outside of this range, 
     * it will return the value of 3 for anything less than 3, and the value of 
     * 24 for anything larger than 24.
     * 
     * <p>Reference: <a href="http://dx.doi.org/10.2320/matertrans.47.1737">
     * Miracle <i>et al.</i></a>
     * 
     * @param NNeighbors Number of 1st nearest neighbors.
     * @return Ideal radius ratio
     */
    static public double getIdealRadiusRatio(int NNeighbors) {
        if (NNeighbors <= 3) {
            return 0.154701;
        } else if (NNeighbors == 4) {
            return 0.224745; 
        } else if (NNeighbors == 5) {
            return 0.361654;
        } else if (NNeighbors == 6) {
            return 0.414213; 
        } else if (NNeighbors == 7) {
            return 0.518145;
        } else if (NNeighbors == 8) {
            return 0.616517;
        } else if (NNeighbors == 9) {
            return 0.709914;
        } else if (NNeighbors == 10) {
            return 0.798907;
        } else if (NNeighbors == 11) {
            return 0.884003;
        } else if (NNeighbors == 12) {
            return 0.902113;
        } else if (NNeighbors == 13) {
            return 0.976006;
        } else if (NNeighbors == 14) {
            return 1.04733;
        } else if (NNeighbors == 15) {
            return 1.11632;
        } else if (NNeighbors == 16) {
            return 1.18318;
        } else if (NNeighbors == 17) {
            return 1.24810;
        } else if (NNeighbors == 18) {
            return 1.31123;
        } else if (NNeighbors == 19) {
            return 1.37271;
        } else if (NNeighbors == 20) {
            return 1.43267;
        } else if (NNeighbors == 21) {
            return 1.49119;
        } else if (NNeighbors == 22) {
            return 1.54840;
        } else if (NNeighbors == 23) {
            return 1.60436;
        } else {
            return 1.65915;
        }
    }
    
    /**
     * Determine the maximum and minimum possible cluster sizes, provided a list of radii.
     * 
     * <p>The smallest possible cluster has the smallest atom in the center and the largest
     * in the outside. The largest possible has the largest in the inside and smallest 
     * in the outside
     * 
     * @param radii List of radii of elements in system
     * @param packingThreshold APE defining the maximum packing threshold
     * @return Pair. Left: minimum cluster size (defined by number of atoms in shell)
     * . Right: maximum cluster size
     */
    static public Pair<Integer, Integer> getClusterRange(double[] radii, 
            double packingThreshold) {
        // Get minimum and maximum radius
        int[] rank = OptimizationHelper.sortAndGetRanks(radii.clone(), true);
        int biggestRadius = rank[0];
        int smallestRadius = rank[radii.length - 1];
        
        // Compute the smallest possible cluster
        int[] cluster = new int[radii.length];
        int centerType = smallestRadius;
        cluster[biggestRadius] = 3;
        
        while (computeAPE(radii, centerType, cluster) < (1 - packingThreshold)) {
            cluster[biggestRadius]++;
            if (cluster[biggestRadius] > 24) {
                throw new RuntimeException("smallest cluster > 24 atoms: packingThreshold must be too large");
            }
        }
        int smallestCluster = cluster[biggestRadius];
        
        // Compute the largest possible cluster
        cluster[biggestRadius] = 0;
        cluster[smallestRadius] = 24;
        centerType = biggestRadius;
        
        while (computeAPE(radii, centerType, cluster) > (1 + packingThreshold)) {
            cluster[smallestRadius]--;
            if (cluster[smallestRadius] < 3) {
                throw new RuntimeException("largest cluster < 3 atoms: packingThreshold must be too large");
            }
        }
        int biggestCluster = cluster[smallestRadius];
        
        // Return result
        return new ImmutablePair<>(smallestCluster, biggestCluster);
    }
}
