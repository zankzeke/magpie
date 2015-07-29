package magpie.data.materials;

import java.text.DecimalFormat;
import magpie.data.MultiPropertyEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.*;
import magpie.data.materials.util.LookupData;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Stores several properties about a compound and its composition. Can store information regarding
 * multiple properties of the material represented by the composition. 
 *
 * @author Logan Ward
 * @version 0.1
 */
public class CompositionEntry extends MultiPropertyEntry {
    /** Names of each element */
    protected String[] ElementNames=null;
    /** Rank of each element (used in display order) */
    protected int[] SortingOrder=null;
    /** Elements present in composition */
    protected int[] Element;
    /** Fraction of each element */
    protected double[] Fraction;
    /** Number of atoms in cell (used to convert when printing) */
    protected double NumberInCell = Double.NEGATIVE_INFINITY;
    /** Whether to print composition in HTML Format */
    public boolean HTMLFormat = false;
    
    /**
     * Allows the creation of arbitrary constructors for superclasses.
     */
    protected CompositionEntry() {};
    
    /** 
     * Make a new instance by parsing the composition of an object, provided by a string.
     * <p>First splits using the regex <code>[A-Z][^A-Z]</code> to separate each component of the alloy. 
     * This assumes that capitalization is used property. Then, splits each component into the Alphabetical part
     * (assumed to be the element name) and the numeric part (amount of element). If no numeric part is present, 1 is assumed.
     * An element can be appear more than once.
     * 
     * @param Composition Composition of entry
	 * @throws Exception If parse fails
     */
    public CompositionEntry(String Composition) throws Exception {
        this.ElementNames = LookupData.ElementNames;
		this.SortingOrder = LookupData.SortingOrder;
		
        // Parse the composition
        Map<Integer, Double> compMap = parseComposition(Composition);
        
		// Crash if nothing read
		if (compMap.isEmpty()) {
			throw new Exception("No composition was read");
		}
		
        // Store values
        this.Element =  new int[compMap.keySet().size()];
        this.Fraction = new double[Element.length];
        Iterator<Map.Entry<Integer,Double>> iter = compMap.entrySet().iterator();
        int i=0;
        while (iter.hasNext()) {
            Map.Entry<Integer,Double> entry = iter.next();
            Element[i] = entry.getKey();
            Fraction[i] = entry.getValue();
            i++;
        }
        rectifyEntry(true);
    }
    
