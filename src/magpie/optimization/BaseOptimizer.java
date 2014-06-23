/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.utilities.filters.BaseDatasetFilter;
import magpie.optimization.rankers.EntryRanker;
import magpie.optimization.analytics.OptimizationStatistics;
import magpie.optimization.oracles.BaseOracle;
import magpie.optimization.rankers.MultiObjectiveEntryRanker;
import magpie.user.CommandHandler;
import static magpie.user.CommandHandler.instantiateClass;
import static magpie.user.CommandHandler.printImplmentingClasses;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;
import magpie.utility.interfaces.Printable;

/**
 * Basic interface for an optimization algorithm. It also contains
 * implementations of general operations needed for an optimization algorithm.
 *
 * <p>
 * Abstractly, an optimization algorithms is required to create a new generation
 * of candidate entries using some strategy. For now, a list of <i>all</i>
 * possible candidates must be provided to define the search space for this
 * algorithms. Regardless of the method used to generate a new generation, only
 * candidates from this list will be included.
 *
 * <p>
 * After creating those new candidates, it is the task of an "oracle" defined by
 * a {@linkplain BaseOracle} class to evaluate their fitness. This process is
 * allowed continue until it has reached a user-defined number of iterations or
 * the Oracle is unable to evaluate all entries.<p>
 *
 * When creating a new implementation of this class, you must:
 * <ul>
 * <li>Implement {@linkplain  #getNewCandidates()}</li>
 * <li>Ensure that {@linkplain  #checkComponents()} verifies that any new
 * variables have been set</li>
 * <li>Add any new commands to {@linkplain #runCommand(java.util.List)} and
 * {@linkplain #setComponent(java.util.List)}
 * </ul>
 *
 * <p><b><u>Implemented Commands:</u></b>
 *
 * <command><p><b>run</b> - Start optimizer
 * <br>Will run until max iterations is reached, or oracle is unable to process
 * all new candidates.</command>
 *
 * <command><p><b>set initial $&lt;dataset></b> - Define initial population
 * <br><pr><i>dataset</i>: Dataset containing entries to use as initial
 * population</command>
 *
 * <command><p><b>set search $&lt;dataset></b> - Define search space
 * <br><pr><i>dataset</i>: Dataset containing any and all entries optimization algorithm is allowed to evaluate</command>
 *
 * <command><p><b>set objective &lt;min|max> &lt;method> [&lt;options...>]</b> - Set the objective function
 * <br><pr><i>min|max</i>: Whether to minimize or maximize the objective function.
 * <br><pr><i>method</i>: Desired objective function. Name of an {@linkplain EntryRanker} class. ("?" for options)
 * <br><pr><i>options...</i>: Options for objective function</command>
 *
 * <command><p><b>set oracle &lt;method> [&lt;options...>]</b> - Define method used to calculate properties of selected candidates
 * <br><pr><i>method</i>: How to calculate properties of an entry. Name of a {@linkplain BaseOracle} ("?" for options)</command>
 *
 * <command><p><b>set gensize &lt;number</b> - Set number of new candidates per iteration
 * <br><pr><i>number</i>: Desired number of new entries per generation</command>
 *
 * <command><p><b>set maxiter &lt;number></b> - Set the number of iterations algorithm will perform
 * <br><pr><i>number</i>: Desired maximum number of iterations</command>
 * 
 * <command><p><b>stats ntop &lt;number></b> - Control number of top entries to detect when evaluating algorithm performance
 * <br><pr><i>number</i>: Number of globally best-performing entries to check for after each generation</command>
 * 
 * <command><p><b>stats success &lt;include|exclude> &lt;filter method&gt; [&lt;filter options...&gt;]</b> 
 *	- Define a filter used to define when an entry is a "success"
 * <br><pr><i>include|exclude</i>: Whether to include entries that pass the filter as a success
 * <br><pr><i>filter method</i>: Name of a dataset filter ("?" for options)
 * <br><pr><i>filter options...</i>: Any options for that filter</command>
 * 
 * <p><b><u>Available Print Command:</u></b>
 * 
 * <print><p><b>stats</b> - Print statistics about optimization run</print>
 *
 * @author Logan Ward
 * @version 1.0
 */
