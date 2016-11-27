package magpie.optimization.algorithms.genetic;

import java.util.List;
import magpie.optimization.algorithms.genetic.operators.BaseCrossoverFunction;
import magpie.optimization.algorithms.genetic.operators.BaseMutationFunction;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.optimization.BaseOptimizer;
import magpie.user.CommandHandler;

/**
 * This class implements a simple genetic algorithm. While simple, this
 * algorithm has many possible options. One can vary the mutation rate, ranking
 * weight depending on fitness, number of previous generations used in breeding pool,
 * and number of elite candidates held in the breeding pool as well.
 *
 * <usage><p><b>Usage</b>: &lt;Crossover Function> &lt;Mutation Function> [-weight &lt;weight>] [-mutprob &lt;mutprob>] [-prev &lt;prev>] [-elite &lt;elite>]
 * <br><pr><i>Mutation Function</i>: Function used to perform mutation operations.
 * <br><pr><i>Crossover Function</i>: Function used to perform cross-over.
 * <br><pr><i>weight</i>: How much weight to apply to better-ranking entries (default: 10)
 * <br><pr><i>mutprob</i>: Probability that a candidate will be mutated after crossover (default: 0.1)
 * <br><pr><i>prev</i>: How many previous generations to include in breeding pool (default: 1)
 * <br><pr><i>elite</i>: How many of the best-performing entries to include in breeding pool (default: 0)</usage>
 *
 *
 * @author Logan Ward
 * @version 0.1
 */
public class GeneticAlgorithm extends BaseOptimizer {

