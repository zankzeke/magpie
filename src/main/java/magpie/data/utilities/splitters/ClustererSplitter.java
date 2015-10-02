package magpie.data.utilities.splitters;

import java.util.LinkedList;
import java.util.List;
import magpie.cluster.BaseClusterer;
import magpie.data.Dataset;

/**
 * Split data based using unsupervised learning. Uses a {@link BaseClusterer} to label
 * each entry's membership in a certain cluster. User can either provide an untrained
 * clusterer and have the splitter train it, or provide a trained clusterer and use as-is.
 * 
 * <usage><p><b>Usage</b>: $&lt;clusterer>
 * <br><pr><i>clusterer</i>: Data will be spit based on clusters produced by this {@linkplain BaseClusterer}</usage>
 * 
 * @author Logan Ward
 * @version 1.0
 * @see BaseClusterer
 */
public class ClustererSplitter extends BaseDatasetSplitter {
    /** Clusterer used to perform splitting */
    private BaseClusterer Clusterer = null;
    /** If it was provided as a trained clusterer */
    private boolean trainedPreviously = false;

    @Override
    public ClustererSplitter clone() {
        ClustererSplitter x = (ClustererSplitter) super.clone();
        x.Clusterer = Clusterer.clone();
        return x;
    }
    
    /** 
     * Set the clusterer used for splitting. If this Clusterer is already trained, it
     * will be used as provided. 
     * @param x Clusterer to be used for splitting
     */
    public void setClusterer(BaseClusterer x) {
        trainedPreviously = x.isTrained();
        this.Clusterer = x;
    }

    @Override
    public void setOptions(List Options) throws Exception {
        try {
            setClusterer((BaseClusterer) Options.get(0));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<clusterer>";
    }
    
    @Override
    public void train(Dataset TrainingSet) {
        if (! trainedPreviously)
            Clusterer.train(TrainingSet);
    }

    @Override
    public int[] label(Dataset D) {
        return Clusterer.label(D);
    }

    @Override
    public List<String> getSplitNames() {
        if (! Clusterer.isTrained()) {
            return new LinkedList<>();
        }
        
        // If trained
        List<String> output = new LinkedList<>();
        for (int i=0; i<Clusterer.NClusters(); i++) {
            output.add("Cluster #" + i);
        }
        
        return output;
    }

    @Override
    protected List<String> getSplitterDetails(boolean htmlFormat) {
        List<String> output = new LinkedList<>();
        
        // Get clusterer details
        String[] clustDetails = Clusterer.printDescription(htmlFormat).split("\n");
        clustDetails[0] = "Clusterer: " + clustDetails[0];
        for (String line : clustDetails) {
            output.add(line);
        }
        
        return output;
    }
    
    
}