abstract public class BaseOptimizer implements java.io.Serializable,
        java.lang.Cloneable, Options, Printable, Commandable {

    /**
     * Entries that are acceptable to search
     */
    protected Set<BaseEntry> SearchSpace = null;
    /**
     * How many iterations have been completed
     */
    protected int CurrentIteration = -1;
    /**
     * Iteration at which optimization stops
     */
    protected int MaxIteration = 0;
    /**
     * Initial population used for algorithm
     */
    protected Dataset InitialData = null;
    /**
     * Candidates produced at each iteration
     */
    private ArrayList<Dataset> Candidates = null;
    /**
     * Entry ranker used for objective function
     */
    protected EntryRanker ObjectiveFunction = null;
    /**
     * Oracle used as a method for evaluating new iterations
     */
    protected BaseOracle Oracle = null;
    /**
     * Whether the algorithm has started
     */
    protected boolean HasStarted = false;
    /**
     * Whether status messages should be printed
     */
    public boolean PrintStatus = false;
    /**
     * How many entries to create per generation
     */
    public int EntriesPerGeneration = 100;
    /**
     * Statistics about the run
     */
    public OptimizationStatistics Statistics = new OptimizationStatistics();

    /**
     * Construct an empty BaseOptimizer
     */
    public BaseOptimizer() {
        Candidates = new ArrayList<>();
    }

    /**
     * Set the maximum number of iterations the algorithm is allowed.
     *
     * @param number Desired maximum
     */
    public void setMaxIterations(int number) {
        if (number < CurrentIteration) {
            throw new Error("Cannot be lower than the current iteration.");
        }
        MaxIteration = number;
        Candidates.ensureCapacity(MaxIteration);
    }

    /**
     * @return Current iteration of the optimizer
     */
    public int currentIteration() {
        return CurrentIteration;
    }

    /**
     * @return Maximum number of iterations allowed by user
     */
    public int maximumIterations() {
        return MaxIteration;
    }

    /**
     * Retrieve entries that define the search space
     *
     * @return Pointer to a set of {@link magpie.data.BaseEntry
     */
    public Set getSearchSpace() {
        return SearchSpace;
    }

    /**
     * Define the search space over which the algorithm is allowed to sample. Dataset does not need to have measured class variables.
     *
     * @param SearchSpace Dataset defining entries
     */
    public void setSearchSpace(Dataset SearchSpace) {
        errorIfStarted();
        this.SearchSpace = new TreeSet<>(SearchSpace.getEntries());
    }

    /**
     * Initial population defined
     *
     * @return
     */
    public Dataset getInitialData() {
        return InitialData;
    }

    /**
     * Define the initial population
     *
     * @param InitialData Dataset containing initial entries
     */
    public void setInitialData(Dataset InitialData) {
        errorIfStarted();
        CurrentIteration = 0;
        this.InitialData = InitialData;
    }

    /**
     * Define the objective function
     *
     * @param ObjectiveFunction Desired objective function
     */
    public void setObjectiveFunction(EntryRanker ObjectiveFunction) {
        errorIfStarted();
        this.ObjectiveFunction = ObjectiveFunction;
    }

    /**
     * @return Objective function used by this optimizer
     */
    public EntryRanker getObjectiveFunction() {
        return ObjectiveFunction;
    }
	
	/**
	 * Get an empty dataset based on initial population.
	 */
	public Dataset getEmptyDataset() {
		return InitialData.emptyClone();
	}

    /**
     * Set the number of new entries created per iteration
     *
     * @param EntriesPerGeneration Number of entries desired
     */
    public void setEntriesPerGeneration(int EntriesPerGeneration) {
        this.EntriesPerGeneration = EntriesPerGeneration;
    }

    /**
     * Define {@linkplain BaseOracle} used to evaluate properties/class variable
     *
     * @param oracle Desired Oracle
     */
    public void setOracle(BaseOracle oracle) {
        errorIfStarted();
        this.Oracle = oracle;
    }

    /**
     * @return Whether the algorithm has started or not
     */
    public boolean hasStarted() {
        return HasStarted;
    }
    
    /**
     * Throw an error if the algorithm has started.
     */
    protected void errorIfStarted() {
        if (HasStarted) {
            throw new Error("Cannot change settings, algorithm has started.");
        }
    }

    /**
     * Throw an error if all components are not ready
     *
     * @throws Exception Detailing all that is not ready
     */
    protected void checkIfReady() throws Exception {
        checkComponents();
    }

    /**
     * Check if all of the necessary components are loaded
     *
     * @throws Exception Tells the first unset component that was encountered
     */
    protected void checkComponents() throws Exception {
        if (InitialData == null) {
            throw new Exception("Initial data not set.");
        }
        if (Oracle == null) {
            throw new Exception("Oracle not set.");
        }
        if (SearchSpace == null) {
            throw new Exception("Search space not set.");
        }
        if (ObjectiveFunction == null) {
            throw new Exception("Ranker not set.");
        }
    }

    /**
     * Get a Dataset representing all entries generated at a certain generation. Note that generation 0 is the initial population
     *
     * @param number Iteration number
     * @return All entries at that iteration
     */
    public Dataset getGeneration(int number) {
        if (number == 0) {
            return InitialData;
        } else {
            return Candidates.get(number - 1);
        }
    }

    /**
     * Returns all entries evaluated up a certain iteration. You may want to use this when generating a training set, for instance.
     *
     * @param iteration Iteration to get the full set for
     * @return A dataset containing all entries from initial data up to <code>iteration - 1</code>
     */
    public Dataset getFullDataset(int iteration) {
        try {
            if (iteration > CurrentIteration) {
                throw new Exception("Iteration cannot be greater than the current iteration.");
            }
            Dataset output = InitialData.clone();
            for (int i = 0; i < iteration; i++) {
                output.combine(Candidates.get(i));
            }
            return output;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Return all entries evaluated up a certain iteration. You may want to use this when generating a training set, for instance.
     *
     * @param iteration Iteration to get the full set for
     * @return A set containing all entries from initial data up to <code>iteration - 1</code>
     */
    public Set getFullSet(int iteration) {
        Dataset Data = getFullDataset(iteration);
        return new TreeSet(Data.getEntries());
    }

    /**
     * Run the optimization algorithm until it either the Oracle is unable to evaluate all new candidates, or the algorithm completes
     *
     * @throws java.lang.Exception Upon any failure during optimization
     */
    public void run() throws Exception {
        long start_time;
        // Make sure everything is ready
        checkIfReady();

        while (CurrentIteration < MaxIteration) {
            System.out.println("\tWorking on iteration " + (CurrentIteration + 1)
                    + " out of " + MaxIteration);

            // Check to see whether we need to add more candidates
            if (Candidates.size() <= CurrentIteration) {
                start_time = System.currentTimeMillis();
                if (PrintStatus) {
                    System.out.print("\nGenerating list of new candidates...");
                }
                // If needed, train the optimization algorithm
                if (ObjectiveFunction instanceof MultiObjectiveEntryRanker) {
                    MultiObjectiveEntryRanker Ptr = (MultiObjectiveEntryRanker) ObjectiveFunction;
                    Ptr.train((MultiPropertyDataset) getFullDataset(CurrentIteration));
                }
                
                Dataset new_entries = getNewCandidates();
                if (Candidates.size() != currentIteration()) {
                    throw new Exception("Candidates array incorrect size. Ensure getNewCandidates did not edit it.");
                }
                Candidates.add(new_entries);
                Oracle.importDataset(new_entries);
                if (PrintStatus) {
                    System.out.println("Done.");
                    UtilityOperations.printRunTime(start_time);
                }
            }

            // Evaluate all the new candidates using the oracle
            start_time = System.currentTimeMillis();
            if (PrintStatus) {
                System.out.print("\nEvaluating new candidates...");
            }
            Oracle.evaluateEntries();
            if (PrintStatus) {
                System.out.println("Done.");
                UtilityOperations.printRunTime(start_time);
            }

            // If all entries have been calculated, add them to Candidates and continue
            if (Oracle.isComplete()) {
                if (PrintStatus) {
                    System.out.println("Oracle has finished calculating.");
                }
                Dataset new_entries = Oracle.extractDataset();
                Candidates.set(CurrentIteration, new_entries);
                CurrentIteration++;
            } else {
                // If not, return
                if (PrintStatus) {
                    System.out.println("Oracle did not finish calculating. Check back later.");
                }
                return;
            }
        }
    }

    /**
     * Evaluate the results of the run using the internal statistics object
     */
    public void evaluate() {
        Statistics.evaluate(this);
    }

    @Override
    public BaseOptimizer clone() {
        BaseOptimizer x;
        try {
            x = (BaseOptimizer) super.clone();
        } catch (CloneNotSupportedException c) {
            throw new Error(c);
        }
        x.Candidates = new ArrayList<>();
        for (Dataset Candidate : Candidates) {
            x.Candidates.add(Candidate);
        }
        x.InitialData = InitialData.clone();
        x.Oracle = Oracle.clone();
        x.SearchSpace = new TreeSet<>();
        x.SearchSpace.addAll(SearchSpace);
        x.Statistics = Statistics.clone();
        return x;
    }

    /**
     * Save to file using serialization
     *
     * @param filename Desired filename
     */
    public void saveState(String filename) {
        UtilityOperations.saveState(this, filename);
    }

    /**
     * Load a new BaseOptimizer from a serialized file
     *
     * @param filename File to be loaded
     * @return New instance loaded from file
     */
    public static BaseOptimizer loadState(String filename) {
        return (BaseOptimizer) UtilityOperations.loadState(filename);
    }

    /**
     * Based on the current optimization state, return a list of new candidate entries
     *
     * @return Dataset of candidate entries
     */
    abstract protected Dataset getNewCandidates();

    @Override
    public String about() {
        if (HasStarted) {
            if (currentIteration() == MaxIteration) {
                return "Status: Finished";
            } else {
                return "Status: Iteration " + currentIteration() + " out of " + MaxIteration;
            }
        } else {
            return "Status: Waiting";
        }
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) {
            return about();
        }

        switch (Command.get(0).toLowerCase()) {
            case "stats":
                return Statistics.printResults();
            default:
                throw new Exception("Error: Print command " + Command.get(0) + " not recognized");
        }
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println(about());
            return null;
        }
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "run":
            case "start":
                run();
                evaluate();
                break;
            case "set":
                setComponent(Command.subList(1, Command.size())); break;
            case "stats":
                runStatisticsCommand(Command.subList(1, Command.size())); break;
            default:
                throw new Exception("ERROR: Optimizer command not recognized:" + Action);
        }
        return null;
    }

    /**
     * Handle commands related to controlling what statistics are generated
     *
     * @param Command Command to be run
     * @throws Exception
     */
    public void runStatisticsCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("Statistics command must be non-empty");
        }
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "ntop": {
                // Usage: ntop <number>
                Integer value;
                try {
                    value = Integer.parseInt(Command.get(1).toString());
                } catch (Exception e) {
                    throw new Exception("Usage: stats ntop <#>");
                }
                Statistics.setNumberTopEntries(value);
                System.out.println("\tSet number of top entries to " + value);
                evaluate();
            }
            break;
            case "success": {
                boolean ToExclude;
				String FilterMethod;
				List<Object> FilterOptions;
                try {
                    if (Command.get(1).toString().equalsIgnoreCase("include")) {
						ToExclude = false;
					} else if (Command.get(1).toString().equalsIgnoreCase("exclude")) {
						ToExclude = true;
					} else {
						throw new Exception();
					}
					FilterMethod = Command.get(2).toString();
					FilterOptions = Command.subList(3, Command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: stats success <include|exclude> <filter method> <filter options...>");
                }
				BaseDatasetFilter filter = (BaseDatasetFilter) instantiateClass(
						"data.utilities.filters." + FilterMethod, FilterOptions);
				filter.setExclude(ToExclude);
				Statistics.setSuccessFilter(filter);
                System.out.println("\tDefined a " + FilterMethod + " as the success filter");
                evaluate();
            }
            break;
            default:
                throw new Exception("Statistics have no such option: " + Action);
        }
    }

    /**
     * Set a specific component or setting of an optimizer.
     *
     * @param Command Setting command to be processed
     * @throws Exception
     */
    protected void setComponent(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("No setting/component specified");
        }
        String Component = Command.get(0).toString().toLowerCase();
        switch (Component) {
            case "objective":
            case "obj": {
                // Usage: <optimizer> set objective <max|min> <method> [<method options..>]
                String Method;
                List<Object> Options;
                boolean toMaximize;
                try {
                    // Read whether to max / minimize
                    if (Command.get(1).toString().startsWith("max")) {
                        toMaximize = true;
                    } else if (Command.get(1).toString().startsWith("min")) {
                        toMaximize = false;
                    } else {
                        throw new Exception();
                    }
                    // Read Method name
                    Method = Command.get(2).toString();
                    if (Method.equals("?")) {
                        System.out.println(printImplmentingClasses(EntryRanker.class, false));
                        return;
                    }
                    // Get method options
                    Options = Command.subList(3, Command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: set objective <max|min> <method> [<method options..>]");
                }
                // Make the ranker, attach it
                EntryRanker objFun = (EntryRanker) instantiateClass("optimization.rankers." + Method, Options);
                objFun.UseMeasured = true;
                objFun.MaximizeFunction = toMaximize;
                setObjectiveFunction(objFun);
                System.out.println("\tDefined objective function to be a " + Method);
            }
            break;
            case "gensize": {
                int value;
                try {
                    value = Integer.parseInt(Command.get(1).toString());
                } catch (Exception e) {
                    throw new Exception("Usage: set gensize <#>");
                }
                setEntriesPerGeneration(value);
                System.out.println("\tSet number of entries per generation to " + value);
            }
            break;
            case "initial": {
                // Usage: set intial $<dataset>
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: set initial $<dataset>");
                }
                setInitialData(Data);
                System.out.println("\tDefined an intial population of " + Data.NEntries() + " entries");
            }
            break;
            case "search": {
                // Usage: set search $<dataset>
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: set search $<dataset>");
                }
                setSearchSpace(Data);
                System.out.println("\tDefined a search space of " + Data.NEntries() + " entries");
            }
            break;
            case "oracle": {
                // Usage: set oracle <method> [<options...>]
                String Method;
                List<Object> Options;
                try {
                    Method = Command.get(1).toString();
                    if (Method.contains("?")) {
                        System.out.println("Available Oracles:");
                        System.out.println(CommandHandler.printImplmentingClasses(BaseOracle.class, false));
                    }
                    Options = Command.subList(2, Command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: set oracle <method> [<options...>]");
                }
                BaseOracle NewOracle = (BaseOracle) CommandHandler.instantiateClass("optimization.oracles." + Method, Options);
                setOracle(NewOracle);
                System.out.println("\tSet algorithm to use a " + Method);
            }
            break;
            case "maxiter": {
                int value;
                try {
                    value = Integer.parseInt(Command.get(1).toString());
                } catch (Exception e) {
                    throw new Exception("Usage: set maxiter <#>");
                }
                setMaxIterations(value);
                System.out.println("\tSet maximum number of iterations to " + value);
            }
            break;
            default:
                throw new Exception("Setting not recognized: " + Component);
        }
    }
}
