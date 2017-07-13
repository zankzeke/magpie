package magpie.data.utilities.splitters;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.utilities.modifiers.NonZeroClassModifier;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;
import magpie.models.classification.WekaClassifier;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Split data based on whether an entry is likely to have a non-zero class variable.
 * If the probability of the entry having a class variable equal to zero is past 
 * a certain threshold, it is assigned to subset #0. 
 * 
 * <usage><p><b>Usage</b>: &lt;threshold> $&lt;model>
 * <br><pr><i>threshold</i>: Probability of having a class == 0 on which to split
 * <br><pr><i>model</i>: {@linkplain AbstractClassifier} to use for partitioning data.
 * Must distinguish between two classes</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class NonZeroClassificationSplitter extends BaseDatasetSplitter {
    /** Model to use for splitting */
    protected BaseModel Clfr;
    /** Probability of being of class #0 below which entry gets placed in entry #1 */
    protected double ProbabilityThreshold = 0.5;

    /**
     * Create a new instances that uses ZeroR to classify.
     * @throws Exception 
     */
    public NonZeroClassificationSplitter() throws Exception {
        this.Clfr = new WekaClassifier();
    }
    
    @Override
    public NonZeroClassificationSplitter clone() {
        NonZeroClassificationSplitter x = (NonZeroClassificationSplitter) super.clone();
        x.Clfr = this.Clfr.clone();
        return x;
    }
    
    @Override
    public void setOptions(List Options) throws Exception {
        try { 
            setProbabilityThreshold(Double.parseDouble(Options.get(0).toString()));
            setModel((BaseModel) Options.get(1));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <threshold> $<model>";
    }

    /**
     * Define the probability of being in class 0 on which to partition data.
     * @param ProbabilityThreshold Desired threshold
     */
    public void setProbabilityThreshold(double ProbabilityThreshold) {
        this.ProbabilityThreshold = ProbabilityThreshold;
    }
    
    /**
     * Define the model used to split data. Should implement {@linkplain AbstractClassifier}
     * @param model Desired model (will be cloned)
     * @throws Exception
     */
    public void setModel(BaseModel model) throws Exception {
        if (! (model instanceof AbstractClassifier)) {
            throw new Exception("Model is not a classifier");
        }
        Clfr = model.clone();
    }
    
    @Override
    public int[] label(Dataset D) {
        int[] output = new int[D.NEntries()];
        Dataset Copy = D.clone();
        new NonZeroClassModifier().transform(Copy);
        Clfr.run(Copy);
        Iterator<BaseEntry> iter = Copy.getEntries().iterator();
        int i=0;
        double prob;
        AbstractClassifier ptr = (AbstractClassifier) Clfr;
        while (iter.hasNext()) {
            BaseEntry entry = iter.next();
            prob = entry.getClassProbilities()[0];
            output[i] = prob > ProbabilityThreshold ? 0 : 1;
            i++;
        }
        return output;
    }
    
    /**
     * Train a classifier for determining which entries have a non-zero class
     * @param TrainingSet 
     */
    @Override
    public void train(Dataset TrainingSet) {
        Dataset Copy = TrainingSet.clone();
        new NonZeroClassModifier().transform(Copy);
        Clfr.train(Copy);
    }

    @Override
    protected List<String> getSplitterDetails(boolean htmlFormat) {
        List<String> output = new LinkedList<>();
        
        // Get classifier details
        String[] submodelDetails = Clfr.printDescription(htmlFormat).split("\n");
        submodelDetails[0] = "Classifier: " + submodelDetails[0];
        for (String line : submodelDetails) {
            output.add(line);
        }
        
        return output;
    }

    @Override
    public List<String> getSplitNames() {
        List<String> output = new LinkedList<>();
        
        output.add("P(Class = 0) > 50%");
        output.add("P(Class = 0) < 50%");
        
        return output;
    }
    
}
