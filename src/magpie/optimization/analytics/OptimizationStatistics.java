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
import magpie.optimization.rankers.EntryRanker;
import magpie.optimization.BaseOptimizer;
import magpie.utility.interfaces.Printable;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Basic statistics and analysis methods for optimization algorithms.
 * 
 * @author Logan Ward
 * @version 1.0
 */
public class OptimizationStatistics implements java.io.Serializable, java.lang.Cloneable, Printable {
    // ---> Settings
    /** If set, will calculate fraction of entries past a certain threshold */
    public double Threshold = Double.NaN;
    /** Number of top entries to select */
    public int NumberTopEntries = 50;
    
    // ---> Things from the dataset
    /** A list of top entries that your algorithm is designed to find */
    protected Dataset TopEntries = null;
    
    
    // ---> Results
    /** Number of iterations that have been evaluated */
    public int IterationsEvaluated = 0;
    /** Number of entries that have been evaluated so far */
    public int[] EvaluatedSoFar = null;
    /** Number of top entries found in the population for each iteration */
    public int[] TopEntriesFound = null;
    /** Whether all of the top entries have been found */
    public boolean[] FoundAll = null;
    /** Average objective function of entries added by the previous generation */
    public double[] GenerationAverage = null;
    /** Best objective function of entries added by the previous generation */
    public double[] GenerationBest = null;
    /** Best objective function of entries added by all previous generations */
    public double[] BestSoFar = null;
    /** Number of entries higher/lower than a cutoff */
    public int[] PastThreshold = null;

    /**
     * Define a threshold defining when an entry is a "hit". If the goal is to maximize 
     *  the objective function, any entry above the threshold will be marked as 
     *  a success. Vis versa for minimization.
     * @param Threshold Desired threshold
     */
    public void setThreshold(double Threshold) {
        this.Threshold = Threshold;
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
        // If not set already, find the top entries from this run
        setTopEntries(Optimizer);
        
        // Allocate arrays for results
        allocateResults(Optimizer.currentIteration() + 1);
        
        // Evaluate each geneneration
        Set Generation = new TreeSet<>();
        Set Population = new TreeSet<>(Generation);
        for (int i=0; i<=Optimizer.currentIteration(); i++) {
            // Get the new popoulation and generation
            Generation.clear();
            Generation.addAll(Optimizer.getGeneration(i).getEntries());
            Population.addAll(Generation);
            // Evaluate it
            evaluateEntries(Generation, Population, Optimizer.getObjectiveFunction(), i);
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
    protected void evaluateEntries(Set<BaseEntry> Generation,
            Set<BaseEntry> Population, EntryRanker Ranker, int IterationNumber) {
        Ranker.UseMeasured = true;
        // Run the objective funciton on each population
        double[] gen_obj = new double[Generation.size()],
                pop_obj = new double[Population.size()];
        int i=0; Iterator<BaseEntry> iter = Generation.iterator();
        while(iter.hasNext()) {
            gen_obj[i] = Ranker.objectiveFunction(iter.next()); i++;
        }
        i=0; iter = Population.iterator();
        while(iter.hasNext()) {
            pop_obj[i] = Ranker.objectiveFunction(iter.next()); i++;
        }
        
        // Now, gather the statistics
        if (Ranker.MaximizeFunction) {
            BestSoFar[IterationNumber] = StatUtils.max(pop_obj);
            GenerationBest[IterationNumber] = StatUtils.max(gen_obj);
        } else {
            BestSoFar[IterationNumber] = StatUtils.min(pop_obj);
            GenerationBest[IterationNumber] = StatUtils.min(gen_obj);
        }
        GenerationAverage[IterationNumber] = StatUtils.mean(gen_obj);
        EvaluatedSoFar[IterationNumber] = Population.size();
        
        // Check how many top entries have been found
        for (i=0; i<NumberTopEntries; i++) {
            BaseEntry Entry = TopEntries.getEntry(i);
            if (Population.contains(Entry)) TopEntriesFound[IterationNumber]++;
        }
        if (TopEntriesFound[IterationNumber] == NumberTopEntries)
            FoundAll[IterationNumber] = true;
        
        // Check how many past a threshold
        if (! Double.isNaN(Threshold))
            for (i=0; i<pop_obj.length; i++) 
                if (Ranker.MaximizeFunction && pop_obj[i] > Threshold) 
                    PastThreshold[IterationNumber]++;
                else if (! Ranker.MaximizeFunction && pop_obj[i] < Threshold)
                    PastThreshold[IterationNumber]++;
    }
    
    /**
     * Get a list of the top entries from the Optimizer. If the search space has measured
     *  properties, extract the top 
     * 
     * @param Optimizer Optimization run to draw results from
     */
    protected void setTopEntries(BaseOptimizer Optimizer) {
        // Get the search space
        Dataset SearchSpace = Optimizer.getInitialData().emptyClone();
        SearchSpace.addEntries(Optimizer.getSearchSpace());
        
        // Check if we have measured entries
        if (SearchSpace.getEntry(0).hasMeasurement())
            setTopEntries(SearchSpace, Optimizer.getObjectiveFunction());
        else // Otherwise, mark as null
            TopEntries = null;
    }
    
    /**
     * Set the top entry list with a known list
     * @param SearchSpace Search space from which to pull top entries
     * @param Ranker Objective function to use for ranking entries
     */
    protected void setTopEntries(Dataset SearchSpace, EntryRanker Ranker) {
        int[] rank = Ranker.rankEntries(SearchSpace, true);
        TopEntries = SearchSpace.emptyClone();
        for (int i=0; i<NumberTopEntries; i++)
            TopEntries.addEntry(SearchSpace.getEntry(rank[i]));
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
        FoundAll = new boolean[NumberGenerations];
        EvaluatedSoFar = new int[NumberGenerations];
        if (! Double.isNaN(Threshold))
            PastThreshold = new int[NumberGenerations];
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
        output += "  GenerationAverage  GenerationBest  TopEntriesFound";
        output += "  FoundAll";
        if (! Double.isNaN(Threshold))
            output+="  PastThreshold";
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
        output += String.format("  %15d", TopEntriesFound[i]);
        output += String.format("  %8d", FoundAll[i] ? 1 : 0);
        if (! Double.isNaN(Threshold))
            output += String.format("  %13d", PastThreshold[i]);
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
        if (! Double.isNaN(Threshold)) {
            x.PastThreshold = Arrays.copyOf(PastThreshold, IterationsEvaluated);
        }
        if (TopEntries != null) 
            x.TopEntries = TopEntries.clone();
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
