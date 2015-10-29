package magpie.data.utilities.splitters;

import magpie.data.materials.CompositionDataset;
import magpie.models.classification.WekaClassifier;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class NonZeroClassificationSplitterTest extends BaseDatasetSplitterTest {

    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        NonZeroClassificationSplitter spltr = new NonZeroClassificationSplitter();
        spltr.setModel(new WekaClassifier("trees.REPTree", null));
        return spltr;
    }

    @Override
    protected CompositionDataset getDataset() throws Exception {
        CompositionDataset data = (CompositionDataset) super.getDataset(); 
        data.setTargetProperty("bandgap", true);
        return data;
    }    

}
