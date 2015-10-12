package magpie.data.utilities.splitters;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MultipleElementGroupsSplitterTest extends BaseDatasetSplitterTest {

    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        MultipleElementGroupsSplitter spltr = new MultipleElementGroupsSplitter();
        spltr.addElementGroup("Al Ni");
        spltr.addElementGroup("Fe");
        return spltr;
    }
    
    
    
}
