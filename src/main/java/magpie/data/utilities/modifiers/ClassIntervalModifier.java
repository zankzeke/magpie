package magpie.data.utilities.modifiers;

import java.util.Arrays;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;

/**
 * Transforms a Dataset with a continuous class variable into a dataset with multiple 
 * class based on the value of the class variable. User must define bins that define
 * the new classes, follows the following procedure.
 * 
 * <p>Class &le; edge0 -> Class = 0
 * <br>Class &gt; edge0 && Class &le; edge1 -> Class = 1
 * <br>...
 * <br>Class &gt; edgeN -> Class = N
 *
 * <p>For MultiPropertyDatasets, adds a new property and sets it as the target class.</p>
 * 
 * <usage><p><b>Usage</b>: &lt;interval edges...&gt;
 * <br><pr><i>interval edges...</i>: Values that define edges of class variable bins.
 * </usage>
 * 
 * @author Logan Ward
 * @version 0.1
 * @see MultiPropertyDataset
 */
public class ClassIntervalModifier extends BaseDatasetModifier {
    /** Interval Edges */
    private double[] Edges = new double[]{0};

    /**
     * Set the Edges that define the class intervals on which data is discretized.
     * @param edges Edges of bins
     */
    public void setEdges(double[] edges) {
        if (edges.length == 0) {
            throw new Error("At least one edge must be defined.");
        }
        this.Edges = edges.clone();
        Arrays.sort(this.Edges);
    }
    
    /**
     * Get number of intervals on which data is split.
     * @return Number of intervals
     */
    public int NBins() {
        return Edges.length + 1;
    }
    
    @Override
    protected void modifyDataset(Dataset Data) {
        // Add property to each entry
        for (BaseEntry entry : Data.getEntries()) {
            double value = Double.NaN;
            
            // Get the bin
            if (entry.hasMeasurement()) {
                value = getBin(entry.getMeasuredClass());
            } 
            
            // Add it to the dataset
            if (entry instanceof MultiPropertyEntry) {
                // Add property
                MultiPropertyEntry Ptr = (MultiPropertyEntry) entry;
                if (entry.hasMeasurement()) {
                    if (Ptr.getTargetProperty() != -1)
                        Ptr.addProperty(value);
                    else {
                        entry.setMeasuredClass(value);
                    }
                } else {
                    if (Ptr.getTargetProperty() != -1) {
                        Ptr.addProperty();
                    }
                }
            } else {
                // Just set it
                if (entry.hasMeasurement()) {
                    entry.setMeasuredClass(value);
                }
            }
        }
		
        // Add property to dataset if MultiPropertyDataset
        if (Data instanceof MultiPropertyDataset) {
            MultiPropertyDataset Ptr = (MultiPropertyDataset) Data;
            if (Ptr.getTargetPropertyIndex() != -1) {
                Ptr.addProperty(Ptr.getTargetPropertyName() + "Bin");
                Ptr.setTargetProperty(Ptr.getTargetPropertyName() + "Bin", true);
            }
        }
        
        // Define new class names
        
        String[] ClassNames = new String[NBins()];
        for (int i=0; i<NBins(); i++) {
            ClassNames[i] = "Bin" + i;
        }
        Data.setClassNames(ClassNames);
    }
    
    /**
     * Calculate new class variable by deciding which interval a class variable 
     *  falls into.
     * @param x Value of class variable for an entry
     * @return New class variable
     */
    private double getBin(double x) {
        for (int i=0; i<Edges.length; i++) {
            if (x <= Edges[i]) return i;
        }
        return Edges.length;
    }

    @Override
    public void setOptions(List Options) throws Exception {
        double[] edges = new double[Options.size()];
        try {
            for (int i=0; i<Options.size(); i++) {
                edges[i] = Double.parseDouble(Options.get(i).toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setEdges(edges);
    }

    @Override
    public String printUsage() {
        return "Usage: <edges...>";
    }
    
}
