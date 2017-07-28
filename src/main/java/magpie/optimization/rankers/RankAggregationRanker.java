
package magpie.optimization.rankers;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.user.CommandHandler;
import magpie.utility.UtilityOperations;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Rank entries based on multiple objectives using the Rank Aggregation. Uses 
 * methods described in work by <a href="http://www.pubmedcentral.nih.gov/articlerender.fcgi?artid=2669484&tool=pmcentrez&rendertype=abstract>
 * Pihur <i>et al.</i></a>. 
 * 
 * <p><b>NOTE</b> This function can only rank entries. The "objective function"
 * method will return 0.
 * 
 * <usage><p><b>Usage</b>: &lt;k&gt; -obj &lt;maximize|minimize&gt; &lt;property&gt; &lt;ranker name&gt; [&lt;ranker options...&gt;] [-opt &lt;...&gt;]
 * <br><pr><i>property</i>: Name of property to be optimized using this ranker
 * <br><pr><i>maximize|minimize</i>: Whether the goal is to minimize this objective function
 * <br><pr><i>ranker method</i>: Name of an {@link BaseEntryRanker}. Avoid using another multi-objective ranker
 * <br><pr><i>ranker options</i>: Any options for that entry ranker
 * <br>The "-obj" flag can be used multiple times, and the syntax for each additional flag is identical. Also, this function
 * is designed to be minimized.</usage>
 * @author Logan Ward
 */
public class RankAggregationRanker extends MultiObjectiveEntryRanker {
    /** Map of property name to objective function */
    protected SortedMap<String,BaseEntryRanker> ObjectiveFunction = new TreeMap<>();
    /** Index of each property of interest */
    protected int[] PropertyIndex;
    /** Number of top entries to be interested in. When ranking a list, any entries
     * with a rank lower than this number are considered indistinguishable */
    protected int K = 10;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() < 2) {
            throw new Exception(printUsage());
        }
        
        int pos = 1;
        while (pos < Options.size()) {
            String objName;
            String property;
            boolean toMaximize;
            List<Object> objOptions = new LinkedList<>();
            try {
                if (! Options.get(pos++).toString().equalsIgnoreCase("-opt")) {
                    throw new Exception();
                }
                
                objName = Options.get(pos++).toString().toLowerCase();
                if (objName.startsWith("max")) {
                    toMaximize = true;
                } else if (objName.startsWith("min")) {
                    toMaximize = false;
                } else {
                    throw new Exception();
                }
                
                property = Options.get(pos++).toString();
                objName = Options.get(pos++).toString();
                
                while ( pos < Options.size() &&
                        ! Options.get(pos).toString().equalsIgnoreCase("-opt")) {
                    objOptions.add(Options.get(pos++));
                }
            } catch (Exception e) {
                throw new Exception(printUsage());
            }
            
            BaseEntryRanker obj = (BaseEntryRanker) CommandHandler.instantiateClass(
                    "optimization.rankers." + objName, objOptions);
            obj.setMaximizeFunction(toMaximize);
            obj.setUseMeasured(true);
            addObjectiveFunction(property, obj);
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <k> -opt <maximize|minimize> <property> <ranker name> [<ranker options...>] [-opt <...>]";
    }

    /** 
     * Define the maximum number of entries to be interested in. Entries with
     * ranks past this number are not actually sorted.
     * @param K Maximum number of entries.
     */
    public void setK(int K) {
        this.K = K;
    }
    
	@Override
	public void setUseMeasured(boolean useMeasured) {
		for (BaseEntryRanker ranker : ObjectiveFunction.values()) {
			ranker.setUseMeasured(useMeasured);
		}
		super.setUseMeasured(useMeasured); //To change body of generated methods, choose Tools | Templates.
	}

    /**
     * Clear out list of currently-defined objective functions
     */
    public void clearObjectiveFunctions() {
        ObjectiveFunction.clear();
    }
    
    /**
     * Define a new objective function. Order in which you add these does not matter
     * @param property Name of property to be optimized
     * @param function Objective function for that property
     */
    public void addObjectiveFunction(String property, BaseEntryRanker function) {
        BaseEntryRanker newObj = function.clone();
        newObj.setUseMeasured(isUsingMeasured());
        ObjectiveFunction.put(property, function.clone());
    }

    /**
     * Get objective function for a certain property.
     * @param property Name of property
     * @return Objective function (null if not defined)
     */
    public BaseEntryRanker getObjectiveFunction(String property) {
        return ObjectiveFunction.get(property);
    }

    @Override
    public String[] getObjectives() {
        String[] output = new String[ObjectiveFunction.size()];
        int i=0; 
        for (String name : ObjectiveFunction.keySet()) {
            output[i++] = name;
        }
        return output;
    }

    @Override
    public void train(MultiPropertyDataset data) {
        // Initialization stuff
        int originalIndex = data.getTargetPropertyIndex();
        PropertyIndex = new int[ObjectiveFunction.size()];
        
        // Main work
        int pos = 0;
		double[] objValues = new double[data.NEntries()];
        for (Map.Entry<String,BaseEntryRanker> pair : ObjectiveFunction.entrySet()) {
            // Set class to a certain property
            String property = pair.getKey();
            PropertyIndex[pos] = data.getPropertyIndex(property);
            
            // Increment loop counter
            pos++;
        }
        
        // Return to its original state
        data.setTargetProperty(originalIndex, true);
    }

    @Override
    public double objectiveFunction(BaseEntry Entry) {
        return 0;
    }

    @Override
    public int[] rankEntries(Dataset Data, double[] Values) {
        // Get pointer to dataset
        if (! (Data instanceof MultiPropertyDataset)) {
            throw new Error("Data must be a MultiPropertyDataset");
        }
        MultiPropertyDataset ptr = (MultiPropertyDataset) Data;
        
        // Get ranks from all objective functions
        double[][] subLists = new double[ObjectiveFunction.size()][];
        int pos=0;
        int originalIndex = ptr.getTargetPropertyIndex();
        for (Map.Entry<String, BaseEntryRanker> entry : ObjectiveFunction.entrySet()) {
            // Get property of interest and approperiate ranking entry
            String property = entry.getKey();
            BaseEntryRanker ranker = entry.getValue();
            
            // Set target property
            ptr.setTargetProperty(property, true);
            
            // Get ranks of each entry
            int[] orderedList = ranker.rankEntries(Data);
            subLists[pos] = new double[Data.NEntries()];
            for (int i=0; i<orderedList.length; i++) {
                subLists[pos][orderedList[i]] = i;
            }
            pos++;
        }
        
        // Reset the original property
        ptr.setTargetProperty(originalIndex, true);
        
        // Configure aggregator and get aggregated ranks
        RankAggregator.k = K;
        RankAggregator.PopulationSize = K * K * K;
        Values = RankAggregator.getAggregatedRanks(subLists);
        
        // Return results
        return UtilityOperations.sortAndGetRanks(Values, isMaximizing());
    }
}

