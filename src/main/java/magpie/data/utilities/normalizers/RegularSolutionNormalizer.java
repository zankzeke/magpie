package magpie.data.utilities.normalizers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Normalize data using regular solution model. Goal is to transform class
 * variable based on stoichiometry of material by fitting a regular solution 
 * model to the class variable given associated composition.
 * 
 * <p>Let <code>y</code> be the variable to be normalized, <code>x</code> be the 
 * composition of the material where x[Z] is the fraction of element with atomic 
 * number Z, and <code>y'</code> be the normalized class variable. 
 * 
 * <p><center><code>y = y' * sum[ x[i] * x[j] ] for j>i ]</code></center>
 * 
 * <p>This model is designed to normalize class values where the class must
 * be equal to zero for unary compositions, and increases in magnitude 
 * when the amount of mixing is large (e.g., formation energy). 
 * 
 * <p>Note: You <i>should</i> filter out unary compositions before using this filter.
 * This normalization procedure would lead to division by zero for unary compounds.
 * For numerical stability, we multiple them by -1 during the normalization procedure, 
 * but we recommend you filter out unary entries before running this normalizer.
 * 
 * <usage><p><b>Usage</b>: *No Options*</usage>
 * 
 * @author Logan Ward
 */
public class RegularSolutionNormalizer extends BaseDatasetNormalizer {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    protected void trainOnAttributes(Dataset Data) {
        // Nothing to do
    }

    @Override
    protected void trainOnMeasuredClass(Dataset Data) {
        // Nothing to do
    }

    @Override
    protected void normalizeAttributes(Dataset Data) {
        if (! (Data instanceof CompositionDataset)) {
            throw new RuntimeException("Dataset must contain composition data.");
        }
        
        for (BaseEntry entry : Data.getEntries()) {
            double mixingTerm = computeMixingTerm((CompositionEntry) entry);
            for (int a=0; a<entry.NAttributes(); a++) {
                entry.setAttribute(a, entry.getAttribute(a) / mixingTerm);
            }
        }
    }

    @Override
    protected void restoreAttributes(Dataset Data) {
        if (! (Data instanceof CompositionDataset)) {
            throw new RuntimeException("Dataset must contain composition data.");
        }
        
        for (BaseEntry entry : Data.getEntries()) {
            double mixingTerm = computeMixingTerm((CompositionEntry) entry);
            for (int a=0; a<entry.NAttributes(); a++) {
                entry.setAttribute(a, entry.getAttribute(a) * mixingTerm);
            }
        }
    }

    @Override
    protected void normalizeClassVariable(Dataset Data) {
        if (! (Data instanceof CompositionDataset)) {
            throw new RuntimeException("Dataset must contain composition data.");
        }
        
        for (BaseEntry entry : Data.getEntries()) {
            double mixingTerm = computeMixingTerm((CompositionEntry) entry);
            
            if (entry.hasMeasurement()) {
                entry.setMeasuredClass(entry.getMeasuredClass() / mixingTerm);
            }
            if (entry.hasPrediction()) {
                entry.setPredictedClass(entry.getPredictedClass() / mixingTerm);
            }
        }
    }

    @Override
    protected void restoreClassVariable(Dataset Data) {
        if (! (Data instanceof CompositionDataset)) {
            throw new RuntimeException("Dataset must contain composition data.");
        }
        
        for (BaseEntry entry : Data.getEntries()) {
            double mixingTerm = computeMixingTerm((CompositionEntry) entry);
            
            if (entry.hasMeasurement()) {
                entry.setMeasuredClass(entry.getMeasuredClass() * mixingTerm);
            }
            if (entry.hasPrediction()) {
                entry.setPredictedClass(entry.getPredictedClass() * mixingTerm);
            }
        }
    }
    
    /**
     * Compute the mixing term for a solution model. This is defined as
     * 
     * <p><code><center>sum[ x[i] * x[j] ] for i<j</center></code>
     * 
     * where x[Z] is the fraction of element Z.
     * 
     * <p>Note: If number of elements is 1, returns -1. 
     * 
     * @param entry Entry for which to compute mixing term
     * @return Value of mixing term
     */
    static public double computeMixingTerm(CompositionEntry entry) {
        // Get the fractions of each element
        double[] fractions = entry.getFractions();
        if (fractions.length == 1) {
            return -1;
        }
        
        // Compute mixing term
        double output = 0;
        for (int i=0; i<fractions.length; i++) {
            for (int j=i+1; j<fractions.length; j++) {
                output += fractions[i] * fractions[j];
            }
        }
        
        return output;
    }
}
