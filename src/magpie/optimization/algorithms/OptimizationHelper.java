/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.algorithms;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Operations that can be useful during optimization.
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class OptimizationHelper {
    
    /**
     * Sort an array and return the original indices of the each value. 
     * @param x Array to be sorted
     * @param Descending Whether to sort the list in descending order
     * @return List of indices, sorted in same order as x
     */
    public static int[] sortAndGetRanks(double[] x, boolean Descending) {
        // Put each entry in a sorted set
        Comparator comp = new Comparator() {
            @Override  public int compare(Object o1, Object o2) {
                Pair<Integer,Double> n1 = (Pair) o1;
                Pair<Integer,Double> n2 = (Pair) o2;
                int output = n1.getRight().compareTo(n2.getRight());
                if (output == 0)
                    return n1.getLeft().compareTo(n2.getLeft());
                else
                    return output;
            }
        };
        NavigableSet<Pair<Integer,Double>> set = new TreeSet<>(comp);
        Pair<Integer,Double> temp;
        for (int i = 0; i < x.length; i++) {
             temp = new MutablePair<>(i,x[i]);
             set.add(temp);
        }
        
        // Retrieve the data from the set
        int[] output = new int[x.length];
        int i=0; 
        Iterator<Pair<Integer,Double>> iter = set.descendingIterator();
        while (iter.hasNext()) {
            temp = iter.next();
            output[i] = temp.getLeft();
            x[i] = temp.getRight();
            i++;
        }
        
        // Flip the order if needed    
        if (! Descending) {
            ArrayUtils.reverse(x);
            ArrayUtils.reverse(output);
        }
        return output;
    }
}
