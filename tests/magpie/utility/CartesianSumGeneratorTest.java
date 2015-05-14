package magpie.utility;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CartesianSumGeneratorTest {

    @Test
    public void test() {
        // Create list #1
        List<Integer> list1 = new LinkedList<>();
        list1.add(0);
        
        // Simple case
        CartesianSumGenerator<Integer> gen = new CartesianSumGenerator<>(list1);
        List<List<Integer>> results = new LinkedList<>();
        for (List<Integer> list : gen) results.add(list);
        assertEquals(1, results.size());
        
        list1.add(1);
        gen = new CartesianSumGenerator<>(list1);
        results.clear();
        for (List<Integer> list : gen) results.add(list);
        assertEquals(2, results.size());
        
        gen = new CartesianSumGenerator<>(list1, list1);
        results.clear();
        for (List<Integer> list : gen) results.add(list);
        assertEquals(4, results.size());
        
        // Create list #2
        List<Integer> list2 = new LinkedList<>();
        list2.add(0);
        
        gen = new CartesianSumGenerator<>(list1, list1, list2);
        results.clear();
        for (List<Integer> list : gen) results.add(list);
        assertEquals(4, results.size());
        
        list2.addAll(list1);
        gen = new CartesianSumGenerator<>(list1, list1, list2);
        results.clear();
        for (List<Integer> list : gen) results.add(list);
        assertEquals(12, results.size());
    }
    
}
