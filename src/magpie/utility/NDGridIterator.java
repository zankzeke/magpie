package magpie.utility;

import java.util.Iterator;

/**
 * Iterate over a n-dimensional array. Example: Iterate over all points in
 * a x by y by z grid, increments from [0,0,0] to [x,y,z].
 * 
 * @author Logan Ward
 */
public class NDGridIterator implements Iterator<int[]> {
    /** Size of grid */
    final private int[] Size;
    /** Current position in iterator */
    private int[] Counter;

    /**
     * Create a iterator over a 
     * @param dim Dimension of grid
     * @param size Number of points in each dimension
     */
    public NDGridIterator(int dim, int size) {
        Size = new int[dim];
        for (int i=0; i<dim; i++) {
            Size[i] = size;
        }
        Counter = new int[dim];
    } 

    /**
     * Initialize iterator
     * @param size Size of each dimension of grid
     */
    public NDGridIterator(int[] size) {
        Size = size.clone();
        Counter = new int[Size.length];
    }

    @Override
    public boolean hasNext() {
        return Counter != null;
    }

    @Override
    public int[] next() {
        int[] output = Counter.clone();
        if (incrementCounter()) {
            Counter = null;
        }
        return output;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }
    
    /**
     * Increment position of counter. 
     * @return Whether the end has been reached
     */
    protected boolean incrementCounter() {
        for (int d = 0; d < Size.length; d++) {
            if (Counter[d] == Size[d] - 1) {
                Counter[d] = 0;
            } else {
                Counter[d]++;
                return false;
            }
        }
        return true;
    }
    
}
