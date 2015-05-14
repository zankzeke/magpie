package magpie.utility;

import java.util.*;

/**
 * Generate Cartesian sum of several collections. These are defined every possible
 * combination of exactly one item from each set.
 * @author Logan Ward
 * @param <Type> Data type
 */
public class CartesianSumGenerator<Type> implements Iterable<List<Type>> {
    /** Collections to be summed */
    final private List<List<Type>> Items;

    public CartesianSumGenerator(Collection<Type>... collections) {
        Items = new ArrayList<>(collections.length);
        for (Collection<Type> col : collections) {
            if (col.isEmpty()) {
                throw new Error("Collections cannot be empty");
            }
            Items.add(new ArrayList<>(col));
        }
    }

    @Override
    public Iterator<List<Type>> iterator() {
        return new Iterator<List<Type>>() {
            /** Iterator position */
            private int[] Position = new int[Items.size()];

            @Override
            public boolean hasNext() {
                return Position != null;
            }

            @Override
            public List<Type> next() {
                // Get the list at the current position
                List<Type> output = new ArrayList<>(Items.size());
                for (int i=0; i<Items.size(); i++) {
                    output.add(Items.get(i).get(Position[i]));
                }
                
                // Increment counter
                if (incrementCounter(Position)) {
                    Position = null; // Mark that we're done
                }
                return output;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }
    
    /**
     * Increment position of counter. 
     * @param counter Counter to be incremented
     * @return Whether the end has been reached
     */
    protected boolean incrementCounter(int[] counter) {
        for (int s = 0; s < Items.size(); s++) {
            if (counter[s] == Items.get(s).size() - 1) {
                counter[s] = 0;
            } else {
                counter[s]++;
                return false;
            }
        }
        return true;
    }
}
