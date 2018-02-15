package magpie.data.materials;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PrototypeDatasetTest {

    @Test
    public void test() throws Exception {
        // Write out the data
        File siteInfo = new File("temp-site-info.file");
        PrintWriter fp = new PrintWriter(new FileOutputStream(siteInfo));
        fp.println("1\n1 -omit");
        fp.close();
        siteInfo.deleteOnExit();

        File dataFile = new File("temp-poly-data.file");
        fp = new PrintWriter(new FileOutputStream(dataFile));
        fp.println("composition delta_e");
        fp.println("AlCl 1");
        fp.println("AlCl 2");
        fp.println("HCl 3");
        fp.close();
        dataFile.deleteOnExit();

        // Read it in
        PrototypeDataset data = new PrototypeDataset();
        data.readStructureInformation(siteInfo.getAbsolutePath());
        data.importText(dataFile.getAbsolutePath(), null);

        // Check the data
        assertEquals(3, data.NEntries());
        assertEquals("Al", data.getEntry(0).getSiteComposition(0).toString());
        assertArrayEquals(new double[]{1,2,3}, data.getMeasuredPropertyArray(0), 1e-6);
    }
    
}
