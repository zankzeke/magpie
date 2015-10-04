package magpie.models.regression;

import java.util.LinkedList;
import java.util.List;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PerceptronRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        PerceptronRegression model = new PerceptronRegression();
        List<Integer> layers = new LinkedList<>();
        layers.add(2);
        model.setHiddenLayers(layers);
        return model;
    }
    
}