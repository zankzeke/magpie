package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class LASSORegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        LASSORegression model = new LASSORegression();
        model.setMaxNumberTerms(1);
        return model;
    }
    
}
