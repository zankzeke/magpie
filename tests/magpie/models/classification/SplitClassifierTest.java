package magpie.models.classification;

import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.splitters.AllMetalsSplitter;
import magpie.models.BaseModel;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class SplitClassifierTest extends BaseClassifierTest{

    public SplitClassifierTest() {
        DataType = new CompositionDataset();
    }

    @Override
    public BaseModel generateModel() {
        try {
            SplitClassifier clfr = new SplitClassifier();
            
            clfr.setPartitioner(new AllMetalsSplitter());
            clfr.setGenericModel(new WekaClassifier("trees.REPTree", null));
            
            return clfr;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
}
