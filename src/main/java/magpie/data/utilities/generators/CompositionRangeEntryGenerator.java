package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.utility.CartesianSumGenerator;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Create compositions in a rectangular, user-specified grid. User should specify
 * the base element along with the minimum, maximum, and step size for each element
 * to be added. It is also possible to define the minimum and maximum amount of a base element
 * 
 * <p>Example: Al with 0-10at% Ni and 0-10at% Zr in step sizes of 0.2at%
 * 
 * <usage><p><b>Usage</b>: &lt;base&gt; [-min &lt;min base fraction&gt;]
 *  [-max &lt;max base fraction&gt;]
 *  -alloy &lt;alloy elem&gt; &lt;alloy min&gt; &lt;alloy max&gt; &lt;alloy step&gt;
 *  [&lt;...&gt;]
 * <br><pr><i>base</i>: Symbol of base element
 * <br><pr><i>min base fraction</i>: Optional: Minimum fraction of base element
 * <br><pr><i>max base fraction</i>: Optional: Maximum fraction of base element
 * <br><pr><i>alloy elem</i>: Symbol of alloying element
 * <br><pr><i>alloy elem</i>: Minimum fraction of alloying element
 * <br><pr><i>alloy elem</i>: Maximum fraction of alloying element
 * <br><pr><i>alloy elem</i>: Step size of alloying element
 * <pr>User can supply any number of alloying elements</usage>
 * 
 * @author Logan Ward
 */
public class CompositionRangeEntryGenerator extends BaseEntryGenerator {
    /** ID of base element */
    protected int BaseElement = -1;
    /** Set minimum amount of base element */
    protected double MinBaseElement = Double.NaN;
    /** Set maximum amount of base element */
    protected double MaxBaseElement = Double.NaN;
    /** Minimum fraction of certain element. */
    final protected SortedMap<Integer, Double> MinFraction = new TreeMap<>();
    /** Maximum fraction of certain element. */
    final protected SortedMap<Integer, Double> MaxFraction = new TreeMap<>();
    /** Fraction step size of certain element. */
    final protected SortedMap<Integer, Double> StepFraction = new TreeMap<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // Options to be set
        String baseElem;
        double minBase = Double.NaN, maxBase = Double.NaN;
        List<String> alloyElems = new ArrayList<>();
        List<Double> alloyMin = new ArrayList<>();
        List<Double> alloyMax = new ArrayList<>();
        List<Double> alloyStep = new ArrayList<>();
        
