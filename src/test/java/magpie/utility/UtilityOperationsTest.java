package magpie.utility;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class UtilityOperationsTest {
    
    @Test
    public void testIsInteger() {
        assertTrue(UtilityOperations.isInteger("1"));
        assertTrue(UtilityOperations.isInteger("-1500123"));
        assertFalse(UtilityOperations.isInteger("-1.0"));
        assertFalse(UtilityOperations.isInteger("1.0"));
        assertFalse(UtilityOperations.isInteger("word"));
    }
    
    @Test
    public void testFindFile() {
        File file = UtilityOperations.findFile("py/lasso_attribute_selection.py");
        assertTrue(file.exists());
        
        file = UtilityOperations.findFile("asdfl;kjasdfaer.asdf");
        assertNull(file);// Unless I'm really unlucky
    }
    
}
