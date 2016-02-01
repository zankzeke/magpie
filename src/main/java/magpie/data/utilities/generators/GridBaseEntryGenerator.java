
package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.utility.NDGridIterator;

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
     * @param maxValue Desired maximum value
     */
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            if (Options.size() != 4) {
                throw new Exception();
            }
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
    public Iterator<BaseEntry> iterator() {
        int sideLength = (int) Math.floor((maxValue - minValue) / gridSpacing + 1);
        final NDGridIterator iter = new NDGridIterator(dimension, sideLength);
        return new Iterator<BaseEntry>() {
            
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public BaseEntry next() {
                int[] counter = iter.next();
                
                // Determine position
                double[] position = new double[dimension];
                for (int d=0; d<dimension; d++) {
                    position[d] = minValue + gridSpacing * counter[d];
                }
                
                // Create entry
                BaseEntry entry = new BaseEntry();
                entry.addAttributes(position);
                return entry;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }
}
