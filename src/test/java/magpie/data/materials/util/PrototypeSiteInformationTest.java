package magpie.data.materials.util;

import java.io.File;
import java.io.PrintWriter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PrototypeSiteInformationTest {

    @Test
    public void testPerovskite() throws Exception {
        // This structure has 3 sites:
        //  A <- Forms a SC lattice
        //  B <- Forms a SC lattice
        //  C <- Typically O, omitted in this study
        //  Stoichiometry: ABO_3
        
        // Write a structure file to model this
        File file = new File("temp_strc.info");
        file.deleteOnExit();
        PrintWriter fp = new PrintWriter(file);
        fp.println("1");
        fp.println("1");
        fp.println("3");
        fp.close();
        
        // Read it in
        PrototypeSiteInformation siteInfo = 
                PrototypeSiteInformation.readFromFile(file.getAbsolutePath());
        
        // Test results
        assertEquals(3, siteInfo.NSites());
        assertEquals(3, siteInfo.NGroups());
        assertEquals(1, siteInfo.NOnSite(0), 1e-6);
        assertEquals(1, siteInfo.NOnSite(1), 1e-6);
        assertEquals(3, siteInfo.NOnSite(2), 1e-6);
        
        assertTrue(siteInfo.siteIsIncludedInAttributes(0));
        assertTrue(siteInfo.siteIsIncludedInAttributes(1));
        assertTrue(siteInfo.siteIsIncludedInAttributes(2));
        
        assertEquals(1, siteInfo.getSiteGroup(0).size());
        assertEquals("A", siteInfo.getGroupLabel(0));
        assertTrue(siteInfo.getSiteGroup(0).contains(0));
        assertEquals(1, siteInfo.getSiteGroup(1).size());
        assertEquals("B", siteInfo.getGroupLabel(1));
        assertTrue(siteInfo.getSiteGroup(1).contains(1));
        assertEquals(1, siteInfo.getSiteGroup(2).size());
        assertEquals("C", siteInfo.getGroupLabel(2));
        assertTrue(siteInfo.getSiteGroup(2).contains(2));
        
        assertEquals(1, siteInfo.getEquivalentArragements().size());
    }
    
    @Test
    public void testDoublePerovskite() throws Exception {
        // This structure has 5 sites:
        //  A, A' <- Form a rocksalt lattice, exchangable
        //  B, B' <- Form a rocksalt lattice, exchangable
        //  C     <- Typically O, omitted in this study
        //  Stoichiometry: AA'BB'O_6
        
        // Write a structure file to model this
        File file = new File("temp_strc.info");
        file.deleteOnExit();
        PrintWriter fp = new PrintWriter(file);
        fp.println("1 -equiv 1");
        fp.println("1 -equiv 0");
        fp.println("1 -equiv 3");
        fp.println("1 -equiv 2");
        fp.println("6 -omit");
        fp.close();
        
        // Read it in
        PrototypeSiteInformation siteInfo = 
                PrototypeSiteInformation.readFromFile(file.getAbsolutePath());
        
        // Test results
        assertEquals(5, siteInfo.NSites());
        assertEquals(3, siteInfo.NGroups());
        assertEquals(1, siteInfo.NOnSite(0), 1e-6);
        assertEquals(1, siteInfo.NOnSite(1), 1e-6);
        assertEquals(1, siteInfo.NOnSite(2), 1e-6);
        assertEquals(1, siteInfo.NOnSite(3), 1e-6);
        assertEquals(6, siteInfo.NOnSite(4), 1e-6);
        
        assertTrue(siteInfo.siteIsIncludedInAttributes(0));
        assertTrue(siteInfo.siteIsIncludedInAttributes(1));
        assertTrue(siteInfo.siteIsIncludedInAttributes(2));
        assertTrue(siteInfo.siteIsIncludedInAttributes(3));
        assertFalse(siteInfo.siteIsIncludedInAttributes(4));
        
        assertEquals(2, siteInfo.getSiteGroup(0).size());
        assertEquals("AB", siteInfo.getGroupLabel(0));
        assertTrue(siteInfo.getSiteGroup(0).contains(0));
        assertTrue(siteInfo.getSiteGroup(0).contains(1));
        assertTrue(siteInfo.groupIsIncludedInAttributes(0));
        
        assertEquals(2, siteInfo.getSiteGroup(1).size());
        assertEquals("CD", siteInfo.getGroupLabel(1));
        assertTrue(siteInfo.getSiteGroup(1).contains(2));
        assertTrue(siteInfo.getSiteGroup(1).contains(3));
        assertTrue(siteInfo.groupIsIncludedInAttributes(1));
        
        assertEquals(1, siteInfo.getSiteGroup(2).size());
        assertEquals("E", siteInfo.getGroupLabel(2));
        assertTrue(siteInfo.getSiteGroup(2).contains(4));
        assertFalse(siteInfo.groupIsIncludedInAttributes(2));
        
        assertEquals(4, siteInfo.getEquivalentArragements().size());
    }
    
    @Test
    public void testDoublePerovskiteAlt() throws Exception {
        // This structure has 5 sites:
        //  A, A' <- Form a rocksalt lattice, exchangable
        //  B, B' <- Form a rocksalt lattice, exchangable
        //  C     <- Typically O, omitted in this study
        //  Stoichiometry: AA'BB'O_6
        
        // Write a structure file to model this
        File file = new File("temp_strc.info");
        file.deleteOnExit();
        PrintWriter fp = new PrintWriter(file);
        fp.println("1 -equiv 3");
        fp.println("1 -equiv 2");
        fp.println("1 -equiv 1");
        fp.println("1 -equiv 0");
        fp.println("6 -omit");
        fp.close();
        
        // Read it in
        PrototypeSiteInformation siteInfo = 
                PrototypeSiteInformation.readFromFile(file.getAbsolutePath());
        
        // Test results
        assertEquals(5, siteInfo.NSites());
        assertEquals(3, siteInfo.NGroups());
        assertEquals(1, siteInfo.NOnSite(0), 1e-6);
        assertEquals(1, siteInfo.NOnSite(1), 1e-6);
        assertEquals(1, siteInfo.NOnSite(2), 1e-6);
        assertEquals(1, siteInfo.NOnSite(3), 1e-6);
        assertEquals(6, siteInfo.NOnSite(4), 1e-6);
        
        assertTrue(siteInfo.siteIsIncludedInAttributes(0));
        assertTrue(siteInfo.siteIsIncludedInAttributes(1));
        assertTrue(siteInfo.siteIsIncludedInAttributes(2));
        assertTrue(siteInfo.siteIsIncludedInAttributes(3));
        assertFalse(siteInfo.siteIsIncludedInAttributes(4));
        
        assertEquals(2, siteInfo.getSiteGroup(0).size());
        assertEquals("AD", siteInfo.getGroupLabel(0));
        assertTrue(siteInfo.getSiteGroup(0).contains(0));
        assertTrue(siteInfo.getSiteGroup(0).contains(3));
        assertTrue(siteInfo.groupIsIncludedInAttributes(0));
        
        assertEquals(2, siteInfo.getSiteGroup(1).size());
        assertEquals("BC", siteInfo.getGroupLabel(1));
        assertTrue(siteInfo.getSiteGroup(1).contains(1));
        assertTrue(siteInfo.getSiteGroup(1).contains(2));
        assertTrue(siteInfo.groupIsIncludedInAttributes(1));
        
        assertEquals(1, siteInfo.getSiteGroup(2).size());
        assertEquals("E", siteInfo.getGroupLabel(2));
        assertTrue(siteInfo.getSiteGroup(2).contains(4));
        assertFalse(siteInfo.groupIsIncludedInAttributes(2));
        
        assertEquals(4, siteInfo.getEquivalentArragements().size());
    }
    
}