        // Loop through options
        int pos = 0;
        try {
            baseElem = Options.get(pos++).toString();
            
            while (pos < Options.size()) {
                String flag = Options.get(pos++).toString();
                
                switch (flag.toLowerCase()) {
                    case "-max":
                        maxBase = Double.parseDouble(Options.get(pos++).toString());
                        break;
                    case "-min":
                        minBase = Double.parseDouble(Options.get(pos++).toString());
                        break;
                    case "-alloy":
                        alloyElems.add(Options.get(pos++).toString());
                        alloyMin.add(Double.parseDouble(Options.get(pos++).toString()));
                        alloyMax.add(Double.parseDouble(Options.get(pos++).toString()));
                        alloyStep.add(Double.parseDouble(Options.get(pos++).toString()));
                        break;
                    default:
                        throw new Exception();
                }
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Applying settings
        clearAlloyingElements();
        setBaseElement(baseElem, minBase, maxBase);
        for (int elem=0; elem<alloyElems.size(); elem++) {
            addAlloyElement(alloyElems.get(elem),
                    alloyMin.get(elem),
                    alloyMax.get(elem),
                    alloyStep.get(elem));
        }
    }

    @Override
    public String printUsage() {
        return "<base element> [-min <min base fraction>] [-max <max base fraction>]"
                + " -alloy <alloy elem> <alloy min> <alloy max> <alloy step> [<...>]";
    }

    
    /**
     * Set the base element. ID is the atomic number minus one.
     * @param id ID of desired base element
     * @param min Minimum allowed value of base element. Use NaN for no limit
     * @param max Maximum allowed value of base element. Use NaN for no limit
     */
    public void setBaseElement(int id, double min, double max) {
        this.BaseElement = id;
        
        // Remove this element from any of the list of those to be added
        MinFraction.remove(id);
        MaxFraction.remove(id);
        StepFraction.remove(id);
        
        // Set min / max
        MinBaseElement = min;
        MaxBaseElement = max;
    }
    
    /**
     * Set the base element.
     * @param elem Symbol of the desired base element
     * @param min Minimum allowed value of base element. Use NaN for no limit
     * @param max Maximum allowed value of base element. Use NaN for no limit
     */
    public void setBaseElement(String elem, double min, double max) {
        // Lookup ID 
        int temp = ArrayUtils.indexOf(LookupData.ElementNames, elem);
        
        if (temp == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalArgumentException("Element not recognized: " + elem);
        }
        
        // Set the base element
        setBaseElement(temp, min, max);
    }
    
    /**
     * Clear the list of elements to be added
     */
    public void clearAlloyingElements() {
        MinFraction.clear();
        MaxFraction.clear();
        StepFraction.clear();
    }
    
    /**
     * Add alloying element
     * @param id ID of element
     * @param min Minimum fraction of element
     * @param max Maximum fraction of element
     * @param step Desired step size
     */
    public void addAlloyElement(int id, double min, double max, double step) {
        // Check that min < max
        if (min > max) {
            throw new IllegalArgumentException("Minimum must be smaller than maximum: " 
                    + min + " > " + max);
        }
        
        // Check that min / max are smaller than 1
        if (max > 1 || min > 1) {
            throw new IllegalArgumentException("Fractions must be smaller than 1");
        }
        
        // Check that element is not the same as the base element
        if (id == BaseElement) {
            throw new IllegalArgumentException("Element cannot be same as base element");
        }
        
        // Add to maps
        MinFraction.put(id, min);
        MaxFraction.put(id, max);
        StepFraction.put(id, step);
    }
    
    /**
     * Add alloying element
     * @param elem Symbol of element
     * @param min Minimum fraction of element
     * @param max Maximum fraction of element
     * @param step Desired step size
     */
    public void addAlloyElement(String elem, double min, double max, double step) {
        // Lookup ID 
        int temp = ArrayUtils.indexOf(LookupData.ElementNames, elem);
        
        if (temp == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalArgumentException("Element not recognized: " + elem);
        }
        
        // Add element
        addAlloyElement(temp, min, max, step);
    }

    @Override
    public Iterator<BaseEntry> iterator() {
        // Check if elements were not defined
        if (MaxFraction.isEmpty()) {
            throw new RuntimeException("No alloying elements were defined");
        }
        if (BaseElement == -1) {
            throw new RuntimeException("Base element was not defined");
        }
        
        // Prepare Cartesian sum generator
        List<Collection<Double>> alloyFractions = new ArrayList<>(MaxFraction.size());
        for (int elem : MaxFraction.keySet()) {
            List<Double> temp = new ArrayList<>();
            for (double x = MinFraction.get(elem); x <= MaxFraction.get(elem);
                    x += StepFraction.get(elem)) {
                temp.add(x);
            }
            alloyFractions.add(temp);
        }
        CartesianSumGenerator<Double> gen = new CartesianSumGenerator<>(alloyFractions);
        
        // Extract iterator
        final Iterator<List<Double>> alloyIter = gen.iterator();
        
        // Get the first entry
        final CompositionEntry firstEntry = getNextComposition(alloyIter);
        
        // Create the iterator
        return new Iterator<BaseEntry>() {
            /** Current position */
            CompositionEntry curEntry = firstEntry;

            @Override
            public boolean hasNext() {
                return curEntry != null;
            }

            @Override
            public BaseEntry next() {
                // Store current position
                CompositionEntry toReturn = curEntry;
                
                // Find next entry
                curEntry = getNextComposition(alloyIter);
                
                return toReturn;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }
    
    /**
     * Get the next alloy composition in the sequence. Alloy must have positive
     * fractions of each element, and a base element fraction within specified
     * ranges.
     * @param alloyIter Iterator of alloy compositions. Created in {@linkplain #iterator() }
     * @return Next alloy composition, or null if there is no next one
     */
    protected CompositionEntry getNextComposition(Iterator<List<Double>> alloyIter) {
        while (alloyIter.hasNext()) {
            // Get the next list of alloying element fractions
            List<Double> alloyFracs = alloyIter.next();
            
            // Compute the amount of base element
            double baseFrac = 1.0;
            for (Double x : alloyFracs) {
                baseFrac -= x;
            }
            
            // Check if it is within bounds
            if (baseFrac < 0) {
                continue;
            }
            if (! Double.isNaN(MinBaseElement) && baseFrac < MinBaseElement) {
                continue;
            }
            if (! Double.isNaN(MaxBaseElement) && baseFrac > MaxBaseElement) {
                continue;
            }
            
            // If so, generate the alloy composition
            double[] fracs = new double[alloyFracs.size() + 1];
            int[] elems = new int[alloyFracs.size() + 1];
            
            //   Base element
            fracs[0] = baseFrac;
            elems[0] = BaseElement;
            
            //   Alloying elements
            Iterator<Integer> elemIter = MaxFraction.keySet().iterator();
            for (int i=1; i<=alloyFracs.size(); i++) {
                elems[i] = elemIter.next();
                fracs[i] = alloyFracs.get(i - 1);
            }
            
            // Return result
            try {
                return new CompositionEntry(elems, fracs);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        // Return null, nothing next
        return null;
    }
}
