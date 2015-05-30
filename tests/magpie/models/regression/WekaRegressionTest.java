package magpie.models.regression;

import magpie.models.BaseModelTest;
import magpie.models.BaseModel;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class WekaRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        try {
            return new WekaRegression();
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
