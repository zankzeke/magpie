package magpie.attributes.selectors;

import magpie.data.Dataset;
import magpie.optimization.algorithms.OptimizationHelper;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Finds pairs of attributes that are correlated with each other, and removes one of the two. User can specify which
 * correlation measure they use, and whether to pick the attribute that correlates best with the class variable. The threshold
 * defines the minimum absolute value of the correlation coefficient at which attributes are marked as "correlated."
 * <p>
 * <usage><p><b>Usage</b>: [&lt;-useclass&gt;] &lt;-pearson|-spearman|-kendall&gt; &lt;threshold&gt;
 * <br><pr><i>-useclass</i>: Whether to pick the attribute that correlates best with the class variable
 * <br><pr><i>-pearson</i>: Use Pearson's correlation coefficient
 * <br><pr><i>-spearman</i>: Use Spearman's correlation coefficient
 * <br><pr><i>-kendall</i>: Use Kendall's correlation coefficient
 * <br><pr><i>threshold</i>: Mark entries as correlated if coefficient greater than this value
 * </p></usage>
 *
 * @author Logan Ward
 */
public class RemoveCorrelatedAttributeSelector extends BaseAttributeSelector {
    /**
     * Correlation measure used for this class
     */
    protected CorrelationMeasure Measure = RemoveCorrelatedAttributeSelector.CorrelationMeasure.PEARSON;
    /**
     * Threshold at which attributes are marked as correlated.
     */
    protected double Threshold = 0.95;
    /**
     * Whether to pick the attribute most strongly correlated with the class variable
     */
    protected boolean UseClass = true;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        CorrelationMeasure measure;
        boolean useClass = false;
        double threshold;

        // Check if the "useclass option was specified
        try {
            int offset = 0;
            if (Options.get(0).toString().toLowerCase().equals("-useclass")) {
                useClass = true;
                offset++;
            }

            // Get the correlation measure
            String corrMethod = Options.get(0 + offset).toString().toLowerCase();
            switch (corrMethod) {
                case "-pearson":
                    measure = CorrelationMeasure.PEARSON;
                    break;
                case "-spearman":
                    measure = CorrelationMeasure.SPEARMAN;
                    break;
                case "-kendall":
                    measure = CorrelationMeasure.KENDALL;
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            // Get the threshold
            threshold = Double.parseDouble(Options.get(1 + offset).toString());
        } catch (Exception e) {
            throw new RuntimeException(printUsage());
        }

        // Set the values
        setUseClass(useClass);
        setMeasure(measure);
        setThreshold(threshold);
    }

    @Override
    public String printUsage() {
        return "Usage: [-useclass] <-pearson|-spearman|-kendall> <threshold>";
    }

    /**
     * Define the method used to measure correlation
     *
     * @param measure Desired method
     */
    public void setMeasure(CorrelationMeasure measure) {
        Measure = measure;
    }

    /**
     * Define whether to select the attribute most correlated with the class variable when selecting which pair
     * of correlated attributes to keep.
     *
     * @param useClass Desired setting
     */
    public void setUseClass(boolean useClass) {
        UseClass = useClass;
    }

    /**
     * Define the threshold at which attributes are declared "correlated"
     *
     * @param threshold Correlation coefficient
     */
    public void setThreshold(double threshold) {
        if (Threshold < 0 || Threshold > 1) {
            throw new IllegalArgumentException("Threshold must be between 0 and 1");
        }
        Threshold = threshold;
    }

    @Override
    protected List<Integer> train_protected(Dataset Data) {
        // Initialize array to store selections
        boolean[] isSelected = new boolean[Data.NAttributes()];
        Arrays.fill(isSelected, true);

        // Store the order in which to search over arrays
        int[] order;
        if (UseClass) {
            // Sort through the array from most correlated with class variable to least
            double[] correlations = new double[Data.NAttributes()];
            double[] classArray = Data.getMeasuredClassArray();
            for (int i = 0; i < Data.NAttributes(); i++) {
                correlations[i] = measureCorrelation(Data.getSingleAttributeArray(i), classArray);
            }

            // Sort from most to least correlated
            order = OptimizationHelper.sortAndGetRanks(correlations, true);
        } else {
            order = new int[Data.NAttributes()];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
        }

        // Go through the list and mark the correlated pairs
        for (int a1ind = 0; a1ind < order.length; a1ind++) {
            int a1 = order[a1ind];
            if (!isSelected[a1]) {
                continue; // Skip if it has already been removed
            }

            // Get the values for this attribute
            double[] a1Vals = Data.getSingleAttributeArray(a1);

            // Loop through the other attributes
            for (int a2ind = a1ind + 1; a2ind < order.length; a2ind++) {
                int a2 = order[a2ind];
                if (!isSelected[a2]) {
                    continue; // Skip if it has already been removed
                }

                // Get the values for this attribute
                double[] a2Vals = Data.getSingleAttributeArray(a2);

                // Mark a2 as "correlated" if its correlation is above the threshold
                //  Note: a1 has the greater correlation with the class, if the user specified UseClass == true
                if (measureCorrelation(a1Vals, a2Vals) >= Threshold) {
                    isSelected[a2] = false;
                }
            }
        }

        // Prepare the output list
        List<Integer> output = new ArrayList<>(order.length);
        for (int a = 0; a < isSelected.length; a++) {
            if (isSelected[a]) {
                output.add(a);
            }
        }
        return output;
    }

    /**
     * Measure the strength correlation between two arrays
     *
     * @param x One array
     * @param y A second array
     * @return The magnitude of the correlation coefficient between these two arrays
     */
    protected double measureCorrelation(double[] x, double[] y) {
        double corr;
        switch (Measure) {
            case KENDALL:
                corr = new KendallsCorrelation().correlation(x, y);
                break;
            case PEARSON:
                corr = new PearsonsCorrelation().correlation(x, y);
                break;
            case SPEARMAN:
                corr = new SpearmansCorrelation().correlation(x, y);
                break;
            default:
                throw new RuntimeException("Correlation method not implemented: " + Measure.toString());
        }
        return Math.abs(corr);
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        StringBuilder output = new StringBuilder();

        output.append("Reduces the number of correlated attributes by selecting only one of two interrelated attributes. ");
        output.append("Specifically, this class searches for pairs attributes which have a ");
        output.append(Measure.toString().toLowerCase());
        output.append(String.format(" correlation coefficient whose absolve value is greater than %f and ", Threshold));

        if (UseClass) {
            output.append("selects the one that has the strongest correlation to the class variable.");
        } else {
            output.append("selects whichever one is listed first.");
        }

        return output.toString();
    }

    /**
     * List of correlation measures
     */
    public enum CorrelationMeasure {
        PEARSON, SPEARMAN, KENDALL
    }
}
