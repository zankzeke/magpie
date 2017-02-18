package magpie.data.utilities.filters;

/**
 * Abstract class for filters that use comparison operators.
 *
 * @author Logan Ward
 */
public abstract class ComparisonOperatorFilter extends BaseDatasetFilter {
    /** Threshold value */
    protected double Threshold = 0.0;
    /** Should entries with Feature==Threshold be kept? */
    protected boolean Equal = true;
    /** Should entries with Feature &lt; Threshold be kept? */
    protected boolean GreaterThan = true;
    /** Should entries with Feature &gt; Threshold be kept? */
    protected boolean LessThan = true;

    /**
     * Set the comparison threshold.
     * @param Threshold Desired threshold
     */
    public void setThreshold(double Threshold) {
        this.Threshold = Threshold;
    }

    /**
     * Define comparison operator. Can be &gt;, le, eq, "!=", and such.
     * @param operator Desired operator
     */
    public void setComparisonOperator(String operator) {
         switch(operator.toLowerCase()) {
            case "eq": case "=": case "==":
                Equal = true; GreaterThan = false; LessThan = false; break;
            case "ne": case "!=": case "~=": case "<>":
                Equal = false; GreaterThan = true; LessThan = true; break;
            case "gt": case ">":
                Equal = false; GreaterThan = true; LessThan = false; break;
            case "ge": case ">=": case "=>":
                Equal = true; GreaterThan = true; LessThan = false; break;
            case "lt": case "<":
                Equal = false; GreaterThan = false; LessThan = true; break;
            case "le": case "<=": case "=<":
                Equal = true; GreaterThan = false; LessThan = true; break;
            default:
                throw new IllegalArgumentException("Criteria \"" + operator + "\" not recognized.");
            }
    }

    /**
     * Evaluate the filtering criteria on each member of an array
     * @param value Values to be tested
     * @return Boolean indicating whether they pass the criteria
     */
    protected boolean[] testCriteria(double[] value) {
        boolean[] passes = new boolean[value.length]; // Starts out false
        for (int i=0; i < passes.length; i++) {
            if (Equal && value[i] == Threshold)
                passes[i] = true;
            else if (GreaterThan && value[i] > Threshold)
                passes[i] = true;
            else if (LessThan && value[i] < Threshold)
                passes[i] = true;
        }
        return passes;
    }
}
