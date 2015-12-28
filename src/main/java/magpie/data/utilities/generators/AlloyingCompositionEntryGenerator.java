package magpie.data.utilities.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Generate entries by adding alloying elements into a set composition. Incrementally
 * adds more of certain elements into alloy.
 * 
 * New compositions are computed by ([old composition])<sub>1-x</sub>[new element]<sub>x</sub>
 * where x is the fraction of element added to alloy.
 * 
 * <usage><p><b>Usage</b>: &lt;base composition&gt; &lt;max addition&gt; &lt;step size&gt; &lt;elements...&gt;
 * <pr><br><i>base composition</i>: Base composition to be alloyed into
 * <pr><br><i>max addition</i>: Maximum fraction of element to add
 * <pr><br><i>step size<i>: Step size between alloying elements
 * <pr><br><i>elements</i>: Elements to add in</usage>
 * 
 * @author Logan Ward
 */
public class AlloyingCompositionEntryGenerator extends BaseEntryGenerator {
    /** Elements of composition to be added to */
    protected int[] Elements;
    /** Fractions of each element of composition to be added to */
    protected double[] Fractions;
    /** Maximum amount to add. Default 10% */
    protected double MaxAlloying = 0.10;
    /** Step size. Default = 1% */
    protected double AlloyStep = 0.01;
    /** Set of elements to add into alloy */
    final protected Set<Integer> AlloyingElements = new TreeSet<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String baseComposition;
        double maxAlloy, alloyStep;
        Set<String> elems = new TreeSet<>();
        
        try {
            // Get the default arguments
            baseComposition = Options.get(0).toString();
            maxAlloy = Double.parseDouble(Options.get(1).toString());
            alloyStep = Double.parseDouble(Options.get(2).toString());
            
            // Get the elements
            for (Object obj : Options.subList(3, Options.size())) {
                elems.add(obj.toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set options
        setComposition(new CompositionEntry(baseComposition));
        setMaxAlloying(maxAlloy);
        setAlloyStep(alloyStep);
        clearElements();
        addAlloyingElements(elems);
    }

    @Override
    public String printUsage() {
        return "Usage: <base composition> <max addition> <step size> <elements...>";
    }

    /**
     * Set starting composition for alloys
     * @param composition Desired composition
     */
    public void setComposition(CompositionEntry composition) {
        this.Elements = composition.getElements();
        this.Fractions = composition.getFractions();
    }

    /** 
     * Set the maximum amount of element to add.
     * @param max Maximum fraction (should be less than 1)
     */
    public void setMaxAlloying(double max) {
        if (max <= 0 || max >= 1) {
            throw new IllegalArgumentException("max should be between 0 and 1. Value: " + max);
        }
        this.MaxAlloying = max;
    }

    /**
     * Set the step size 
     * @param step Spacing between alloy, in fractional units
     */
    public void setAlloyStep(double step) {
        if (step <= 0 || step >= 1) {
            throw new IllegalArgumentException("step should be between 0 and 1. Value: " + step);
        }
        this.AlloyStep = step;
    }
    
    /**
     * Clear the list of alloying elements
     */
    public void clearElements() {
        AlloyingElements.clear();
    }
    
    /**
     * Add element to list of potential alloying elements
     * @param element Symbol (ex: Au) of element to be added
     */
    public void addAlloyingElement(String element) {
        // Get the id of the element
        int elemID = ArrayUtils.indexOf(LookupData.ElementNames, element);
        if (elemID == ArrayUtils.INDEX_NOT_FOUND) {
            throw new RuntimeException("No such element: " + element);
        }
        
        // Add to list
        AlloyingElements.add(elemID);
    }
    
    /**
     * Add elements to list of potential alloying elements
     * @param elements Symbols (ex: Au) of elements to be added
     */
    public void addAlloyingElements(Collection<String> elements) {
        for (String element : elements) {
            addAlloyingElement(element);
        }
    }

    @Override
    public Iterator<BaseEntry> iterator() {
        
        
        return new Iterator<BaseEntry>() {
            /** Iterates over elements */
            Iterator<Integer> elemIter = AlloyingElements.iterator();
            /** Iterates over fractions */
            Iterator<Double> fracIter = null;
            /** Current alloying element */
            int curElem;

            @Override
            public boolean hasNext() {
                return elemIter.hasNext() || (fracIter == null ? false : fracIter.hasNext());
            }

            @Override
            public BaseEntry next() {
                // If there is another fraction
                if (fracIter == null || !fracIter.hasNext()) {
                    // Get next element
                    curElem = elemIter.next();
                    
                    // Re-initialize fracIter
                    List<Double> possibleFracs = new ArrayList<>((int) (MaxAlloying / AlloyStep));
                    for (double x=AlloyStep; x<=MaxAlloying; x+=AlloyStep) {
                        possibleFracs.add(x);
                    }
                    fracIter = possibleFracs.iterator();
                }
                
                // Get fraction
                double toAdd = fracIter.next();
                
                // Get new element list
                int[] newElems = ArrayUtils.add(Elements, curElem);
                double[] newFracs = ArrayUtils.add(Fractions, toAdd);
                
                // Adjust old fractions
                for (int i=0; i<Elements.length; i++) {
                    newFracs[i] *= (1 - toAdd);
                }
                
                // Make new composition, and return it
                try {
                    return new CompositionEntry(newElems, newFracs);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported."); 
            }
        };
    }
    
}
