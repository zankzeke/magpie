/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.utility;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for EqualSumCombinations
 * @author Logan Ward
 */
public class EqualSumCombinationsTest {

    /**
     * Test of incrementCounter method, of class EqualSumCombinations.
     */
    @Test
    public void testIncrementCounter() {
        int[] pos = new int[]{2,0};
        EqualSumCombinations.incrementCounter(pos, 2);
        assertArrayEquals(new int[]{1,1}, pos);
    }

    /**
     * Test of generateAll method, of class EqualSumCombinations.
     */
    @Test
    public void testGenerateAll() {
        EqualSumCombinations x = new EqualSumCombinations(2, 2);
        assertTrue(x.generateAll().size() == 3);
        x = new EqualSumCombinations(2, 3);
        assertTrue(x.generateAll().size() == 6);
    }
}
