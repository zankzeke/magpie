package magpie.models.classification;

import java.util.List;
import magpie.models.BaseModel;
import magpie.models.regression.WekaRegression;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

/**
 *
 * @author Logan Ward
 */
public class WekaClassifierTest extends BaseClassifierTest {

    @Override
    public BaseModel generateModel() {
        try {
            return new WekaClassifier();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    @Test
    public void testCitations() throws Exception {
        // Make a RF model
        WekaRegression o = new WekaRegression("trees.RandomForest", null);
        
        // Get the citation and print to make sure it looks right
        List<Pair<String,Citation>> cite = o.getCitations();
        
        // Print out it to screen
        for (Pair<String,Citation> c : cite) {
            System.out.println(c.getKey());
            System.out.println(c.getValue().printInformation());
        }
    }
}
