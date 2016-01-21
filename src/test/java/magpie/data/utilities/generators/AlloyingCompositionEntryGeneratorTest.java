package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class AlloyingCompositionEntryGeneratorTest {

    @Test
    public void test() throws Exception {
        // Make options
        List<Object> options = new ArrayList<>();
        options.add("NaCl");
        options.add("0.1");
        options.add("0.05");
        options.add("F");
        options.add("Br");
        
        // Make generator
        AlloyingCompositionEntryGenerator gen = new AlloyingCompositionEntryGenerator();
        gen.setOptions(options);
        System.out.println(gen.printUsage());
        
        // Run generator
        List<BaseEntry> entries = gen.generateEntries();
        
        // Check results
        assertEquals(4, entries.size());
        assertTrue(entries.contains(new CompositionEntry("(Na0.5Cl0.5)0.95F0.05")));
        assertTrue(entries.contains(new CompositionEntry("(Na0.5Cl0.5)0.9F0.1")));
        assertTrue(entries.contains(new CompositionEntry("(Na0.5Cl0.5)0.95Br0.05")));
        assertTrue(entries.contains(new CompositionEntry("(Na0.5Cl0.5)0.9Br0.1")));
    }
    
}
