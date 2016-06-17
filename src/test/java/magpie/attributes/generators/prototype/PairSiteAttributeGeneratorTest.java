package magpie.attributes.generators.prototype;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.util.PrototypeSiteInformation;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PairSiteAttributeGeneratorTest {
    
    @Test
    public void test() throws Exception {
        // Make a A2(BB') site info 
        PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
        siteInfo.addSite(2, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{1}));
        
        // Make a prototype dataset with a new entries
        PrototypeDataset data = new PrototypeDataset();
        data.setSiteInfo(siteInfo);
        
        data.addEntry("H2HeLi");
        data.addEntry("H2LiHe");
        data.addEntry("He2LiH");
        
        data.addElementalProperty("Number");
        data.addElementalProperty("Row");
        
        // Generate attributes
        PairSiteAttributeGenerator gen = new PairSiteAttributeGenerator();
        gen.addAttributes(data);
        
        // Check out the attributes
        assertEquals(12, data.NAttributes());
        assertEquals(data.getAttributeName(0), 2, data.getEntry(0).getAttribute(0), 1e-6); // Number
        assertEquals(data.getAttributeName(1), 2, data.getEntry(0).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 1.5, data.getEntry(0).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 1.5, data.getEntry(0).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 1, data.getEntry(0).getAttribute(4), 1e-6); 
        assertEquals(data.getAttributeName(5), 1, data.getEntry(0).getAttribute(5), 1e-6); 
        
        assertEquals(data.getAttributeName(6), 1.0, data.getEntry(0).getAttribute(6), 1e-6); // Row
        assertEquals(data.getAttributeName(7), 1.0, data.getEntry(0).getAttribute(7), 1e-6);
        assertEquals(data.getAttributeName(8), 0.5, data.getEntry(0).getAttribute(8), 1e-6);
        assertEquals(data.getAttributeName(9), 0.5, data.getEntry(0).getAttribute(9), 1e-6);
        assertEquals(data.getAttributeName(10), 0, data.getEntry(0).getAttribute(10), 1e-6);
        assertEquals(data.getAttributeName(11), 0, data.getEntry(0).getAttribute(11), 1e-6);
        
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a),
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(1).getAttribute(a),
                    1e-6);
        }
        
        // He2LiH
        assertEquals(data.getAttributeName(0), 1.0, data.getEntry(2).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 1.0, data.getEntry(2).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 0, data.getEntry(2).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 0, data.getEntry(2).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), -1.0, data.getEntry(2).getAttribute(4), 1e-6);
        assertEquals(data.getAttributeName(5), 1.0, data.getEntry(2).getAttribute(5), 1e-6);
        
        assertEquals(data.getAttributeName(6), 1.0, data.getEntry(2).getAttribute(6), 1e-6);
        assertEquals(data.getAttributeName(7), 1.0, data.getEntry(2).getAttribute(7), 1e-6);
        assertEquals(data.getAttributeName(8), 0.5, data.getEntry(2).getAttribute(8), 1e-6);
        assertEquals(data.getAttributeName(9), 0.5, data.getEntry(2).getAttribute(9), 1e-6);
        assertEquals(data.getAttributeName(10), 0.0, data.getEntry(2).getAttribute(10), 1e-6);
        assertEquals(data.getAttributeName(11), 0.0, data.getEntry(2).getAttribute(11), 1e-6);
        
        // Print description
        System.out.println(gen.printDescription(true));
    }
    
    @Test
    public void testTwo() throws Exception {
        // Make a A(BB')C site info 
        PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{1}));
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        
        // Make a prototype dataset with a new entries
        PrototypeDataset data = new PrototypeDataset();
        data.setSiteInfo(siteInfo);
        
        data.addEntry("HHeLiBe");
        data.addEntry("HLiHeBe");
        
        data.addElementalProperty("Number");
        data.addElementalProperty("Row");
        
        // Generate attributes
        PairSiteAttributeGenerator gen = new PairSiteAttributeGenerator();
        gen.addAttributes(data);
        
        List<Object> options = new LinkedList<>();
        gen.setOptions(options);
        
        System.out.println(gen.printUsage());
        
        // Check out the attributes
        assertEquals(14*2, data.NAttributes());
        assertEquals(data.getAttributeName(0), 2, data.getEntry(0).getAttribute(0), 1e-6); // Number A-BC
        assertEquals(data.getAttributeName(1), 2, data.getEntry(0).getAttribute(1), 1e-6); 
        assertEquals(data.getAttributeName(2), 1.5, data.getEntry(0).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 1.5, data.getEntry(0).getAttribute(3), 1e-6); 
        assertEquals(data.getAttributeName(4), 1, data.getEntry(0).getAttribute(4), 1e-6); 
        assertEquals(data.getAttributeName(5), 1, data.getEntry(0).getAttribute(5), 1e-6); 
        
        assertEquals(data.getAttributeName(6), 3, data.getEntry(0).getAttribute(6), 1e-6); // Number A-D
        assertEquals(data.getAttributeName(7), 3, data.getEntry(0).getAttribute(7), 1e-6); 
        
        assertEquals(data.getAttributeName(8), 1, data.getEntry(0).getAttribute(8), 1e-6); // Number BC-D
        assertEquals(data.getAttributeName(9), 1, data.getEntry(0).getAttribute(9), 1e-6); 
        assertEquals(data.getAttributeName(10), 1.5, data.getEntry(0).getAttribute(10), 1e-6);
        assertEquals(data.getAttributeName(11), 1.5, data.getEntry(0).getAttribute(11), 1e-6);
        assertEquals(data.getAttributeName(12), 2, data.getEntry(0).getAttribute(12), 1e-6); 
        assertEquals(data.getAttributeName(13), 2, data.getEntry(0).getAttribute(13), 1e-6); 
        
        assertEquals(data.getAttributeName(14), 1, data.getEntry(0).getAttribute(14), 1e-6); // Row A-BC
        assertEquals(data.getAttributeName(15), 1, data.getEntry(0).getAttribute(15), 1e-6);
        assertEquals(data.getAttributeName(16), 0.5, data.getEntry(0).getAttribute(16), 1e-6);
        assertEquals(data.getAttributeName(17), 0.5, data.getEntry(0).getAttribute(17), 1e-6);
        assertEquals(data.getAttributeName(18), 0, data.getEntry(0).getAttribute(18), 1e-6);
        assertEquals(data.getAttributeName(19), 0, data.getEntry(0).getAttribute(19), 1e-6);
        
        assertEquals(data.getAttributeName(20), 1, data.getEntry(0).getAttribute(20), 1e-6); // Row A-D
        assertEquals(data.getAttributeName(21), 1, data.getEntry(0).getAttribute(21), 1e-6);
        
        assertEquals(data.getAttributeName(22), 0, data.getEntry(0).getAttribute(22), 1e-6); // Row BC-D
        assertEquals(data.getAttributeName(23), 0, data.getEntry(0).getAttribute(23), 1e-6);
        assertEquals(data.getAttributeName(24), 0.5, data.getEntry(0).getAttribute(24), 1e-6); 
        assertEquals(data.getAttributeName(25), 0.5, data.getEntry(0).getAttribute(25), 1e-6); 
        assertEquals(data.getAttributeName(26), 1, data.getEntry(0).getAttribute(26), 1e-6); 
        assertEquals(data.getAttributeName(27), 1, data.getEntry(0).getAttribute(27), 1e-6); 
        
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a),
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(1).getAttribute(a),
                    1e-6);
        }
        
        // Print description
        System.out.println(gen.printDescription(true));
    }
    
}