    /**
     * Parse string containing a composition. Supports parentheses and addition compounds
     * (ex: Na<sub>2</sub>CO<sub>3</sub>-10H<sub>2</sub>O). Note, will 
     * not propertly parse addition compounds inside parentheses (ex:
     * Na<sub>2</sub>(CO<sub>3</sub>-10H<sub>2</sub>O)<sub>1</sub>).
     * 
     * @param composition String describing a composition
     * @return Map of element ID (Z-1) to amount
     * @throws Exception 
     */
    private Map<Integer, Double> parseComposition(String composition) 
            throws Exception {
        // Check for a guest structure (ex: Al2O3-2H20)
        int startGuest = composition.indexOf("-");
        int pos = composition.indexOf(183); 
        if (pos != -1 && pos < startGuest) {
            startGuest = pos;
        }

        // If this composition doesn't contain a guest either
        if (startGuest == -1) {
            // Check for ({['s
            int startParen = composition.indexOf('(');
            pos = composition.indexOf('{');
            if (pos != -1 && pos < startParen) {
                startParen = pos;
            }
            pos = composition.indexOf('[');
            if (pos != -1 && pos < startParen) {
                startParen = pos;
            }

            // If none found, parse as normal
            if (startParen == -1) {
                return parseElementAmounts(composition);
            } else {
                // First, get the part of string before the parens
                String firstPart = composition.substring(0, startParen);

                // Next, find the porition inside of the parens
                char parenType = composition.charAt(startParen);
                pos = startParen;
                int parenBalence = 1;
                while (parenBalence > 0) {
                    // Increment position
                    pos++;

                    // Check whether we are past the end of the string
                    if (pos > composition.length()) {
                        throw new Exception("Missing close paren. Start: "
                                + composition.substring(startParen, startParen + 3));
                    }

                    // Get the character at the current position
                    char curChar = composition.charAt(pos);

                    // Figure out change in paren balance
                    if (parenType == '(') {
                        if (curChar == '(') {
                            parenBalence++;
                        } else if (curChar == ')') {
                            parenBalence--;
                        }
                    } else if (parenType == '{') {
                        if (curChar == '{') {
                            parenBalence++;
                        } else if (curChar == '}') {
                            parenBalence--;
                        }
                    } else if (parenType == '[') {
                        if (curChar == '[') {
                            parenBalence++;
                        } else if (curChar == ']') {
                            parenBalence--;
                        }
                    } else {
                        throw new Exception("Unrecognized paren type: " + parenType);
                    }
                }

                // Get the multiplier of the composition inside the parens
                int endParen = pos;
                pos++;
                String mult = "";
                while (pos < composition.length()
                        && (Character.isDigit(composition.charAt(pos))
                        || composition.charAt(pos) == '.')) {
                    mult += composition.charAt(pos++);
                }
                double parenMult;
                if (mult.isEmpty()) {
                    parenMult = 1;
                } else {
                    parenMult = Double.parseDouble(mult);
                }

                // Get the portion insides of those parens, and end porition
                String insideParen = composition.substring(startParen + 1, endParen);
                String endPortion = composition.substring(pos);

                // Parse those poritions and combine results
                Map<Integer, Double> totalComp = parseElementAmounts(firstPart);
                Map<Integer, Double> addComp = parseComposition(insideParen);

                combineCompositions(totalComp, addComp, parenMult);

                // Parse the end portion
                addComp = parseComposition(endPortion);
                combineCompositions(totalComp, addComp, 1.0);

                // Done!
                return totalComp;
            }
        } else {
            // Find how many guests
            int endHost = startGuest;
            pos = startGuest + 1;
            String mult = "";
            while (pos < composition.length() && 
                    (Character.isDigit(composition.charAt(pos)) ||
                    composition.charAt(pos) == '.')) {
                mult += composition.charAt(pos++);
            }
            startGuest = pos;
            double guestMult = mult.isEmpty() ? 1.0 : Double.parseDouble(mult);
            
            // Split compound
            String hostPart = composition.substring(0, endHost);
            String guestPart = composition.substring(startGuest);
            
            // Compute them separately
            Map<Integer, Double> hostComp = parseComposition(hostPart),
                    guestComp = parseComposition(guestPart);
            combineCompositions(hostComp, guestComp, guestMult);
            
            return hostComp;
        }

        
        
    }

    /**
     * Add composition from one map to another
     * @param totalComp Composition to be added to
     * @param addComp Composition to add
     * @param multiplier Multiplier for added part
     */
    public void combineCompositions(Map<Integer, Double> totalComp,
            Map<Integer, Double> addComp, double multiplier) {
        for (Map.Entry<Integer, Double> entrySet : addComp.entrySet()) {
            Integer elem = entrySet.getKey();
            Double amount = entrySet.getValue();
            Double curAmount = totalComp.get(elem);
            
            // If totalComp contains this element
            if (curAmount != null) {
                double newAmount = curAmount + amount * multiplier;
                totalComp.put(elem, newAmount);
            } else {
                totalComp.put(elem, amount * multiplier);
            }
        }
    }