/**
 * Actually performs the rank aggregation process
 * @author Logan Ward
 */
class RankAggregator {
    /** Population size */
    static public int PopulationSize = 1000;
    /** Number of top entries to evaluate */
    static public int k = 30;
    /** Maximum number of iterations */
    static public int MaxGenerations = 1000;
    /** Mutation probability */
    static public double MutationProbability = 0.1;
    /** Crossover probability */
    static public double CrossoverProbability = 0.99;
    
    /**
     * Get the aggregated ranks 
     * @param subLists Ranks from the sub objective functions
     * @return Aggregated ranks of each entry as double
     */
    static public double[] getAggregatedRanks(double[][] subLists) {
        // Get list sorted by average ranks
        //List<Integer> initialGuess = getStartingGuess(subLists);
        List<Integer> initialGuess = new ArrayList<>(subLists[0].length);
        for (int i=0; i<subLists[0].length; i++) {
            initialGuess.add(i);
        }
        
        // Generate initial population
        List<List<Integer>> population = new ArrayList<>(PopulationSize);
        population.add(initialGuess);
        for (int i=1; i<PopulationSize; i++) {
            List<Integer> temp = new ArrayList<>(initialGuess);
            mutateList(temp, PopulationSize);
            population.add(temp);
        }
        
        // Propogate Genetic Algorithm
        List<Integer> bestAnswer = runGA(population, subLists);
        
        // Convert best answer to an array where [i] = rank of entry #i
        double[] output = new double[bestAnswer.size()];
        int pos=0;
        for (Integer entry : bestAnswer) {
            output[entry] = pos++;
        }
        return output;
    }
    
    /**
     * Run genetic algorithm to find optimal list
     * @param population Population of lists. Initial provided as input. 
     * Final population provide as output
     * @return Best guess
     */
    static protected List<Integer> runGA(List<List<Integer>> population,
            double[][] subLists) {
        int sinceLastBest = 0;
        double curBest = Double.MAX_VALUE;
        
        // Get performance of initial population
        double[] perf = new double[population.size()];
        for (int i=0; i<population.size(); i++) {
            perf[i] = getFootruleDistance(population.get(i), subLists);
        }
        
        // Propogate algorithms
        for (int generation = 0; generation < MaxGenerations; generation++) {
            
            // See if a new optimum has been found
            if (StatUtils.min(perf) < curBest) {
                curBest = StatUtils.min(perf);
                sinceLastBest = 0;
            } else {
                sinceLastBest++;
                if (sinceLastBest > 15) {
                    break;
                }
            }
            
            // Create new population
            List<List<Integer>> newPopulation = 
                    generateNewPopulation(population, perf);
            
            // Replace old population
            population.clear();
            population.addAll(newPopulation);
            
            // Evaluate new entries
            for (int i=0; i<population.size(); i++) {
                perf[i] = getFootruleDistance(population.get(i), subLists);
            }
        }
        
        // Return best list
        List<Integer> output = population.get(0);
        double bestPerf = perf[0];
        for (int i=1; i<population.size(); i++) {
            if (perf[i] < bestPerf) {
                output = population.get(i);
                bestPerf = perf[i];
            }
        }
        
        return output;
    }

