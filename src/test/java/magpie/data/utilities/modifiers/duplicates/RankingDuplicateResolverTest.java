package magpie.data.utilities.modifiers.duplicates;

import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class RankingDuplicateResolverTest {

    @Test
    public void testSimple() throws Exception {
        // Make a dataset
        Dataset data = new Dataset();
        for (int i=0; i<6; i++) {
            data.addEntry(new BaseEntry());
        }
        
        data.addAttribute("x", new double[]{0,1,1,2,2,3});
        
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(1).setMeasuredClass(1);
        data.getEntry(2).setMeasuredClass(2);
        data.getEntry(3).setMeasuredClass(3);
        Dataset originalData = data.clone();
        
        // Resolve it
        RankingDuplicateResolver res = new RankingDuplicateResolver();
        List<Object> options = new LinkedList<>();
        options.add("maximize");
        options.add("SimpleEntryRanker");
        
        res.setOptions(options);
        
        // Run it
        res.modifyDataset(data);
        
        assertEquals(4, data.NEntries());
        int entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(1));
        assertEquals(2, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(3));
        assertEquals(3, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(5));
        assertFalse(data.getEntry(entryID).hasMeasurement());
    }
    
    @Test
    public void testSimpleMinimize() throws Exception {
        // Make a dataset
        Dataset data = new Dataset();
        for (int i=0; i<6; i++) {
            data.addEntry(new BaseEntry());
        }
        
        data.addAttribute("x", new double[]{0,1,1,2,2,2});
        
        data.getEntry(0).setMeasuredClass(0);
        data.getEntry(1).setMeasuredClass(1);
        data.getEntry(2).setMeasuredClass(2);
        data.getEntry(3).setMeasuredClass(3);
        data.getEntry(3).setMeasuredClass(3);
        Dataset originalData = data.clone();
        
        // Resolve it
        RankingDuplicateResolver res = new RankingDuplicateResolver();
        List<Object> options = new LinkedList<>();
        options.add("minimize");
        options.add("SimpleEntryRanker");
        
        res.setOptions(options);
        
        // Run it
        res.modifyDataset(data);
        
        assertEquals(3, data.NEntries());
        int entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(0, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(1));
        assertEquals(1, data.getEntry(entryID).getMeasuredClass(), 1e-6);
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(3));
        assertEquals(3, data.getEntry(entryID).getMeasuredClass(), 1e-6);
    }
 
    
    @Test
    public void testGroundState() throws Exception {
        // Make a dataset
        CompositionDataset data = new CompositionDataset();
        data.addProperty("energy_pa");
        data.addProperty("bandgap");
        
        data.addEntry("NaCl");
        data.addEntry("NaCl");
        data.addEntry("FeCl2");

        // Set the delta_e property
        data.setTargetProperty(0, true);
        data.setMeasuredClasses(new double[]{-1,0,-2});
        
        // Set the bandgap property
        data.setTargetProperty(1, true);
        data.setMeasuredClasses(new double[]{5,0,-3});
        
        data.setTargetProperty(-1, true);
        Dataset originalData = data.clone();
        
        // Resolve it
        RankingDuplicateResolver res = new RankingDuplicateResolver();
        List<Object> options = new LinkedList<>();
        options.add("minimize");
        options.add("PropertyRanker");
        options.add("energy_pa");
        options.add("SimpleEntryRanker");
        
        res.setOptions(options);
        
        // Run it
        res.modifyDataset(data);
        
        assertEquals(2, data.NEntries());
        int entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(0));
        assertEquals(-1, data.getEntry(entryID).getMeasuredProperty(0), 1e-6);
        assertEquals(5, data.getEntry(entryID).getMeasuredProperty(1), 1e-6);
        
        entryID = data.getEntriesWriteAccess().indexOf(originalData.getEntry(2));
        assertEquals(-2, data.getEntry(entryID).getMeasuredProperty(0), 1e-6);
        assertEquals(-3, data.getEntry(entryID).getMeasuredProperty(1), 1e-6);
        
    }
}
