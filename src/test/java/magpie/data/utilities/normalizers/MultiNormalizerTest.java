package magpie.data.utilities.normalizers;

import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MultiNormalizerTest {

    @Test
    public void test() throws Exception {
        Dataset data = new Dataset();
        data.importText("datasets/simple-data.csv", null);
        
        MultiNormalizer multi = new MultiNormalizer();
        multi.addNormalizer(new RescalingNormalizer());
        multi.addNormalizer(new ZScoreNormalizer());
        
        assertTrue(multi.test(data));
    }
    
}
