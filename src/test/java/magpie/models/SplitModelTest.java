package magpie.models;

import magpie.cluster.WekaClusterer;
import magpie.data.utilities.splitters.ClustererSplitter;
import magpie.models.regression.GuessMeanRegression;
import magpie.models.regression.SplitRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class SplitModelTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        SplitRegression model = new SplitRegression();
        model.setGenericModel(new GuessMeanRegression());
        ClustererSplitter spltr = new ClustererSplitter();
        spltr.setClusterer(new WekaClusterer());
        model.setPartitioner(spltr);
        return model;
    }
        
}
