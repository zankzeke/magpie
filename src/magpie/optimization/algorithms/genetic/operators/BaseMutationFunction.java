/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.algorithms.genetic.operators;

import java.io.Serializable;
import java.util.Set;
import magpie.data.BaseEntry;

/**
 * Mutation operations needed for a {@linkplain  GeneticAlgorithm}, takes a 
 * {@link magpie.data.BaseEntry} and modifies it slightly. 
 * 
 * <p><b>Implementation Guide</b>
 * <p>Implementations of this class must fulfill the following operations:
 * <ol>
 * <li>{@linkplain #configureFunction(magpie.data.Dataset) }: Given the search space, 
 * configure the function to mutate entries within acceptable ranges.</li>
 * <li>{@linkplain #mutate(magpie.data.BaseEntry) }: Mutate an entry.
 * </ol>
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseMutationFunction implements Serializable {
    
    /**
     * Configure the mutation function based on the search space. 
     * @param searchSpace Dataset containing all possible entries 
     * in the search space
     */
    abstract public void configureFunction(Set<BaseEntry> searchSpace);
    
    /**
     * Mutate an entry so that it only somewhat resembles the original
     * @param Entry Entry to be mutated
     */
    abstract public void mutate(BaseEntry Entry);
}
