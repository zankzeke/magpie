/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.optimization.rankers;

import magpie.data.MultiPropertyDataset;

/**
 * Base class for multi-objective entry rankers. 
 * 
 * @author Logan Ward
 */
abstract public class MultiObjectiveEntryRanker extends EntryRanker implements Cloneable {

    /**
     * Train the entry ranker. This may be necessary for multi-objective methods
     *  that rely on the performance of other entries (e.g. Pareto fronts).
     * 
     * <p>Note: This might be an opportune operation to map the name
     * of each property with their index to allow for faster operation of 
     * the {@linkplain EntryRanker#objectiveFunction(magpie.data.BaseEntry)}
     */
    abstract public void train(MultiPropertyDataset data);
    
    /**
     * Get properties currently used in objective functions.
     *
     * @return Array of properties currently in use
     */
    abstract public String[] getObjectives();
    
}
