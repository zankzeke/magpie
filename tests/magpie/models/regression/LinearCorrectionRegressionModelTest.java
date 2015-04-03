
package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class LinearCorrectionRegressionModelTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        BaseModel sub = new WekaRegression();
        LinearCorrectionRegressionModel model = new LinearCorrectionRegressionModel();
        try {
            model.setSubmodel(sub);
        } catch (Exception e) {
            throw new Error(e);
        }
        return model;
    }
    
}