    /**
     * Generate a new population. 
     * 
     * @param population Old population
     * @param perf Performance of old population
     * @return New population
     */
    protected static List<List<Integer>> generateNewPopulation(
            List<List<Integer>> population, double[] perf) {
        
        // Generate weights for each member of old population
        double[] weight = new double[population.size()];
        weight[0] = Math.pow(1.0 / perf[0], 5);
        for (int i=1; i<population.size(); i++) {
            weight[i] = Math.pow(1.0 / perf[i], 5) + weight[i-1];
        }
        double totalWeight = weight[weight.length - 1];
        
        Random rnd = new Random();
        List<List<Integer>> newPopulation = new ArrayList<>(population.size());
        // Create new population
        for (int i=0; i<population.size(); i++) {
            int pos = Arrays.binarySearch(weight, rnd.nextDouble() * totalWeight);
            if (pos < 0) pos = -1 * (pos + 1);
            newPopulation.add(population.get(pos));
        }
        
        // Perform crossover
        for (int i=0; i<newPopulation.size(); i++) {
            if (Math.random() < CrossoverProbability) {
                int parentB = Arrays.binarySearch(weight, rnd.nextDouble() 
                        * totalWeight);
                if (parentB < 0) parentB = -1 * (parentB + 1);
                newPopulation.set(i, performCrossover(newPopulation.get(i),
                        population.get(parentB)));
            }
        }
        
        // Perform mutation
        int maxSwaps = Math.max(k / 10, 5); 
        for (List<Integer> list : newPopulation) {
            if (Math.random() < MutationProbability) {
                mutateList(list, rnd.nextInt(maxSwaps));
            }
        }
        return newPopulation;
    }
    
    /**
     * Get distance between a aggregated list and all sublists
     * @param list Aggregated lists
     * @param subLists Rank of each entry in each sub list
     * @return 
     */
    static public double getFootruleDistance(List<Integer> list, double[][] subLists) {
        double totalDist = 0;
        int pos = 0;
        for (Integer entry : list) {
            for (double[] subList : subLists) {
                double otherPos = subList[entry];
                if (otherPos > k) otherPos = k;
                totalDist += Math.abs((pos > k ? k : pos) - otherPos);
            }
            pos++;
        }
        return totalDist;
    }
    
    /**
     * Get a starting guess. Creates an ordered list where elements are sorted by 
     * their average rank.
     * @param subLists Ranks of each entry (ordered by entry number) 
     * @return Initial guess as linked list 
     */
    static protected List<Integer> getStartingGuess(double[][] subLists) {
        // Get average ranks
        double[] meanRank = new double[subLists[0].length];
        double[] temp = new double[subLists.length];
        for (int i=0; i<meanRank.length; i++) {
            for (int j=0; j<temp.length; j++) {
                temp[j] = subLists[j][i];
            }
            meanRank[i] = StatUtils.mean(temp);
        }
        
        // Rank by average rank from sublists
        int[] startingRanks = UtilityOperations.sortAndGetRanks(meanRank, false);
        
        // Generate starting list
        List<Integer> output = new ArrayList<>(startingRanks.length);
        for (int r : startingRanks) {
            output.add(r);
        }
        return output;
    }
    
    /**
     * Mutate a list by randomly swapping positions in the front of th list.
     * @param list List to be mutated
     * @param nSwaps Number of swapping operations to perform
     */
    static protected void mutateList(List<Integer> list, int nSwaps) {
        int shortEnd = Math.min(k * 2, list.size());
        for (int i=0; i<nSwaps; i++) {
            int a,b;
            if (Math.random() < 0.5) {
                a = (int) Math.floor(Math.random() * (double) shortEnd);
                b = (int) Math.floor(Math.random() * (double) shortEnd);
            } else {
                a = (int) Math.floor(Math.random() * list.size());
                b = (int) Math.floor(Math.random() * list.size());
            }
            Collections.swap(list, a, b);
        }
    }
    
    
    /**
     * Generate a new entry by crossover. 
     * @param parentA Parent A
     * @param parentB Parent B
     * @return New entry
     */
    static protected List<Integer> performCrossover(List<Integer> parentA,
            List<Integer> parentB) {
        LinkedList<Integer> A = new LinkedList<>(parentA),
                B = new LinkedList<>(parentB);
        List<Integer> output = new ArrayList<>(A.size());
        while (! (A.isEmpty() || B.isEmpty())) {
            int toAdd;
            if (Math.random() < 0.5) {
                toAdd = A.poll(); B.removeFirstOccurrence(toAdd);
            } else {
                toAdd = B.poll(); A.removeFirstOccurrence(toAdd);
            }
            output.add(toAdd);
        }
        return output;
    }
}
