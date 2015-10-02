package magpie.models.classification;

import magpie.data.materials.CompositionDataset;
import magpie.data.materials.PrototypeDataset;
import magpie.models.BaseModel;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CumulantExpansionClassifierTest extends BaseClassifierTest {

    public CumulantExpansionClassifierTest() {
        DataType = new PrototypeDataset();
    }

    @Override
    public BaseModel generateModel() {
        CumulantExpansionClassifier clfr = new CumulantExpansionClassifier();
        clfr.defineKnownCompounds("datasets/prototypes.list");
        return clfr;
    }
    
}
