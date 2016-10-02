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
    

    
    
}
