package magpie.models.regression;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.models.BaseModel;
import magpie.models.BaseModelTest;
import magpie.models.classification.WekaClassifier;
import magpie.optimization.rankers.PropertyFormulaRanker;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class MultiObjectiveRegressionTest extends BaseModelTest {
    PropertyFormulaRanker obj;

    public MultiObjectiveRegressionTest() throws Exception {
        DataType = new CompositionDataset();
        
        obj = new PropertyFormulaRanker();
        obj.setFormula("#{delta_e} / #{volume_pa}");
    }

    @Override
    public BaseModel generateModel() {
        MultiObjectiveRegression model = new MultiObjectiveRegression();
       
        try {
            model.setObjectiveFunction(obj);
       
            // Set models
            model.setModel("delta_e", new WekaRegression("trees.REPTree", null));
            model.setModel("volume_pa", new WekaRegression());
        } catch (Exception e) {
            throw new Error(e);
        }
        
        return model;
    }

    @Override
    public Dataset getData() throws Exception {
        CompositionDataset data = (CompositionDataset) super.getData(); 
        
        // Filter out data that don't have the properties
        data.setTargetProperty("delta_e", false);
        data.setTargetProperty("volume_pa", false);
        
        // Compute class variable
        data.setTargetProperty(-1, true);
        obj.setUseMeasured(true);
        obj.train(data);
        for (BaseEntry e : data.getEntries()) {
            e.setMeasuredClass(obj.objectiveFunction(e));
        }
        
        return data;
    }
    
    
    
    
}