    /**
     * Given a string of elements and amounts, compute fractions of each element.
     * @param composition Composition as a string
     * @return Map of element ID (Z-1) to amount
     * @throws Exception 
     */
    private Map<Integer, Double> parseElementAmounts(String composition) throws Exception {
        // Add up all the constituents
        Matcher compMatcher = Pattern.compile("[A-Z][^A-Z]*").matcher(composition);
        Pattern elemPattern = Pattern.compile("[A-Z][a-z]?");
        Pattern fracPattern = Pattern.compile("[\\.0-9]+");
        Map<Integer, Double> compMap = new TreeMap<>();
        while (compMatcher.find()) {
            String component = compMatcher.group();
            
            // Get the element information
            Matcher elemMatcher = elemPattern.matcher(component); 
            if (! elemMatcher.find()) throw new Error("Something has gone horribly wrong!");
            String element = elemMatcher.group();
            if (element.equals("D") || element.equals("T")) element = "H";// Special case for D/T
            Integer elementNumber = Arrays.asList(ElementNames).indexOf(element);
            if (elementNumber == -1)
                throw new Exception("Element " + element + " not recognized");
            
            // Get the amount of this element
            Matcher fracMatcher = fracPattern.matcher(component);
            Double elementFraction = 1.0;
            if (fracMatcher.find()) {
                String fraction = fracMatcher.group();
                try {
                    elementFraction = Double.valueOf(fraction);
                } catch (NumberFormatException e) {
                    throw new Exception("Element amount " + fraction + " not a valid number.");
                }
            }
            
            // Skip if fraction is zero
            if (elementFraction == 0) {
                continue;
            }
            
            // Add (or update the value)
            if (compMap.containsKey(elementNumber)) {
                Double newAmount = compMap.get(elementNumber) + elementFraction;
                compMap.put(elementNumber, newAmount);
            } else 
                compMap.put(elementNumber, elementFraction);
        }
        return compMap;
    }

    /**
     * Given the element numbers (probably Z-1) and fractions, create an entry
     * @param Element Numbers of element in ElementList
     * @param Amount Amount of each element present
     */
    public CompositionEntry(int[] Element, double[] Amount) {
        setComposition(Element, Amount, true);
    }

	/**
	 * Set the composition of this entry
	 * @param elements Elements in sample (listed by index 
	 * in {@linkplain LookupData#ElementNames})
	 * @param amount Amount of each element
     * @param toSort Whether to store elements in sorted order
	 */
	final protected void setComposition(int[] elements, double[] amount, 
            boolean toSort) {
		this.Element = elements.clone();
		this.Fraction = amount.clone();
		this.ElementNames = LookupData.ElementNames;
		this.SortingOrder = LookupData.SortingOrder;
		rectifyEntry(toSort);
	}
    
    
    
    @Override@SuppressWarnings("CloneDeclaresCloneNotSupported")
    public CompositionEntry clone() {
        CompositionEntry x = (CompositionEntry) super.clone();
        // Don't reallocate the composition and property data
        x.Element = Element.clone();
        x.Fraction = Fraction.clone();
        return x;
    }

    /**
     * @return Names of elements known by this entry
     */
    public String[] getElementNameList() {
        return ElementNames;
    }

    /**
     * @return Order in which elements listed during printing
     */
    public int[] getSortingOrder() {
        return SortingOrder;
    }
    
    
    /**
     * @return List of elements contained in an entry
     */
    public int[] getElements() { 
        return Element.clone(); 
    }
    /**
     * @return Fractions of each element in array in same order as {@link #getElements()}
     */
    public double[] getFractions() {
        return Fraction.clone(); 
    }    

    /** 
     * Return the fraction of a certain element found in an entry
     * @param elem Abbreviation of element
     * @return Fraction of that element present in this entry
     */
    public double getElementFraction(String elem) {
        for (int i=0; i<ElementNames.length; i++)
            if (ElementNames[i].equalsIgnoreCase(elem))
                return getElementFraction(i);
        return 0;
    }
    /** 
     * Return the fraction of a certain element found in an entry
     * @param element Index of element (usually: AtomicNumber - 1)
     * @return Fraction of that element present in this entry
     */
    public double getElementFraction(int element) {
        for (int i=0; i < Element.length; i++)
            if (Element[i] == element)
                return Fraction[i];
        return 0;
    }
    
