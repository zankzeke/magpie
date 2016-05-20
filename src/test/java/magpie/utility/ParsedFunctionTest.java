
package magpie.utility;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ParsedFunctionTest {

    @Test
    public void test() throws Exception {
        ParsedFunction func = new ParsedFunction("#{a0} - #{b1}");
        
        // Check that it read correctly
        assertEquals(2, func.numVariables());
        assertArrayEquals(new String[]{"a0","b1"}, 
                func.getVariableNames().toArray(new String[0]));
        
        // Check setting variables through the "set" interface
        func.setVariable("a0", 1.0);
        func.setVariable(1, -1.0);
        assertEquals(2.0, func.evaluate(), 1e-6);
        
        // Check on the MultivariateFunction interface
        assertEquals(2.0, func.value(new double[]{1, -1}), 1e-6);
        assertEquals(6.0, func.value(new double[]{1, -5}), 1e-6);
    }
    
    @Test
    public void testWithCrazyNames() throws Exception {
        ParsedFunction func = new ParsedFunction("#{r:x,^x} * #{b**4-3}");
        
        // Check that it read correctly
        assertEquals(2, func.numVariables());
        assertArrayEquals(new String[]{"b**4-3", "r:x,^x"}, 
                func.getVariableNames().toArray(new String[0]));
        
        // Check setting variables through the "set" interface
        func.setVariable("b**4-3", 1.0);
        func.setVariable(1, -1.0);
        assertEquals(-1, func.evaluate(), 1e-6);
        
        // Check on the MultivariateFunction interface
        assertEquals(-1, func.value(new double[]{1, -1}), 1e-6);
        assertEquals(-5, func.value(new double[]{1, -5}), 1e-6);
    }
    
    @Test
    public void testFunctions() throws Exception {
        ParsedFunction func = new ParsedFunction("log(exp(#{x}))");
        
        // Check that it read correctly
        assertEquals(1, func.numVariables());
        assertArrayEquals(new String[]{"x"},
                func.getVariableNames().toArray());
        
        // Check setting variables through the "set" interface
        func.setVariable("x", 1.0);
        assertEquals(1.0, func.evaluate(), 1e-6);
        
        // Check on the MultivariateFunction interface
        assertEquals(2.0, func.value(new double[]{2}), 1e-6);
        assertEquals(6.0, func.value(new double[]{6}), 1e-6);
    }
}
