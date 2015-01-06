
package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;

/**
 * Generate {@linkplain BaseEntry} objects that represent a grid. User defines 
 * the dimension of grid, maximum value, minimum value, and grid spacing.
 * 
 * <usage><p><b>Usage</b>: &lt;dimension&gt; &lt;min&gt; &lt;step&gt; &lt;step&gt;
 * <br><pr><i>dimension</i>: Number of dimensions in grid
 * <br><pr><i>min</i>: Minimum value in each direction
 * <br><pr><i>step</i>: Distance in each direction between points
 * <br><pr><i>max</i>: Maximum value in each direction</usage>
 * @author Logan Ward
 */
public class GridBaseEntryGenerator extends BaseEntryGenerator {
    /** Dimension of grid */
    private int dimension = 2;
    /** Minimum value of grid */
    private double minValue = -1.0;
    /** Maximum value of grid */
    private double maxValue = 1.0;
    /** Grid spacing */
    private double gridSpacing = 0.1;

    /**
     * Define dimension of grid.
     * @param dimension Desired dimension
     */
    public void setDimension(int dimension) {
        if (dimension <= 0) {
            throw new Error("Grid dimension must be greater than 0.");
        }
        this.dimension = dimension;
        
    }

    /**
     * Define minimum value in all directions.
     * @param minValue Desired minimum value
     */
    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    /**
     * Define grid step size.
     * @param gridSpacing Desired step size
     */
    public void setGridSpacing(double gridSpacing) {
        this.gridSpacing = Math.abs(gridSpacing);
    }

    /**
     * Define maximum value in all directions
     * @param maxValue 
     */
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            setDimension(Integer.parseInt(Options.get(0).toString()));
            setMinValue(Double.parseDouble((Options.get(1).toString())));
            setGridSpacing(Double.parseDouble(Options.get(2).toString()));
            setMaxValue(Double.parseDouble(Options.get(3).toString()));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <dimension> <min> <step> <max>";
    }

    @Override
    public List<BaseEntry> generateEntries() {
        double[][] grid = generateGrid();
        List<BaseEntry> output = new ArrayList<>(grid.length);
        for (double[] point : grid) {
            BaseEntry entry = new BaseEntry();
            entry.setAttributes(point);
            output.add(entry);
        }
        return output;
    }
    
    /**
     * Generates grid of points based on current settings.
     * @return Array containing coordinates of each point
     */
    protected double[][] generateGrid() {
        // Calculate number of points in each direction
        double sideLength = (maxValue - minValue) / gridSpacing + 1;
        sideLength = Math.floor(sideLength);
        
        // Pre-allocate grid
        double[][] output = new double[(int) Math.pow(sideLength, (double) dimension)][];
        double[] point = new double[dimension];
        int position = 0;
        while (true) {
            output[position] = new double[dimension];
            for (int d=0; d<dimension; d++) {
                output[position][d] = point[d] * gridSpacing + minValue;
            }
            // Increment position
            boolean allDone = false;
            position++;
            for (int d=0; d<dimension; d++) {
                point[d]++;
                allDone = false;
                if (point[d] >= sideLength) {
                    point[d] = 0;
                    allDone = true;
                    continue;
                } else {
                    break;
                }
            }
            if (allDone) break;
        }
        if (position != output.length) {
            throw new Error("Implementation Error: Incorrect number of entries generated.");
        }
        return output;
    }
}
