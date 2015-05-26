package magpie.data.utilities.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.util.PrototypeSiteInformation;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan
 */
public class PrototypeEntryGeneratorTest {
    
    public PrototypeEntryGeneratorTest() {
    }

    @Test
    public void testNoMixing() throws Exception {
        // Make site information
        PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
        siteInfo.addSite(3, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        
        // Element list
        List<String> elems = new LinkedList();
        elems.add("Fe");
        elems.add("Ti");
        
        // Make the generator
        PrototypeEntryGenerator gen = new PrototypeEntryGenerator();
        gen.setSiteInfo(siteInfo);
        gen.defineSitePossibilities(0, 1, 25, elems);
        gen.defineSitePossibilities(1, 1, 25, elems);
        
        // Generate entries
        List<BaseEntry> entries = gen.generateEntries();
        
        // Checks
        assertEquals(4, entries.size());
    }
    
    @Test
    public void testMixing() throws Exception {
        // Make site information
        PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
        siteInfo.addSite(3, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        
        // Element list
        List<String> elems = new LinkedList();
        elems.add("Fe");
        elems.add("Ti");
        
        // Make the generator
        PrototypeEntryGenerator gen = new PrototypeEntryGenerator();
        gen.setSiteInfo(siteInfo);
        gen.defineSitePossibilities(0, 2, 25, elems);
        gen.defineSitePossibilities(1, 2, 25, elems);
        
        // Generate entries
        List<BaseEntry> entries = gen.generateEntries();
        
        // Checks
        assertEquals(25, entries.size());
    }
    
}
