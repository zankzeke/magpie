package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class StagedRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        StagedRegression model = new StagedRegression();
        model.useAbsoluteError();
        model.setModel(0, new LASSORegression());
        model.setGenericModel(new PolynomialRegression());
        model.setNumberOfModels(3);
        return model;
    }
    
}
