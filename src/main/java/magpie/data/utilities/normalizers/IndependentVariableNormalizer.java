
package magpie.data.utilities.normalizers;

import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Base class for normalizers that treat attributes/class independently. Implementations
 *  must supply the following functions:
 * 
 * <ol>
 * <li>{@link #computeAttributeStatistics(int, double[])}
 * <li>{@link #normalizeAttributes(double[])}
 * <li>{@linkplain #restoreAttributes(double[]) }
 * </ol>
 * 
 * @author Logan Ward
 */
abstract public class IndependentVariableNormalizer extends BaseDatasetNormalizer {

    @Override
    protected void trainOnAttributes(Dataset Data) {
        prepareAttributeArrays(Data.NAttributes());
        // For each attribute, calculate statistics
        for (int i=0; i<Data.NAttributes(); i++) {
            computeAttributeStatistics(i, Data.getSingleAttributeArray(i));
        }
    }

    @Override
    protected void trainOnMeasuredClass(Dataset Data) {
        double[] measured;
        measured = Data.getMeasuredClassArray();
        computeClassStatistics(measured);
    }
    
    /**
     * Prepare arrays that will hold attribute statistics.
     * @param NAttributes 
     */
    abstract protected void prepareAttributeArrays(int NAttributes);
    
    /**
     * Compute statistics required to perform normalization/restoration.
     * @param attributeNumber Attribute number
     * @param values 
     */
    abstract protected void computeAttributeStatistics(int attributeNumber, double[] values);
    
    /**
     * Compute statistics regarding the class variable.
     * @param values Measured values of the class variable
     */
    abstract protected void computeClassStatistics(double[] values);

    @Override
    protected void normalizeAttributes(Dataset Data) {
        for (BaseEntry entry : Data.getEntries()) {
            double[] normalized = entry.getAttributes();
            normalizeAttributes(normalized);
            entry.setAttributes(normalized);
        }
    }

    @Override
    protected void normalizeClassVariable(Dataset Data) {
        for (BaseEntry entry : Data.getEntries() ) {
            if (entry.hasMeasurement()) {
                entry.setMeasuredClass(normalizeClassVariable(entry.getMeasuredClass()));
            }
            if (entry.hasPrediction()) {
                entry.setPredictedClass(normalizeClassVariable(entry.getPredictedClass()));
            }
        }
    }
    
    /**
     * Normalize each attribute for an entry
     * @param attributes Attributes to be normalized
     */
    abstract protected void normalizeAttributes(double[] attributes);
    
    /**
     * Normalize a class variable
     * @param value Value of class variable
     * @return Normalized value
     */
    abstract protected double normalizeClassVariable(double value);

    @Override
    protected void restoreAttributes(Dataset Data) {
        // For each entry, restore attributes
        for (BaseEntry entry : Data.getEntries()) {
            double[] normalized = entry.getAttributes();
            restoreAttributes(normalized);
            entry.setAttributes(normalized);
        }
    }

    @Override
    protected void restoreClassVariable(Dataset Data) {
        for (BaseEntry entry : Data.getEntries()) {
            if (entry.hasMeasurement()) {
                entry.setMeasuredClass(restoreClassVariable(entry.getMeasuredClass()));
            }
            if (entry.hasPrediction()) {
                entry.setPredictedClass(restoreClassVariable(entry.getPredictedClass()));
            }
        }
    }
    
    /**
     * Restore each attribute for an entry
     * @param attributes Normalized attributes to be restored
     */
    abstract protected void restoreAttributes(double[] attributes);
    
    /**
     * Restore a class variable
     * @param value Normalized value of class variable
     * @return Original value
     */
    abstract protected double restoreClassVariable(double value);
}
