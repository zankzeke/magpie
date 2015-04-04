
package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class LinearCorrectedRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        BaseModel sub = new WekaRegression();
        LinearCorrectedRegression model = new LinearCorrectedRegression();
        try {
            model.setSubmodel(sub);
        } catch (Exception e) {
            throw new Error(e);
        }
        return model;
    }
    
}
