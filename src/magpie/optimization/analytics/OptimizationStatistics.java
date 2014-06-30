/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.analytics;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.utilities.filters.BaseDatasetFilter;
import magpie.optimization.rankers.EntryRanker;
import magpie.optimization.BaseOptimizer;
import magpie.optimization.rankers.MultiObjectiveEntryRanker;
import magpie.utility.interfaces.Printable;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Basic statistics about the performance optimization algorithms. Useful in cases
 *  where the correct answers both are known and yet-undiscovered.
 * 
 * @author Logan Ward
 * @version 1.0
 */
public class OptimizationStatistics implements java.io.Serializable, java.lang.Cloneable, Printable {
    // ---> Settings
	/** Defines when an entry is considered successful */
	protected BaseDatasetFilter SuccessFilter = null;
    /** Number of top entries to select */
    public int NumberTopEntries = 50;
    
    // ---> Things from the dataset
    /** A list of top entries that optimization algorithm is designed to find. 
	 * Only used if the search space associated with the algorithm being evaluated
	 *  contains the measured properties of each possible candidate.
	 */
    protected Dataset TopEntries = null;
	/** A list of entries that pass the SuccessFilter (if defined) */
	protected Dataset SuccessfulEntries = null;
    
    
    // ---> Results
    /** Number of iterations that have been evaluated */
    protected int IterationsEvaluated = 0;
    /** Number of entries that have been evaluated so far */
    protected int[] EvaluatedSoFar = null;
    /** Number of top entries found in the population for each iteration */
    protected int[] TopEntriesFound = null;
    /** Whether all of the top entries have been found */
    protected boolean[] FoundAllTop = null;
    /** Average objective function of entries added by the previous generation */
    protected double[] GenerationAverage = null;
    /** Best objective function of entries added by the previous generation */
    protected double[] GenerationBest = null;
    /** Best objective function of entries added by all previous generations */
    protected double[] BestSoFar = null;
    /** Number of entries pass a success filter */
    protected int[] NSuccess = null;
	/** Whether all successful entries have been found */
	protected boolean[] FoundAllSuccess = null;

	/** 
	 * Define a filter to define when the optimization algorithm has found an acceptable 
	 *  entry.
	 * @param filter Desired filter
	 */
    public void setSuccessFilter(BaseDatasetFilter filter) {
        this.SuccessFilter = filter;
    }

	/**
	 * Define the number of top entries to hold for statistical purposes.
	 * @param NumberTopEntries Desired number
	 */
	public void setNumberTopEntries(int NumberTopEntries) {
		this.NumberTopEntries = NumberTopEntries;
	}
    
    /**
     * Evaluate the performance of an optimizer. Produces statistics for each iteration
     * @param Optimizer Optimization run to be analyzed
     */
    public void evaluate(BaseOptimizer Optimizer) {
        // If answers to this optimization problem are known, find them
        collectAnswers(Optimizer);
        
        // Allocate arrays for results
        allocateResults(Optimizer.currentIteration() + 1);
        
        // Evaluate each geneneration
        Set Generation = new TreeSet<>();
        Set Population = new TreeSet<>(Generation);
		Dataset genDataset = Optimizer.getEmptyDataset(),
			popDataset = Optimizer.getEmptyDataset();
        for (int i=0; i<=Optimizer.currentIteration(); i++) {
            // Get the new popoulation and generation
            Generation.clear();
            Generation.addAll(Optimizer.getGeneration(i).getEntries());
            Population.addAll(Generation);
            // Evaluate it
            genDataset.clearData();
			genDataset.addEntries(Generation);
            popDataset.clearData();
			popDataset.addEntries(Population);
            evaluateEntries(genDataset, popDataset, Optimizer.getObjectiveFunction(), i);
        }
    }
    
