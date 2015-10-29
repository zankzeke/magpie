
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
        ParsedFunction func = new ParsedFunction("#{a} - #{b}");
        
        // Check that it read correctly
        assertEquals(2, func.numVariables());
        assertArrayEquals(new String[]{"a","b"}, 
                func.getVariableNames().toArray(new String[0]));
        
        // Check setting variables through the "set" interface
        func.setVariable("a", 1.0);
        func.setVariable(1, -1.0);
        assertEquals(2.0, func.evaluate(), 1e-6);
        
        // Check on the MultivariateFunction interface
        assertEquals(2.0, func.value(new double[]{1, -1}), 1e-6);
        assertEquals(6.0, func.value(new double[]{1, -5}), 1e-6);
    }
    
}
