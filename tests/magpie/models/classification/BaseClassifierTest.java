package magpie.models.classification;

import magpie.data.Dataset;
import magpie.data.utilities.modifiers.ClassIntervalModifier;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;

/**
 *
 * @author Logan Ward
 */
public class BaseClassifierTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        return new WekaClassifier();
    }
    
    @Override
    public Dataset getData() throws Exception {
        Dataset data = super.getData();
        ClassIntervalModifier mdfr = new ClassIntervalModifier();
        mdfr.setEdges(new double[]{0});
        mdfr.transform(data);
        return data;
    }
    
}
