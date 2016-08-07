package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.utility.DistinctPermutationGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Generate compositions with a fixed stoichiometry. For example, use this code 
 * to generate all AB compounds.
 * 
 * <p>Creates entries by computing unique combinations of elements in certain, 
 * user-defined ratios.
 * 
 * <usage><p><b>Usage</b>: &lt;stoichiometries...&gt; -elems &lt;elements...&gt;
 * <br><pr><i>stiochiometries</i>: List of stoichiometries to generate (e.g., AB)
 * <br><pr><i>elements</i>: List of elements use when creating entries</usage>
 * @author Logan Ward
 */
public class FixedStoichiometryCompositionEntryGenerator extends BaseEntryGenerator {
    /** List of stiochiometries. Stored as ratio list. */
    protected List<double[]> Stiochiometries = new ArrayList<>();
    /** Set of elements to use when generating entries */
    protected Set<Integer> Elements = new TreeSet<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() < 3) {
            throw new IllegalArgumentException(printUsage());
        }
        
        // Clear the current data
        clearElements();
        clearStoichiometries();
        
        // Get the stoichiometries
        int pos = 0;
        while (pos < Options.size() && 
                ! Options.get(pos).toString().equalsIgnoreCase("-elems")) {
            addStoichiometry(Options.get(pos++).toString());
        }
        
        // Get the elements
        if (pos == Options.size()) {
            throw new IllegalArgumentException(printUsage());
        }
        pos++;
        while (pos < Options.size()) {
            addElement(Options.get(pos++).toString());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <stoichiometries...> -elems <elements...>";
    }
    
    /**
     * Clear the list of currently-defined stoichiometries
     */
    public void clearStoichiometries() {
        Stiochiometries.clear();
    }
    
    /**
     * Clear the list of currently-defined elements
     */
    public void clearElements() {
        Elements.clear();
    }
    
    /**
     * Add an element to the list of those used when generating entries
     * @param elem Element to add
     */
    public void addElement(String elem) {
        // Get the element ID
        int elemID = ArrayUtils.indexOf(LookupData.ElementNames, elem);
        if (elemID == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalArgumentException("No such element: " + elem);
        }
        
        // Add it to the set
        Elements.add(elemID);
    }
    
    /**
     * Add a stoichiometry to the list of those to generate
     * 
     * <p>Simply list the stoichiometry using example element names. Examples 
     * include NiZr, AB, or A2BC
     * 
     * @param stoich Stoichiometry to add
     */
    public void addStoichiometry(String stoich) {
        // Parse it
        Pattern ptrn = Pattern.compile("[A-Z][a-z]?(?<amount>[0-9.]*)");
        Matcher hits = ptrn.matcher(stoich);
        
        //   Get the points
        List<Double> amounts = new ArrayList<>();
        while (hits.find()) {
            String temp = hits.group("amount");
            if (temp.length() == 0) {
                amounts.add(new Double(1));
            } else {
                try {
                    amounts.add(new Double(temp));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Parse failure: \"" + temp + "\" is not a number" );
                }
            }
        }
        
        // Put it in the list
        Stiochiometries.add(ArrayUtils.toPrimitive(amounts.toArray(new Double[0])));
    }

    /**
     * Get the current list of stoichiometries
     * @return List of stoichiometries
     */
    public List<double[]> getStiochiometries() {
        return new ArrayList<>(Stiochiometries);
    }

    /**
     * Get set of elements used in generation
     * @return Set of elements by abbreviation
     */
    public Set<String> getElements() {
        Set<String> output = new TreeSet<>();
        for (Integer elem : Elements) {
            output.add(LookupData.ElementNames[elem]);
        }
        return output;
    }

    @Override
    public Iterator<BaseEntry> iterator() {
        // Make an iterator over stoichiometires
        final Iterator<double[]> stoichIter = Stiochiometries.iterator();
        
        // Turn elements into list
        final List<Integer> elementList = new ArrayList<>(Elements);
        
        return new Iterator<BaseEntry>() {
            /** Current stoichiometry being generated */
            protected double[] CurStoich = null;
            /** Iterator over the element list */
            protected Iterator<int[]> ElemsCombIter = new ArrayList<int[]>().iterator();
            /** Current combination of elements */
            protected int[] CurElemsComb = null;
            /** Iterator over unique arrangements of amounts */
            protected Iterator<double[]> AmountIterator = new ArrayList<double[]>().iterator();
            
            @Override
            public boolean hasNext() {
                return stoichIter.hasNext() 
                        || ElemsCombIter.hasNext() 
                        || AmountIterator.hasNext();
            }

            @Override
            public BaseEntry next() {
                // Integredients for composition
                double[] amounts;
                
                // Check if there is another unique order of amounts
                if (AmountIterator.hasNext()) {
                    amounts = AmountIterator.next();
                } else {
                    // Get a new combination of elements and restart the amount iterator
                    int[] elemListIDs;
                    if (ElemsCombIter.hasNext()) {
                        // Get the next item in the list
                        elemListIDs = ElemsCombIter.next();
                        
                        // Restart the amount list and get first amount
                        AmountIterator = DistinctPermutationGenerator
                                .generatePermutations(CurStoich).iterator();
                        amounts = AmountIterator.next();
                    } else {
                        // Get the next stoichiometry
                        CurStoich = stoichIter.next();

                        // Restart the iterator
                        ElemsCombIter = CombinatoricsUtils.combinationsIterator(
                                elementList.size(), CurStoich.length);

                        // Get the next amount iterator
                        AmountIterator = DistinctPermutationGenerator
                                .generatePermutations(CurStoich).iterator();
                        
                        // Get the next values
                        amounts = AmountIterator.next();
                        elemListIDs = ElemsCombIter.next();
                    }
                    
                    // Convert element list indices to element IDs
                    CurElemsComb = new int[elemListIDs.length];
                    for (int i=0; i<CurElemsComb.length; i++) {
                        CurElemsComb[i] = elementList.get(elemListIDs[i]);
                    }
                }
                
                // Make the entry
                try {
                    return new CompositionEntry(CurElemsComb, amounts);
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
