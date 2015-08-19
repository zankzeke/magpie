
package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class NonlinearRegressionExprTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        try {
            NonlinearRegressionExpr model = new NonlinearRegressionExpr();
            model.parseFormula("#{x} + #{y}");
            return model;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
}