    @Override public int compare(Object A_obj, Object B_obj) {
        if (A_obj instanceof CompositionEntry && B_obj instanceof CompositionEntry) {
            CompositionEntry A = (CompositionEntry) A_obj;
            CompositionEntry B = (CompositionEntry) B_obj;
            // If A has more elements, it is greater. 
            if (A.Element.length != B.Element.length)
                return (A.Element.length > B.Element.length) ? 1 : -1;
            // Check which has greater element fractions
            for (int i=0; i<A.Element.length; i++)
                if (A.Element[i] != B.Element[i]) {
                    return (A.Element[i] > B.Element[i]) ? 1 : -1;
                } else if (A.Fraction[i] != B.Fraction[i])
                    return (A.Fraction[i] > B.Fraction[i]) ? 1 : -1;
            // We have concluded they are equal
            return 0;
        } else 
            return 0;
    }
    
    @Override public int hashCode() {
        if (Element.length > 0)  return (int) Arrays.hashCode(Element);
        else return 0;
    }
    
    @Override public boolean equals(Object other) {
        if (other instanceof CompositionEntry) {
            CompositionEntry obj = (CompositionEntry) other;
            if (obj.Element.length != Element.length ) { return false; }
            return (java.util.Arrays.equals(Element, obj.Element) && 
                    java.util.Arrays.equals(Fraction, obj.Fraction));
        } else return false;
    }
    
    /** 
     * Makes sure this entry is in a proper format. <B>Must be run from constructor</B>
     * <p>Performs the following operations:
     * <ul>
     * <li>Optional: Ensure Element and Fraction are sorted according to the order 
     * listed in {@linkplain #SortingOrder}.</li>
     * <li>Ensure that the sum of {@linkplain #Fraction} is equal to 1.</li>
     * <li>Set {@linkplain #NumberInCell} is equal to the original 
     * sum of {@linkplain #Fraction}.</li>
     * </ul>
     * @param toSort Whether to sort elements in "Sorting order"
     */
    final public void rectifyEntry(boolean toSort) {
        // Makes sure we have a sorting order
        if (SortingOrder == null) {
            SortingOrder = new int[ElementNames.length];
            for (int i=0; i<SortingOrder.length; i++) SortingOrder[i] = i;
        }
        
        // Simple bubble sort (we are dealing with small lists)
        if (toSort) {
            int n=Element.length, i, new_n, i_temp; 
            double d_temp;
            do {
                new_n=0;
                for (i=1; i<n; i++)
                    if ( SortingOrder[Element[i-1]]>SortingOrder[Element[i]]) {
                        i_temp=Element[i]; Element[i]=Element[i-1]; Element[i-1]=i_temp;
                        d_temp=Fraction[i]; Fraction[i]=Fraction[i-1]; Fraction[i-1]=d_temp;
                        new_n=i;
                    }
                n = new_n;
            } while (n != 0);
        }
        
        // Normalize fraction, if it has not been already
        NumberInCell = StatUtils.sum(Fraction);
        for (int i=0; i<Fraction.length; i++) Fraction[i] /= NumberInCell;
    }
        
    // Feature calculation methods
    /** Calculate the alloy mean of a property
     * @param Lookup Lookup table of elemental properties
     * @return Alloy mean of that property
     */
    public double getMean(double[] Lookup) {
        double mean=0;
        for (int i=0; i<Element.length; i++) 
            mean+=Lookup[Element[i]]*Fraction[i];
        return mean;
    }
    /** Calculate maximum difference between the properties of two elements that
     * are present in this entry
     * @param Lookup Lookup table of elemental properties
     * @return Maximum difference between 
     */
    public double getMaxDifference(double[] Lookup) {
        double min=Lookup[Element[0]], max=Lookup[Element[0]];
        for (int i=1; i<Element.length; i++) {
            min=Lookup[Element[i]]<min ? Lookup[Element[i]] : min;
            max=Lookup[Element[i]]>max ? Lookup[Element[i]] : max;
        }
        return max-min;
    }
    
    /** Calculate the mean deviation of a property from the average. This is computed
     *  as: <code> d<sub>f</sub> = sum[ x<sub>i</sub> * (f<sub>i</sub> - f<sub>mean</sub>) ] </code>
     * @param Lookup Lookup table of elemental properties
     * @return Average deviation of property from mean
     */
    public double getAverageDeviation(double[] Lookup){
        double mean=getMean(Lookup);
        return getAverageDeviation(Lookup,mean);
    }
    
