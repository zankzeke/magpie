package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;

/**
 *
 * @author Logan Ward
 */
public class ScikitLearnRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        ScikitLearnRegression model = new ScikitLearnRegression();
        try {
            model.readModel("test-files/sklearn-linreg.pkl");
        } catch (Exception e) {
            throw new Error(e);
        }
        return model;
    }
    
}
