package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositeRegressionTest extends BaseModelTest {
    @Override
    public BaseModel generateModel() {
        CompositeRegression model = new CompositeRegression();
        
        LASSORegression submodel = new LASSORegression();
        submodel.setMaxNumberTerms(1);
        
        model.setModel(0, submodel);
        model.setModel(1, submodel);
        return model;
    }
}
