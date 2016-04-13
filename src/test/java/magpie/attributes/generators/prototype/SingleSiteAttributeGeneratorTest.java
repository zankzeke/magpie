package magpie.attributes.generators.prototype;

import java.util.Arrays;
import java.util.LinkedList;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.util.PrototypeSiteInformation;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class SingleSiteAttributeGeneratorTest {

    @Test
    public void test() throws Exception {
        // Make a A2(BB')O_3 site info (e.g., like L2_1)
        PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
        siteInfo.addSite(2, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{1}));
        siteInfo.addSite(3, false, new LinkedList<Integer>());
        
        // Make a prototype dataset with a new entries
        PrototypeDataset data = new PrototypeDataset();
        data.setSiteInfo(siteInfo);
        
        data.addEntry("H2HeLiO3");
        data.addEntry("H2LiHeO3");
        data.addEntry("He2LiHO3");
        
        data.addElementalProperty("Number");
        
        // Generate attributes
        SingleSiteAttributeGenerator gen = new SingleSiteAttributeGenerator();
        gen.addAttributes(data);
        
        // Check out the attributes
        assertEquals(6, data.NAttributes());
        assertEquals(data.getAttributeName(0), 1, data.getEntry(0).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 2, data.getEntry(0).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 2.5, data.getEntry(0).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 3, data.getEntry(0).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 0.5, data.getEntry(0).getAttribute(4), 1e-6);
        assertEquals(data.getAttributeName(5), 1, data.getEntry(0).getAttribute(5), 1e-6);
        
        for (int a=0; a<data.NAttributes(); a++) {
            assertEquals(data.getAttributeName(a),
                    data.getEntry(0).getAttribute(a),
                    data.getEntry(1).getAttribute(a),
                    1e-6);
        }
        
        assertEquals(data.getAttributeName(0), 2, data.getEntry(2).getAttribute(0), 1e-6);
        assertEquals(data.getAttributeName(1), 1, data.getEntry(2).getAttribute(1), 1e-6);
        assertEquals(data.getAttributeName(2), 2, data.getEntry(2).getAttribute(2), 1e-6);
        assertEquals(data.getAttributeName(3), 3, data.getEntry(2).getAttribute(3), 1e-6);
        assertEquals(data.getAttributeName(4), 1, data.getEntry(2).getAttribute(4), 1e-6);
        assertEquals(data.getAttributeName(5), 2, data.getEntry(2).getAttribute(5), 1e-6);
        
        // Print description
        System.out.println(gen.printDescription(true));
    }
    
    @Test
    public void testDescription() throws Exception {
        // Make a A2(BB')O_3 site info (e.g., like L2_1)
        PrototypeSiteInformation siteInfo = new PrototypeSiteInformation();
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        
        // Make a prototype dataset with a new entries
        PrototypeDataset data = new PrototypeDataset();
        data.setSiteInfo(siteInfo);
        data.addElementalProperty("Number");
        
        // Create generator
        SingleSiteAttributeGenerator gen = new SingleSiteAttributeGenerator();
        
        // Print description
        gen.addAttributes(data);
        System.out.println(gen.printDescription(true));
        
        // Add a set of equivalent sites
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{1}));
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{1}));
        data.clearAttributes();
        gen.addAttributes(data);
        System.out.println(gen.printDescription(true));
        
        // Add a set of equivalent sites
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{4}));
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{4}));
        data.clearAttributes();
        gen.addAttributes(data);
        System.out.println(gen.printDescription(true));
        
        // Add a set of equivalent sites
        siteInfo.addSite(1, true, new LinkedList<Integer>());
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{7}));
        siteInfo.addSite(1, true, Arrays.asList(new Integer[]{7}));
        data.clearAttributes();
        gen.addAttributes(data);
        System.out.println(gen.printDescription(true));
    }
}
