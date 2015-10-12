package magpie.utility.tools;

import java.util.List;
import magpie.data.materials.CompositionEntry;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class OxidationStateGuesserTest {

    @Test
    public void test() throws Exception {
        OxidationStateGuesser gsr = new OxidationStateGuesser();
        gsr.setOxidationStates("./Lookup Data/OxidationStates.table");
        gsr.setElectronegativity("./Lookup Data/Electronegativity.table");
        
        List<int[]> res = gsr.getPossibleStates(new CompositionEntry("NaCl"));
        assertEquals(1, res.size());
        assertArrayEquals(new int[]{1,-1}, res.get(0));
        
        res = gsr.getPossibleStates(new CompositionEntry("Fe2O3"));
        assertEquals(1, res.size());
        assertArrayEquals(new int[]{3,-2}, res.get(0));
        
        res = gsr.getPossibleStates(new CompositionEntry("NaHCO3"));
        assertEquals(1, res.size());
        assertArrayEquals(new int[]{10,0,5,7}, new CompositionEntry("NaHCO3").getElements());
        assertArrayEquals(new int[]{1,1,4,-2}, res.get(0));
        
        res = gsr.getPossibleStates(new CompositionEntry("NH3"));
        assertEquals(2, res.size());
        assertArrayEquals(new int[]{0,6}, new CompositionEntry("NH3").getElements());
        assertArrayEquals(new int[]{1,-3}, res.get(0));
        
        res = gsr.getPossibleStates(new CompositionEntry("NaAl"));
        assertEquals(0, res.size());
        
        res = gsr.getPossibleStates(new CompositionEntry("PbTiO3"));
        assertEquals(2, res.size());
        assertArrayEquals(new int[]{21,81,7}, new CompositionEntry("PbTiO3").getElements());
        assertArrayEquals(new int[]{4,2,-2}, res.get(0));
    }
    
}
