package magpie.models;

import magpie.cluster.WekaClusterer;
import magpie.data.Dataset;
import magpie.data.utilities.splitters.ClustererSplitter;
import magpie.models.regression.SplitRegression;
import magpie.models.regression.WekaRegression;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

/**
 *
 * @author Logan Ward
 */
public class SplitModelTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        try {
            SplitRegression model = new SplitRegression();
            model.setGenericModel(new WekaRegression("trees.M5P", null));
            ClustererSplitter spltr = new ClustererSplitter();
            spltr.setClusterer(new WekaClusterer());
            model.setPartitioner(spltr);
            return model;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCitations() throws Exception {
        BaseModel model = generateModel();
        Dataset dataset = getData();
        model.train(dataset, true);
        for (Pair<String, Citation> citation : model.getCitations()) {
            System.out.println(citation);
        }

    }
}
