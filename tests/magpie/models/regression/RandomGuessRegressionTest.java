package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class RandomGuessRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        RandomGuessRegression model = new RandomGuessRegression();
        return model;
    }

    // This model is non-deterministic. Cloning doesn't matter
    @Override
    public void testClone() throws Exception {}
    
}