    /**
     * The highest-ranked entry in a breeding pool is this times as likely to be
     * selected for crossover
     */
    private double RankingWeight = 10.0;
    /**
     * Probably that an entry will be mutated after cross-over
     */
    private double MutationProbability = 0.1;
    /**
     * Number of breeding pool that are guaranteed to be the best so far
     */
    private int NumberElite = 0;
    /**
     * Function that performs crossover given two BaseEntry classes
     */
    private BaseCrossoverFunction CrossoverFunction;
    /**
     * Function that will mutate an entry
     */
    private BaseMutationFunction MutationFunction;
    /**
     * Number of previous generations to use
     */
    private int PreviousIterations = 1;
    /**
     * Maximum number of attempts per entry when creating a generation. There is
     * no guarantee that the algorithm will find a full number candidates in the
     * search space.
     */
    final private int AttemptsPerEntry = 10000;

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        if (Options.length < 2) {
            throw new Exception(printUsage());
        }
        // Load in Options
        for (int i = 0; i < 2; i++) {
            String Name = Options[i];
            Object operation = CommandHandler.instantiateClass(
                    "optimization.algorithms.genetic.operators." + Name, null);
            if (operation instanceof BaseCrossoverFunction) {
                setCrossoverFunction((BaseCrossoverFunction) operation);
            } else if (operation instanceof BaseMutationFunction) {
                setMutationFunction((BaseMutationFunction) operation);
            } else {
                throw new Exception(printUsage()); // Not sure how this would happen...
            }
        }
        // Read in other options
        int pos = 2;
        try {
            while (pos < Options.length) {
                switch (Options[pos].toLowerCase()) {
                    case "-weight":
                        setRankingWeight(Double.valueOf(Options[++pos]));
                        break;
                    case "-elite":
                        setNumberElite(Integer.valueOf(Options[++pos]));
                        break;
                    case "-mutprob":
                        setMutationProbability(Double.valueOf(Options[++pos]));
                        break;
                    case "-prev":
                        setPreviousIterations(Integer.valueOf(Options[++pos]));
                        break;
                    default:
                        throw new Exception();
                }
                pos++;
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <Crossover Function> <Mutation Function> [-weight <weight>]"
                + " [-mutprob <mutprob>] [-prev <prev>] [-elite <elite>]";
    }

    /**
     * Define the cross-over function to be used with this algorithm.
     *
     * @param CrossoverFunction Instantiated cross-over function
     */
    public void setCrossoverFunction(BaseCrossoverFunction CrossoverFunction) {
        this.CrossoverFunction = CrossoverFunction;
    }

    /**
     * Define the mutation function to use with this algorithm
     *
     * @param MutationFunction Instantiated cross-over function
     */
    public void setMutationFunction(BaseMutationFunction MutationFunction) {
        this.MutationFunction = MutationFunction;
        // Set up the mutation function
        if (SearchSpace != null) {
            this.MutationFunction.configureFunction(this.SearchSpace);
        }
    }

    @Override
    public void setSearchSpace(Dataset SearchSpace) {
        super.setSearchSpace(SearchSpace);
        // Ensure that the mutation function is set up
        if (MutationFunction != null) {
            MutationFunction.configureFunction(this.SearchSpace);
        }
    }

    /**
     * Probability that a candidate will be mutated after cross-over
     *
     * @param MutationProbability Desired probability
     * @throws Exception Probability out of range
     */
    public void setMutationProbability(double MutationProbability) throws Exception {
        if (MutationProbability < 0 || MutationProbability > 1) {
            throw new Exception("Probability out of range");
        }
        this.MutationProbability = MutationProbability;
    }

    /**
     * Define how many elite entries to keep in breeding pool
     *
     * @param NumberElite Desired number of elite entries
     */
    public void setNumberElite(int NumberElite) {
        this.NumberElite = NumberElite;
    }

    /**
     * Set the number of previous iterations to include in breeding pool
     *
     * @param PreviousIterations Number of iterations (>= 0)
     */
    public void setPreviousIterations(int PreviousIterations) {
        this.PreviousIterations = PreviousIterations;
    }

    /**
     * Define weighting parameter used when selecting better-performing entries.
     * The best-performing entry is this times as likely to be selected over the
     * least.
     *
     * @param RankingWeight Desired weight
     */
    public void setRankingWeight(double RankingWeight) {
        this.RankingWeight = RankingWeight;
    }

    @Override
    protected Dataset getNewCandidates() {
        Dataset output = InitialData.emptyClone(), Pool;
        Dataset Full = getFullDataset(CurrentIteration);

        if (CurrentIteration == 0) {
            // If we are at the first iteration, use all of the initial data
            Pool = InitialData.clone();
        } else {
            // If not, get the training set first
            Pool = InitialData.emptyClone();
            for (int i = Math.max(0, CurrentIteration - PreviousIterations);
                    i < CurrentIteration; i++) {
                if (i == 0) {
                    Pool.combine(InitialData);
                } else {
                    Pool.combine(getFullDataset(CurrentIteration));
                }
            }

            // Now, add in the elite
            if (NumberElite > 0) {
                int[] elite = ObjectiveFunction.rankEntries(Full);
                for (int i = 0; i < Math.min(NumberElite, elite.length); i++) {
                    Pool.addEntry(Full.getEntry(elite[i]));
                }
                Pool.removeDuplicates();
            }

        }

        // Determine rankings and weights for each entry
        int[] rank = ObjectiveFunction.rankEntries(Pool);
        double[] weight = new double[Pool.NEntries()];
        double temp = Math.log(RankingWeight) / ((double) Pool.NEntries() - 1);
        weight[0] = 1.0;
        for (int i = 1; i < weight.length; i++) {
            weight[i] = Math.exp(-1.0 * temp * i) + weight[i - 1];
        }
        double total_weight = weight[weight.length - 1];

        // Generate the speciifed numbers of new entries
        int id1, id2;
        BaseEntry Entry1, Entry2, Entry;
        long attempts_left = (long) EntriesPerGeneration * (long) AttemptsPerEntry;
        Set FullSet = new TreeSet(Full.getEntries());
        while (output.NEntries() < EntriesPerGeneration) {
            attempts_left--;
            if (attempts_left < 0) {
                System.err.println("WARNING: Ran out of attempts when generating entries");
                break;
            }

            // Select two entries for crossover
            temp = Math.random() * total_weight;
            id1 = 0;
            while (weight[id1] < temp) {
                id1++;
            }
            id2 = id1;
            while (id2 == id1) {
                temp = Math.random() * total_weight;
                id2 = 0;
                while (weight[id2] < temp) {
                    id2++;
                }
            }

            // Generate a new entry through crossover
            Entry1 = Pool.getEntry(rank[id1]);
            Entry2 = Pool.getEntry(rank[id2]);
            Entry = CrossoverFunction.crossover(Entry1, Entry2);

            // If required, mutate it
            if (Math.random() < MutationProbability) {
                MutationFunction.mutate(Entry);
            }

            // Check it we can add this one to the output set
            if (output.containsEntry(Entry)) {
                continue; // If it is in the current set
            }
            if (FullSet.contains(Entry)) {
                continue; // If it has ever been tested
            }
            if (SearchSpace != null) {
                if (! SearchSpace.contains(Entry)) {
                    continue;
                }
            }
            output.addEntry(Entry);
        }

        return output;
    }
}
