package magpie.data.utilities.splitters;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class SingleElementGroupSplitterTest extends BaseDatasetSplitterTest {

    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        SingleElementGroupSplitter spltr = new SingleElementGroupSplitter();
        spltr.setElements(Arrays.asList(new String[]{"O"}));
        return spltr;
    }
    
}
