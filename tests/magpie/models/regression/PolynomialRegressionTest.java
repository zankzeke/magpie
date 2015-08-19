package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PolynomialRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        PolynomialRegression model = new PolynomialRegression();
        model.setOrder(2);
        return model;
    }
    
}
