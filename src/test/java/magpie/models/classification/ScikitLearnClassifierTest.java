package magpie.models.classification;

import java.io.FileInputStream;
import magpie.models.BaseModel;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ScikitLearnClassifierTest extends BaseClassifierTest {

    @Override
    public BaseModel generateModel() {
        ScikitLearnClassifier model = new ScikitLearnClassifier();
        try {
            model.readModel(new FileInputStream("test-files/sklearn-gbc.pkl"));
            model.setCompressionLevel(1);
            model.Debug = true;
        } catch (Exception e) {
            throw new Error(e);
        }
        return model;
    }

    @Test
    public void testModelTypeMismatch() throws Exception {
        boolean failed = false;

        // Load in a classifier
        ScikitLearnClassifier model = new ScikitLearnClassifier();
        model.readModel(new FileInputStream("test-files/sklearn-linreg.pkl"));

        // Try to run the model
        try {
            model.train(getData());
        } catch (Exception e) {
            failed = true;
        }

        assertTrue(failed);
    }
    
    
}
