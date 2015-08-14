package magpie.cluster;

import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class WekaClustererTest {

    @Test
    public void test() throws Exception {
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.txt", null);
        
        WekaClusterer clstr = new WekaClusterer();
        clstr.train(data);
        assertTrue(clstr.isTrained());
        clstr.label(data);
        assertTrue(clstr.NClusters() > 1);
    }
    
}
