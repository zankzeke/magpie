package magpie.attributes.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.modifiers.NonZeroClassModifier;
import magpie.data.utilities.splitters.AllMetalsSplitter;
import magpie.models.SplitModel;
import magpie.models.classification.WekaClassifier;
import magpie.models.regression.SplitRegression;
import magpie.models.regression.WekaRegression;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class ModelPredictionAttributeGeneratorTest {

    @Test
    public void testRegression() throws Exception {
        // Create a regression model and the dataset to compute attributes
        SplitModel model = new SplitRegression();
        model.setPartitioner(new AllMetalsSplitter());
        model.setGenericModel(new WekaRegression());
        
        CompositionDataset data = new CompositionDataset();
        data.addElementalProperty("Electronegativity");
        
        // Train this model
        data.importText("datasets/small_set.txt", null);
        data.generateAttributes();
        data.setTargetProperty("delta_e", true);
        
        model.train(data);
        
        // Create generator
        ModelPredictionAttributeGenerator gen = new ModelPredictionAttributeGenerator();
        gen.setModel("delta_e", model, data);
        
        // Add attributes
        int oldNAttr = data.NAttributes();
        gen.addAttributes(data);
        
        // Test results
        assertEquals(oldNAttr + 1, data.NAttributes());
        int newAttrNum = oldNAttr;
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(entry.getAttribute(newAttrNum), 
                    entry.getPredictedClass(), 1e-6);
        }
        
        // Print settings
        System.out.println(gen.printDescription(true));
    }
         
    @Test
    public void testClassification() throws Exception {
        // Create a classification model and the dataset to compute attributes
        WekaClassifier model = new WekaClassifier();
        
        CompositionDataset data = new CompositionDataset();
        data.addElementalProperty("Electronegativity");
        
        // Make this a metal/nonmetal problem
        data.importText("datasets/small_set.txt", null);
        data.generateAttributes();
        data.setTargetProperty("bandgap", false);
        
        NonZeroClassModifier mdfr = new NonZeroClassModifier();
        mdfr.transform(data);
        
        // Train this model
        model.train(data);
        
        // Create generator
        ModelPredictionAttributeGenerator gen = new ModelPredictionAttributeGenerator();
        gen.setModel("delta_e", model, data);
        
        // Add attributes
        int oldNAttr = data.NAttributes();
        gen.addAttributes(data);
        
        // Test results
        assertEquals(oldNAttr + 3, data.NAttributes());
        int newAttrNum = oldNAttr;
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(entry.getAttribute(newAttrNum), 
                    entry.getPredictedClass(), 1e-6);
        }
        
        // Print settings
        System.out.println(gen.printDescription(true));
    }

    @Test
    public void testOptions() throws Exception {
        // Create a classification model and the dataset to compute attributes
        WekaClassifier model = new WekaClassifier();
        
        CompositionDataset data = new CompositionDataset();
        data.addElementalProperty("Electronegativity");

        // Make this a metal/nonmetal problem
        data.importText("datasets/small_set.txt", null);
        data.generateAttributes();
        data.setTargetProperty("bandgap", false);
        
        NonZeroClassModifier mdfr = new NonZeroClassModifier();
        mdfr.transform(data);
        
        // Train this model
        model.train(data);

        // Assemble into options list
        List<Object> options = new ArrayList<>();
        options.add("test");
        options.add(model);
        options.add(data);

        // Set options
        ModelPredictionAttributeGenerator gen = new ModelPredictionAttributeGenerator();
        gen.setOptions(options);

        // Test usage
        assertEquals("Usage: <name> $<model> $<data>", gen.printUsage());
    }
}
