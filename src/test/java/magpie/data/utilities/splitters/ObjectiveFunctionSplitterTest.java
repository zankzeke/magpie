package magpie.data.utilities.splitters;

import magpie.optimization.rankers.BaseEntryRanker;
import magpie.optimization.rankers.SimpleEntryRanker;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ObjectiveFunctionSplitterTest extends BaseDatasetSplitterTest {

    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        ObjectiveFunctionSplitter spltr = new ObjectiveFunctionSplitter();
        spltr.setObjectiveFunction(new SimpleEntryRanker());
        spltr.setThreshold(0.0);
        return spltr;
    }
    
}
