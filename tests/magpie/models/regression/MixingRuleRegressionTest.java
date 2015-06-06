package magpie.models.regression;

import java.util.Arrays;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MixingRuleRegressionTest extends BaseModelTest {

    public MixingRuleRegressionTest() {
        DataType = new CompositionDataset();
    }

    @Override
    public Dataset getData() throws Exception {
        CompositionDataset data = (CompositionDataset) super.getData(); 
        data.setTargetProperty("volume_pa", false);
        return data;
    }
    
    @Override
    public BaseModel generateModel() {
        MixingRuleRegression model = new MixingRuleRegression();
        model.setPropertyName("ICSDVolume");
        model.setFittedElements(Arrays.asList(new String[]{"Fe"}));
        return model;
    }
    
}
