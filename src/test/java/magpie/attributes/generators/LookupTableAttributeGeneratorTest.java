package magpie.attributes.generators;

import java.io.File;
import java.io.PrintWriter;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class LookupTableAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        
        // Make a fake lookup table
        PrintWriter fp = new PrintWriter("temp.lookup");
        fp.println("entry newAttr1");
        fp.println("NaCl 1.0");
        fp.close();
        new File("temp.lookup").deleteOnExit();
        
        // Add some attributes
        LookupTableAttributeGenerator gen = new LookupTableAttributeGenerator();
        gen.readAttributeTable("temp.lookup");
        gen.addAttributes(data);
        
        // Check on results
        assertEquals(1, data.NEntries());
        assertEquals(1, data.NAttributes());
        assertEquals(1, data.getEntry(0).getAttributes().length);
        assertEquals(1, data.getEntry(0).getAttribute(0), 1e-6);
    }
    
}
