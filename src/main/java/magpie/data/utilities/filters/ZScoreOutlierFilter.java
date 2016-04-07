package magpie.data.utilities.filters;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Remove entries that are outliers based on Z-score. Removes entries where 
 * attributes or measured class is above a certain number of standard deviations 
 * from the mean.
 * 
 * <p>Entries whose value for <i>any</i> property is more than a certain number
 * of standard deviations from the mean are flagged as outliers.
 * 
 * <p>If an attribute has fewer than 10 distinct values, it is classified as a
 * discrete variable and will be excluded from screening.
 * 
 * <usage><p><b>Usage</b>: &lt;tolerance&gt; [-class] [-attributes]
 * <pr><br><i>tolerance</i>: Number of standard deviations from the mean used
 * to classify outliers
 * <pr><br><i>-class</i>: Consider class values when determining outliers
 * <pr><br><i>-attributes</i>: Consider attribute value when computing outliers
 * </usage>
 * 
 * @author Logan Ward
 */
public class ZScoreOutlierFilter extends BaseDatasetFilter {
    /** Number of standard deviation away from mean used when defining outliers */
    protected double Tolerance = 5.0;
    /** Whether to screen based on attributes */
    protected boolean ScreenAttributes = true;
    /** Whether to screen based on class variable */
    protected boolean ScreenClass = true;
    /** Means of attributes */
    protected double[] AttributeMean;
    /** Standard deviation of attributes */
    protected double[] AttributeStdDev;
    /** Mean of class variable */
    protected double ClassMean;
    /** Standard deviation of class variable */
    protected double ClassStdDev;
    /** Whether this class has been trained */
    protected boolean Trained = false;
    /** 
     * Names of attributes. Used to check if attributes used to train filter
     * are the same as those during labeling.
     */
    protected String[] AttributeNames;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        boolean screenClass = false;
        boolean screenAttr = false;
        double tol;
        
        // Parse settings
        try {
            tol = Double.parseDouble(Options.get(0).toString());
            
            for (Object arg : Options.subList(1, Options.size())) {
                switch (arg.toString().toLowerCase()) {
                    case "-class": screenClass = true; break;
                    case "-attributes": screenAttr = true; break;
                    default: throw new Exception();
                }
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Enact settings
        setTolerance(tol);
        setScreenAttributes(screenAttr);
        setScreenClass(screenClass);
    }

    @Override
    public String printUsage() {
        return "Usage: <tolarance> [-class] [-attributes]";
    }

    /**
     * Set the tolerance defining what is an outlier.
     * @param tolerance Number of standard deviations from mean defining an outlier
     */
    public void setTolerance(double tolerance) {
        Trained = false;
        this.Tolerance = tolerance;
    }  

    /**
     * Set whether to screen based on whether any attribute value is an outlier.
     * @param screenAttributes Desired setting 
     */
    public void setScreenAttributes(boolean screenAttributes) {
        Trained = false;
        this.ScreenAttributes = screenAttributes;
    }
    
    /**
     * Set whether to screen based on whether measured class value is an outlier
     * @param screenClass Desired setting 
     */
    public void setScreenClass(boolean screenClass) {
        Trained = false;
        this.ScreenClass = screenClass;
    }

    @Override
    public void train(Dataset trainData) {
        // Mark as trained
        Trained = true;
        
        // Store attribute names
        AttributeNames = trainData.getAttributeNames();
        
        if (ScreenAttributes) {
            // Initialize storage arrays
            AttributeMean = new double[trainData.NAttributes()];
            AttributeStdDev = new double[trainData.NAttributes()];
            
            // For each attribute
            for (int a=0; a<trainData.NAttributes(); a++) {
                // Get values 
                double[] values = trainData.getSingleAttributeArray(a);
                
                // Determine if number of unique values greater than or equal to 4
                Set<Double> uniqueValues = new TreeSet<>();
                boolean isDiscrete = true;
                for (double val : values) {
                    uniqueValues.add(val);
                    if (uniqueValues.size() > 4) {
                        isDiscrete = false;
                        break;
                    }
                }
                
                // If discrete, set mean / std dev to NaN
                if (isDiscrete) {
                    AttributeMean[a] = Double.NaN;
                    AttributeStdDev[a] = Double.NaN;
                } else {
                    AttributeMean[a] = StatUtils.mean(values);
                    AttributeStdDev[a] = Math.sqrt(StatUtils.variance(values, AttributeMean[a]));
                    if (AttributeStdDev[a] == 0) {
                        AttributeStdDev[a] = 1; // Make sure no one divides by zero later
                    }
                }
            }
        }
        
        // For class variable
        if (ScreenClass) {
            // Get entries with a measurement
            Dataset subset = trainData.getTrainingExamples();

            // Compute mean and std dev
            double[] measurements = subset.getMeasuredClassArray();
            ClassMean = StatUtils.mean(measurements);
            ClassStdDev = Math.sqrt(StatUtils.variance(measurements, ClassMean));
            if (ClassStdDev == 0) {
                ClassStdDev = 1;
            }
        }
    }

    @Override
    protected boolean[] label(Dataset D) {
        // Check if trained
        if (! Trained) {
            throw new RuntimeException("Filter not yet trained!");
        }
        
        // Create output array
        boolean[] output = new boolean[D.NEntries()];
        
        // Loop through each entry
        for (int e=0; e<output.length; e++) {
            // Get the entry
            BaseEntry entry = D.getEntry(e);
            
            // Set outlier status to false
            output[e] = false;
            
            if (ScreenClass) {
                // Check if class is outlier
                if (entry.hasMeasurement()) {
                    double z = Math.abs(entry.getMeasuredClass() - ClassMean) / ClassStdDev;
                    output[e] = z > Tolerance;
                }
            }
            
            // If it is an outlier, continue
            if (output[e]) {
                continue;
            }
            
            if (ScreenAttributes) {
                // Check if any attributes are outliers
                for (int a=0; a < entry.NAttributes(); a++) {
                    // If attribute is discrete, skip it
                    if (Double.isNaN(AttributeMean[a])) {
                        continue;
                    }
                    
                    double z = Math.abs(entry.getAttribute(a) - AttributeMean[a]) / AttributeStdDev[a];
                    
                    if (z > Tolerance) {
                        output[e] = true;
                        break;
                    }
                }
            }
        }
        
        return output;
    }
    
}
