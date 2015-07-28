package magpie.data.utilities.filters;

import java.util.List;
import magpie.data.Dataset;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Filter based on predicted probability that an entry is a certain class.
 * 
 * <p>Example use: Filtering out all compounds more than 50% likely to be metallic.
 * 
 * 
 * <usage><p><b>Usage</b>: &lt;class&gt; &lt;min probability&gt;
 * <br><pr><i>class</i>: Name of class used for filtering
 * <br><pr><i>min probability</i>: Probability threshold. Any entry with a 
 * predicted probability below this threshold is filted.</usage>
 * 
 * @author Logan Ward
 */
public class ClassProbabilityFilter extends BaseDatasetFilter {
    /** Name of class used for filtering */
    private String ClassName = null;
    /** Probability threshold */
    private double Threshold = 0.5;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String printUsage() {
        return "Usage: <class name> <probability threshold>";
    }

    /**
     * Set the name of the class considered for filtering.
     * @param name Name of target class
     */
    public void setClassName(String name) {
        this.ClassName = name;
    }

    /**
     * Set the probability threshold used for filtering. Any entry with a predicted
     * probability less than this value is filtered.
     * @param threshold Desired threshold. Must be between 0 and 1.
     * @throws Exception 
     */
    public void setThreshold(double threshold) throws Exception {
        if (threshold < 0 || threshold > 1) {
            throw new Exception("Threshold must be between 0 and 1");
        }
        Threshold = threshold;
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Nothing to do
    }

    @Override
    protected boolean[] label(Dataset D) {
        if (D.NClasses() == 1) {
            throw new Error("Data must have multiple classes");
        }
        
        // Get ID of this class
        int classIndex = ArrayUtils.indexOf(D.getClassNames(), ClassName);
        if (classIndex == ArrayUtils.INDEX_NOT_FOUND) {
            throw new Error("Data does not contain class: " + ClassName);
        }
        
        // Perform labelling
        boolean[] output = new boolean[D.NEntries()];
        for (int e=0; e<D.NEntries(); e++) {
            output[e] = D.getEntry(e).getClassProbilities()[classIndex] > Threshold;
        }
        return output;
    }
    
}
