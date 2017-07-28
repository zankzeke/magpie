package magpie.attributes.evaluators;

import java.util.Comparator;
import magpie.data.Dataset;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.Options;

/**
 * Base class for objects designed to evaluate the predictive power of single 
 *  attributes. Implementing subclasses need to provide the following operations:
 * <ul>
 * <li>{@linkplain #evaluateAttributes_internal(magpie.data.Dataset)} - Calculate predictive power of attributes
 * <li>{@linkplain #positiveIsBetter()} - Whether a more positive parameter means better predictive ability
 * <li>{@linkplain #setOptions(java.lang.String[]) } - Set any necessary options
 * </ul>
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseAttributeEvaluator implements Options{
    
    /**
     * Calculate the predictive power of each attributes in a dataset
     * @param Data Dataset to evaluate
     * @return Predictive power of each attribute
     */
    public double[] evaluateAttributes(Dataset Data) {
        if (Data.NEntries() == 0) 
            throw new Error("Dataset contains no entries");
        if (Data.NAttributes() == 0)
            throw new Error("Dataset contains no attributes");
        if (! Data.getEntry(0).hasMeasurement())
            throw new Error("Dataset contains no measurements");
        return evaluateAttributes_internal(Data);
    }
    
    /**
     * Operation that actually performs the work of evaluating attributes.
     * @param Data Dataset used to evaluate attributes
     * @return Predictive power of each attribute.
     */
    abstract protected double[] evaluateAttributes_internal(Dataset Data);
    
    /**
     * Create a comparator that will sort entries from the best to worst
     * @return Whether the predictive power of an attribute increases with increasing parameter 
     */
    abstract protected Comparator<Double> compare();
    
    /**
     * Generate rank of attributes sorted by their predictive power
     * @param Data Dataset used to evaluate attributes
     * @return List of attribute numbers sorted decreasingly by predictive ability (0 is best)
     */
    public int[] getAttributeRanks(Dataset Data) {
        double[] power = evaluateAttributes(Data);
        return UtilityOperations.sortAndGetRanks(power, compare());
    }
    
    /**
     * Print the predictive powers of the top attributes
     * @param Data Dataset used to evaluate attributes
     * @param NumberToPrint Number of top attributes to print
     * @return Formatted table of attribute names, powers, and predictive capability
     */
    public String printRankings(Dataset Data, int NumberToPrint) {
        String output = "Rank\t       Attribute_Name     \t  Power\n";
        double[] power = evaluateAttributes(Data);
        int[] rank = UtilityOperations.sortAndGetRanks(power, compare());
        for (int i = 0; i < Math.min(NumberToPrint, Data.NAttributes()); i++) {
            output += String.format("#%4d\t%24s\t%7.4f\n", i + 1, Data.getAttributeName(rank[i]), power[i]);
        }
        return output;
    }
}
