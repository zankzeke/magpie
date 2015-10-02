package magpie.data.utilities.splitters;

import magpie.data.materials.CompositionDataset;
import magpie.models.classification.WekaClassifier;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PredictedClassIntervalSplitterTest extends BaseDatasetSplitterTest {
    
    @Override
    protected BaseDatasetSplitter getSplitter() throws Exception {
        PredictedClassIntervalSplitter spltr = new PredictedClassIntervalSplitter();
        spltr.setClassifier(new WekaClassifier("trees.REPTree", null));
        spltr.setEdges(new double[]{0,1.5,2.0});
        return spltr;
    }

    @Override
    protected CompositionDataset getDataset() throws Exception {
        CompositionDataset data = super.getDataset(); 
        data.setTargetProperty("bandgap", true);
        return data;
    }
    
}