    /** Calculate the mean deviation of a property from the average. This is computed
     *  as: <code> d<sub>f</sub> = sum[ x<sub>i</sub> * (f<sub>i</sub> - f<sub>mean</sub>) ] </code>
     * @param Lookup Lookup table of elemental properties
     * @param mean Mean as computed using "getMean"
     * @return Average deviation of property from mean
     */
    public double getAverageDeviation(double[] Lookup, double mean){
        double x=0;
        for (int i=0; i<Element.length; i++) 
            x+=Fraction[i]*Math.abs(Lookup[Element[i]]-mean);
        return x;
    }
    
    /**
     * Get the maximum of an elemental property
     * @param Lookup Lookup table of elemental properties
     * @return Maximum of the property amongst all elements in the composition
     */
    public double getMaximum(double[] Lookup) {
        double x=Lookup[Element[0]];
        for (int i=1; i<Element.length; i++) 
            if (Lookup[Element[i]] > x) x = Lookup[Element[i]];
        return x;
    }
    
    /**
     * Get the minimum of an elemental property
     * @param Lookup Lookup table of elemental properties
     * @return Minimum of the property amongst all elements in the compound
     */
    public double getMinimum(double[] Lookup) {
        double x=Lookup[Element[0]];
        for (int i=1; i<Element.length; i++) 
            if (Lookup[Element[i]] < x) x = Lookup[Element[i]];
        return x;
    }
    
    /**
     * Get the elemental property of the most-prevalent element. If multiple elements are
     *  equally prevalent, returns the average of their properties.
     * @param Lookup Lookup table of elemental properties
     * @return Property of the most-prevalent element
     */
    public double getMost(double[] Lookup) {
        // Special case
        if (Element.length == 1) {
            return Lookup[Element[0]];
        } 
        
        boolean[] isMost = new boolean[Element.length];
        double maxValue = -1;
        // Find the most prevalent element(s)
        for (int i=0; i<Element.length; i++)
            if (Fraction[i] > maxValue) {
                Arrays.fill(isMost, false);
                isMost[i] = true;
                maxValue = Fraction[i];
            } else if (Fraction[i] == maxValue)
                isMost[i] = true;
        // Return the average
        double output = 0;
        double count = 0;
        for (int i=0; i<isMost.length; i++) 
            if (isMost[i]){
                output += Lookup[Element[i]];
                count++;
            }
        return output / count;
    }
    
    @Override public String toString() {
        if (ElementNames == null)
            return "\"Elem_List not defined\"";
        if (HTMLFormat)
            return toHTMLString();
        else {
            String output="";
			String[] Numbers = printNumber(Fraction, NumberInCell);
			for (int i=0; i<Element.length; i++) {
				output+=ElementNames[Element[i]];
				if (Numbers[i].length() > 0) output += Numbers[i];
			}
            return output;
        }
    }
    
    @Override
    public String toHTMLString() {
        String output="";
        String[] Numbers = printNumber(Fraction, NumberInCell);
        for (int i=0; i<Element.length; i++) {
            output+=ElementNames[Element[i]];
            if (Numbers[i].length() > 0) output+="<sub>" + Numbers[i] + "</sub>";
        }
 
        return output;
    }  
    
    /**
     * Print out the number of atoms in a formula unit for each element given its fraction
     * @param fraction Fractions to be printed
     * @param NInFormulaUnit Number of atoms in a formula unit
     * @return Result formatted as a string
     */
    static public String[] printNumber(double[] fraction, double NInFormulaUnit) {
        DecimalFormat format = new DecimalFormat("#.###");
        String[] output = new String[fraction.length];
        for (int i=0; i<fraction.length; i++) {
            double expanded = fraction[i] * NInFormulaUnit;
            if (Math.abs(expanded - Math.round(expanded)) < 0.0001)
                output[i] = (int) expanded == 1 ? "" : String.format("%d", (int) expanded);
            else 
                output[i] = format.format(expanded);
        }
        return output;
    }
}
