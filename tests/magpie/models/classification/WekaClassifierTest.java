package magpie.models.classification;

import magpie.models.BaseModel;

/**
 *
 * @author Logan Ward
 */
public class WekaClassifierTest extends BaseClassifierTest {

    @Override
    public BaseModel generateModel() {
        return new WekaClassifier();
    }
    
}
