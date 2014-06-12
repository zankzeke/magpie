/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.algorithms.genetic.operators;

import java.io.Serializable;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Operation that performs crossover for a genetic algorithm. 
 * 
 * @author Logan Ward
 * @version 0.1
 */

abstract public class BaseCrossoverFunction implements Serializable {
    /**
     * Given two BaseEntry classes, return a third that is a combination of the two
     * @param A Parent A
     * @param B Parent B
     * @return Offspring
     */
    abstract public BaseEntry crossover(BaseEntry A, BaseEntry B);
}
