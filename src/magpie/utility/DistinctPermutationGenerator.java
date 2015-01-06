
package magpie.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Given a list of objects, generate the set of distinct permutations.
 * @author Logan Ward
 */
abstract public class DistinctPermutationGenerator {
    /**
     * Given a list, generate all permutations that are not equal to each other
     * @param originalList List to generate permutations of
     * @return New list
     */
    static public Set<List<Object>> generatePermutations(List<Object> originalList) {
        Set<List<Object>> output = new HashSet<>();
        if (originalList.size() == 1) {
            output.add(originalList);
            return output;
        }
        // Make a copy of the input
        List<Object> listCopy;
        try {
            listCopy = originalList.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            listCopy = new LinkedList<>();
        }
        listCopy.addAll(originalList);
        for (int i=0; i<originalList.size(); i++) {
            Object head = listCopy.remove(i);
            // Generate all permuations without this entry
            Set<List<Object>> subPermutations = generatePermutations(listCopy);
            for (List<Object> subPermutation : subPermutations) {
                List<Object> newList;
                try {
                    newList = originalList.getClass().newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    newList = new LinkedList<>();
                }
                newList.add(head); // Add head to front of list
                newList.addAll(subPermutation); // Add in the rest of the entries
                output.add(newList);
            }
            // Place head back and repeat
            listCopy.add(i,head);
        }
        return output;
    }
    
    /**
     * Generate all distinct permutations of an array of doubles. Be careful 
     *  about ensuring elements of the list that are within a certain tolerance
     *  are made precisely equal
     * @param originalList List to be permuted
     * @return All distinct permutations of this list
     */
    static public Set<double[]> generatePermutations(double[] originalList) {
        // Create a copy of list using Doubles
        List<Object> listCopy = new ArrayList<>(originalList.length);
        for (double x : originalList) {
            listCopy.add(x);
        }
        // Generate permutations
        Set<List<Object>> lists = generatePermutations(listCopy);
        // Convert it back
        Set<double[]> output = new HashSet<>(); 
        for (List<Object> list : lists) {
            double[] toAdd = new double[originalList.length];
            for (int i=0; i<toAdd.length; i++) {
                toAdd[i] = (Double) list.get(i);
            }
            output.add(toAdd);
        }
        return output;
    }
	
	/**
	 * Generate all distinct permutations of an array of doubles. 
	 * @param originalList
	 * @return 
	 */
	static public Set<int[]> generatePermutations(int[] originalList) {
		// Create a copy of list using Doubles
        List<Object> listCopy = new ArrayList<>(originalList.length);
        for (int x : originalList) {
            listCopy.add(x);
        }
        // Generate permutations
        Set<List<Object>> lists = generatePermutations(listCopy);
        // Convert it back
        Set<int[]> output = new HashSet<>(); 
        for (List<Object> list : lists) {
            int[] toAdd = new int[originalList.length];
            for (int i=0; i<toAdd.length; i++) {
                toAdd[i] = (Integer) list.get(i);
            }
            output.add(toAdd);
        }
        return output;
	}
}
