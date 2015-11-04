package magpie.models.regression;

import java.util.List;
import magpie.models.BaseModelTest;
import magpie.models.BaseModel;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class WekaRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        try {
            return new WekaRegression();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    @Test
    public void testCitations() throws Exception {
        // Make a M5P model
        WekaRegression o = new WekaRegression("trees.M5P", null);
        
        // Get the citation and print to make sure it looks right
        List<Pair<String,Citation>> cite = o.getCitations();
        
        // Print out it to screen
        for (Pair<String,Citation> c : cite) {
            System.out.println(c.getKey());
            System.out.println(c.getValue().printInformation());
        }
    }
}
