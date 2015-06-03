package magpie.models.regression;

import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import magpie.models.classification.WekaClassifier;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ClassificationRegressionTest extends BaseModelTest {

    @Override
    public BaseModel generateModel() {
        try {
            ClassificationRegression model = new ClassificationRegression();
            model.setThreshold(0.5);
            model.setClassifier(new WekaClassifier("trees.REPTree", null));
            return model;
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
}
