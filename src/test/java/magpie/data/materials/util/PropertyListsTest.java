package magpie.data.materials.util;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PropertyListsTest {
    
    @Test
    public void test() throws Exception {
        for (String set : new String[]{"general", "radii"}) {
            String[] props = PropertyLists.getPropertySet(set);
            
            // Make sure all files are there
            for (String prop : props) {
                assertTrue(prop, new File("lookup-data", prop + ".table").isFile());
            }
        }
    }
    
}