    /** 
     * Given the new entries for a generation and the total population so far, 
     * calculate the statistics for this generation
     * @param Generation New entries added this iteration
     * @param Population Total population so far
     * @param Ranker Objective function used during optimization
     * @param IterationNumber Which iteration we are one
     */
    protected void evaluateEntries(Dataset Generation,
            Dataset Population, EntryRanker Ranker, int IterationNumber) {
        Ranker.setUseMeasured(true);
        
        // If necessary, train the objective function on the total population
        if (Ranker instanceof MultiObjectiveEntryRanker) {
            MultiObjectiveEntryRanker p = (MultiObjectiveEntryRanker) Ranker;
            if (! (Population instanceof MultiPropertyDataset)) {
                throw new Error("Data must extend MultiPropertyDataset");
            }
            MultiPropertyDataset p2 = (MultiPropertyDataset) Population;
            p.train(p2);
        }
        
        // Run the objective function on each population
        double[] gen_obj = new double[Generation.NEntries()],
                pop_obj = new double[Population.NEntries()];
        int i=0; Iterator<BaseEntry> iter = Generation.getEntries().iterator();
        while(iter.hasNext()) {
            gen_obj[i] = Ranker.objectiveFunction(iter.next()); i++;
        }
        i=0; iter = Population.getEntries().iterator();
        while(iter.hasNext()) {
            pop_obj[i] = Ranker.objectiveFunction(iter.next()); i++;
        }
        
        // Now, gather the statistics
        if (Ranker.isMaximizing()) {
            BestSoFar[IterationNumber] = StatUtils.max(pop_obj);
            GenerationBest[IterationNumber] = StatUtils.max(gen_obj);
        } else {
            BestSoFar[IterationNumber] = StatUtils.min(pop_obj);
            GenerationBest[IterationNumber] = StatUtils.min(gen_obj);
        }
        GenerationAverage[IterationNumber] = StatUtils.mean(gen_obj);
        EvaluatedSoFar[IterationNumber] = Population.NEntries();
        
        // Check how many top entries have been found
        if (TopEntries != null) {
            for (i=0; i<NumberTopEntries; i++) {
                BaseEntry Entry = TopEntries.getEntry(i);
                if (Population.containsEntry(Entry)) TopEntriesFound[IterationNumber]++;
            }
        }
        
        if (TopEntriesFound[IterationNumber] == NumberTopEntries) {
            FoundAllTop[IterationNumber] = true;
        }
        
        // Calculate how many "successes" have been found
		if (SuccessFilter != null) {
			Dataset popClone = Population.clone();
			SuccessFilter.filter(popClone);
			NSuccess[IterationNumber] = popClone.NEntries();
			if (SuccessfulEntries != null) {
				FoundAllSuccess[IterationNumber] = true;
				for (BaseEntry e : SuccessfulEntries.getEntries()) {
					if (! Population.containsEntry(e)) {
						FoundAllSuccess[IterationNumber] = false;
						break;
					}
				}
			}
		}
    }
    
    /**
     * If answers to optimization problem are already known, collect them. This requires
	 * that the optimizer being evaluated has measured properties in the search space.
     * 
     * @param Optimizer Optimization run to draw results from
     */
    protected void collectAnswers(BaseOptimizer Optimizer) {
        // Get the search space
        Dataset searchSpace = Optimizer.getInitialData().emptyClone();
        searchSpace.addEntries(Optimizer.getSearchSpace());
        
        // Set top entries (may do nothing if no mesaured values)
        findTopEntries(searchSpace, Optimizer.getObjectiveFunction());
		
		// Find entries that pass the success filter, if defined
		findSuccesses(searchSpace);
    }
    
    /**
     * Set the top entry list with a known list
     * @param searchSpace Search space from which to pull top entries
     * @param ranker Objective function to use for ranking entries
     */
    protected void findTopEntries(Dataset searchSpace, EntryRanker ranker) {
        try {
            // If necessary, train the objective function on the total population
            if (ranker instanceof MultiObjectiveEntryRanker) {
                MultiObjectiveEntryRanker p = (MultiObjectiveEntryRanker) ranker;
                if (! (searchSpace instanceof MultiPropertyDataset)) {
                    throw new Error("Data must extend MultiPropertyDataset");
                }
                MultiPropertyDataset p2 = (MultiPropertyDataset) searchSpace;
                p.train(p2);
            }
            int[] rank = ranker.rankEntries(searchSpace, true);
            TopEntries = searchSpace.emptyClone();
            for (int i=0; i<NumberTopEntries; i++)
                TopEntries.addEntry(searchSpace.getEntry(rank[i]));
        } catch (Exception e) {
            TopEntries = null;
        }
    }
	
