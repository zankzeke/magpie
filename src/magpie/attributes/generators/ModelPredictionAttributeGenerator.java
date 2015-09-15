package magpie.attributes.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;

/**
 * Use the result from another model as an attribute. User must provide model
 * and dataset used to compute attributes for model.
 * 
 * <p>For regression, the predicted class value will be used as an attribute
 * 
 * <p>For classification, the predicted class and probability of an entry being each class
 * known to the model will be used as attributes. Example: P(Yes)>50%, P(Yes), and P(No) for 
 * a binary classification model.
 * 
 * <usage><p><b>Usage</b>: &lt;name&gt; $&lt;model&gt; $&lt;dataset&gt;
 * <pr><br><i>name</i>: Name of model (used to name attributes)
 * <pr><br><i>model</i>: Model used to create attributes
 * <pr><br><i>dataset</i>: Dataset used to compute attributes for model</usage>
 * 
 * @author Logan Ward
 */
public class ModelPredictionAttributeGenerator extends BaseAttributeGenerator {
    /** Name of model. Used to create unique names for attributes */
    private String ModelName;
    /** Model to evaluate */
    private BaseModel Model;
    /** Dataset used to compute attributes for {@linkplain #Model}. */
    private Dataset ModelData;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // Read input
        String name;
        Dataset data;
        BaseModel model;
        try {
            name = Options.get(0).toString();
            model = (BaseModel) Options.get(1);
            data = (Dataset) Options.get(2);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }

        // Define settings
        setModel(name, model, data);
    }

    @Override
    public String printUsage() {
        return "Usage: <name> $<model> $<data>";
    }
    
    /**
     * Set the model used to create attribute(s). Define both the model
     * and dataset used to create attributes.
     * @param name Name of model. Used to create unique attribute names.
     * @param model Model to evaluate. Will be cloned
     * @param data Dataset used to generate attributes for that model. Will be cloned
     * @throws Exception If model not trained
     */
    public void setModel(String name, BaseModel model, Dataset data) throws Exception {
        if (! model.isTrained()) {
            throw new Exception("Model mustbe trained first");
        }
        this.ModelName = name;
        this.Model = model.clone();
        this.ModelData = data.emptyClone();
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Determine if underlying model is a classifier
        boolean isClassifier = Model instanceof AbstractClassifier;
        AbstractClassifier clfr = null;
        if (isClassifier) {
            clfr = (AbstractClassifier) Model;
        }
        
        // Determine number of attributes
        int nAttr = isClassifier ? 1 + clfr.getNClasses() : 1;
        
        // Create class names
        List<String> newNames = new ArrayList<>(nAttr);
        newNames.add("Predicted" + ModelName);
        if (isClassifier) {
            for (String cls : clfr.getClassNames()) {
                newNames.add("Predicted" + ModelName + ":P(" + cls + ")");
            }
        }
        data.addAttributes(newNames);
        
        // Iterate through entries in batches of 1000 to reduce memeory requirements
        //  for evaluating model
        Iterator<BaseEntry> iter = data.getEntries().iterator();
        double[] attrs = new double[nAttr]; // To avoid re-making this every loop
        while (iter.hasNext()) {
            // Clear out old results
            ModelData.clearData();
            
            // Get a sublist of entries
            List<BaseEntry> sublist = new ArrayList(1000);
            int counter = 0;
            while (counter < 1000 && iter.hasNext()) {
                counter++;
                sublist.add(iter.next());
            }
            
            // Clone them, delete old attributes
            //  Cloning ensures the input dataset is not affected.
            for (BaseEntry entry : sublist) {
                BaseEntry clone = entry.clone();
                clone.clearAttributes();
                ModelData.addEntry(clone);
            }
            
            // Generate attributes and run model
            ModelData.generateAttributes();
            Model.run(ModelData);
            
            // Store results
            for (int e=0; e<sublist.size(); e++) {
                BaseEntry original = sublist.get(e);
                BaseEntry clone = ModelData.getEntry(e);
                
                // Get predicted class variable
                attrs[0] = clone.getPredictedClass();
                
                // If classifier, get probabilities
                if (isClassifier) {
                    double[] probs = clone.getClassProbilities();
                    for (int cl=1; cl<nAttr; cl++) {
                        attrs[cl] = probs[cl - 1];
                    }
                }
                
                // Store these attributes in original entry
                original.addAttributes(attrs);
            }
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Get number of attributes
        int nAttr = 1;
        if (Model instanceof AbstractClassifier) {
            AbstractClassifier clfr = (AbstractClassifier) Model;
            nAttr += clfr.getNClasses();
        }
        
        // Create description
        output += " (" + nAttr + ") Predicted class value ";
        if (Model instanceof AbstractClassifier) {
            output += "and class probabilities ";
        }
        output += "from another model. Model name: " + ModelName;
        
        return output;
    }
    
}
