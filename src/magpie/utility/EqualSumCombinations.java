/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.utility;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Iterator over all combinations of nonnegative integers that have equal sum. 
 * 
 * <p>Example Iteration: (2 0 0) -> (1 1 0) -> (1 0 1) -> (0 2 0) -> (0 1 1) -> (0 0 2)
 * 
 * @author Logan Ward
 */
public class EqualSumCombinations implements Iterable<int[]> {
    /** Starting position */
    private int[] start = null;
    /** Sum */
    private int sum = 0;

    /**
     * Create an iterator over all possible vectors with a certain number of entries
     * that are all nonnegative integers that sum to the same amount.
     * @param sum Desired sum (must be greater than 0)
     * @param size Number of integers (must be greater than 1)
     */
    public EqualSumCombinations(int sum, int size) {
        if (sum <= 0) {
            throw new Error("Sum must be positive");
        }
        this.sum = sum;
        if (size < 2) {
            throw new Error("Size must be greater than 1");
        }
        start = new int[size];
        start[0] = sum;
    }

    @Override
    public Iterator<int[]> iterator() {
        Iterator<int[]> output = new Iterator<int[]>() {
            /** Position of iterator */
            int[] position = start.clone();
                
            @Override
            public boolean hasNext() {
                return position != null; 
            }

            @Override
            public int[] next() {
                int[] output = position.clone();
                EqualSumCombinations.incrementCounter(position, sum);
                if (position[0] == sum) { // It has looped back through
                    position = null;
                }
                return output;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported for this iterator."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        return output;
    }
    

    /**
     * Increment a counter that iterates through all possible vectors. Starts at 
     *  [0, ..., sum] and ends at [sum, 0, ...].
     * @param counter Counter to be incremented
     * @param sum Sum of elements in vector
     */
    public static void incrementCounter(int[] counter, int sum) {
        for (int pos=counter.length - 2; pos >= 0; pos--) {
            if (counter[pos] > 0) {
                counter[pos]--;
                counter[pos+1]++;
                return;
            } else {
                counter[pos] = counter[pos+1];
                counter[pos+1] = 0;
            }
        }
    }
    
    /**
     * Generate all possible vectors
     * @return List containing all possible vectors
     */
    public List<int[]> generateAll() {
        List<int[]> output = new LinkedList<>();
        for (int[] possibility : this) {
            output.add(possibility);
        }
        return output;
    }
}
