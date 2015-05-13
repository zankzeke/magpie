package magpie.models.classification;

import magpie.models.BaseModel;

/**
 *
 * @author Logan Ward
 */
public class WekaClassifierTest extends BaseClassifierTest {

    @Override
    public BaseModel generateModel() {
        try {
            return new WekaClassifier();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
}