	/**
	 * Find all entries in the search space that pass the success filter. If there
	 *  are no measured classes in the search space dataset or no success filter is
	 *  defined, will set {@linkplain #SuccessfulEntries} to null.
	 * 
	 * @param searchSpace Search space
	 */
	protected void findSuccesses(Dataset searchSpace) {
		if (SuccessFilter == null) {
			SuccessfulEntries = null;
			return;
		}
		try {
			SuccessFilter.train(searchSpace);
			SuccessfulEntries = searchSpace.clone();
			SuccessFilter.filter(SuccessfulEntries);
		} catch (Exception e) {
			SuccessfulEntries = null;
		}
	}

    /**
     * Before evaluation, allocate result arrays
     * @param NumberGenerations Number of generations to expect
     */
    protected void allocateResults(int NumberGenerations) {
        // Allocate result arrays
        IterationsEvaluated = NumberGenerations;
        BestSoFar = new double[NumberGenerations];
        GenerationAverage = new double[NumberGenerations];
        GenerationBest = new double[NumberGenerations];
        TopEntriesFound = new int[NumberGenerations];
        FoundAllTop = new boolean[NumberGenerations];
        EvaluatedSoFar = new int[NumberGenerations];
        if ( SuccessFilter != null) {
            NSuccess = new int[NumberGenerations];
			if (SuccessfulEntries != null) {
				FoundAllSuccess = new boolean[NumberGenerations];
			}
		}
    }
    
    /**
     * Print out all results into a neatly-formated table
     * @return Table as string
     */
    public String printResults() {
        String output = getHeader() + "\n";
        for (int i=0; i<IterationsEvaluated; i++)
            output += printData(i) + "\n";
        return output;
    }
    
    /**
     * Print out a header that has all statistics which have been calculated. It will 
     * look something like:<br>
     * <center>IterationNumber  EvaluatedSoFar    BestSoFar...</center>
     * @return This header as a string
     */
    protected String getHeader() {
        String output = "IterationNumber  EvaluatedSoFar  BestSoFar";
        output += "  GenerationAverage  GenerationBest";
		if (TopEntries != null) {
			output += "TopEntriesFound  FoundAllTop";
		}
        if (SuccessFilter != null) {
            output += "  SuccessesFound";
			if (SuccessfulEntries != null) {
				output += "  FoundAllSuccesses";
			}
		}
        return output;
    }
    
    /**
     * Print out the data for a single iteration
     * @param i Iteration number to print
     * @return String containing all available data (blank if missing)
     */
    protected String printData (int i) {
        String output = String.format("%15d  %14d", i, EvaluatedSoFar[i]);
        output += String.format("  %9.5f", BestSoFar[i]);
        output += String.format("  %17.5f", GenerationAverage[i]);
        output += String.format("  %14.5f", GenerationBest[i]);
		if (TopEntries != null) {
			output += String.format("  %15d", TopEntriesFound[i]);
			output += String.format("  %8d", FoundAllTop[i] ? 1 : 0);
		}
        if (SuccessFilter != null) {
            output += String.format("  %13d", NSuccess[i]);
			if (SuccessfulEntries != null) {
				output += String.format("  %17d", FoundAllSuccess[i] ? 1 : 0);
			}
		}
        return output;
    }
    
    @Override@SuppressWarnings("CloneDeclaresCloneNotSupported")
    public OptimizationStatistics clone() {
        OptimizationStatistics x;
        try { x = (OptimizationStatistics) super.clone(); }
        catch (CloneNotSupportedException c) {throw new Error(c);}
        if (BestSoFar != null) {
            x.BestSoFar = Arrays.copyOf(BestSoFar, IterationsEvaluated);
            x.GenerationAverage = Arrays.copyOf(GenerationAverage, IterationsEvaluated);
            x.GenerationBest = Arrays.copyOf(GenerationBest, IterationsEvaluated);
            x.TopEntriesFound = Arrays.copyOf(TopEntriesFound, IterationsEvaluated);
        }
        if (SuccessFilter != null) {
            x.NSuccess = Arrays.copyOf(NSuccess, IterationsEvaluated);
			if (SuccessfulEntries != null) {
				x.FoundAllSuccess = FoundAllSuccess.clone();
			}
        }
        if (TopEntries != null) {
            x.TopEntries = TopEntries.clone();
			x.FoundAllTop = FoundAllTop.clone();
		}
        return x;
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) return about();
        switch (Command.get(0).toLowerCase()) {
            case "stats":
                return printResults();
            default:
                throw new Exception("Print command \"" + Command.get(0) + "\" not supported.");
        }
    }

    @Override
    public String about() {
        return "Number of iterations evaluated: " + IterationsEvaluated;
    }
}
