package magpie.data.materials;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ElementEntryTest {

    @Test
    public void test() throws Exception {
        ElementEntry entry = new ElementEntry("Fe");
        assertEquals("Fe", entry.toString());
        assertEquals("Fe", entry.toHTMLString());
        assertEquals(25, entry.getElementID());
        
        entry = new ElementEntry(25, false);
        assertEquals("Fe", entry.toString());
        assertEquals("Fe", entry.toHTMLString());
        assertEquals(25, entry.getElementID());
        
        entry = new ElementEntry(26, true);
        assertEquals("Fe", entry.toString());
        assertEquals("Fe", entry.toHTMLString());
        assertEquals(25, entry.getElementID());
    }
    
}
