package magpie.data.utilities.splitters;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class AttributeIntervalSplitterTest extends BaseDatasetSplitterTest {

    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        AttributeIntervalSplitter spltr = new AttributeIntervalSplitter();
        spltr.setAttributeName("mean_Number");
        spltr.setBinEdges(new double[]{10,20,30});
        return spltr;
    }
    
}
