package magpie.data.utilities.splitters;

import magpie.cluster.WekaClusterer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ClustererSplitterTest extends BaseDatasetSplitterTest {

    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        ClustererSplitter spltr = new ClustererSplitter();
        spltr.setClusterer(new WekaClusterer());
        return spltr;
    }
    
}
