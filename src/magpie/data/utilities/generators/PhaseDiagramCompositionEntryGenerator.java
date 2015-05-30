
package magpie.data.utilities.generators;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.utility.EqualSumCombinations;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.Combinations;

/**
 * Generate composition entries at many points in many phase diagrams.
 * 
 * <p>Has two different ways of selecting compositions within phase diagrams
 * 
 * <ol>
 * <li><b>Even Spacing</b>: Compositions are selected as to be evenly-spaced throughout
 * the phase diagram (e.g. A0.2B0.2C0.6, A0.4B0.2C0.4, etc.). This method is most appropriate
 * for alloy systems.
 * <li><b>Simple Fractions</b>: Compositions with the smallest denominator are selected.
 * (e.g ABC, A2C, B2C, etc.). This method is most appropriate for phase diagrams that represent
 * ordered crystalline compounds.
 * 
 * </ol>
 * 
 * <usage><b>Usage</b>: &lt;min order&gt; &lt;max order&gt; [-alloy|-crystal] &lt;number&gt; &lt;elements...&gt;
 * <br><pr><i>min order</i>: Minimum number of constituents
 * <br><pr><i>max order</i>: Maximum number of constituents
 * <br><pr><i>-alloy</i>: Generate evenly-spaced compositions
 * <br><pr><i>-crystal</i>: Generate fractional compositions with small denominators (i.e. AB2, or A2B5)
 * <br><pr><i>number</i>: Either desired maximum spacing [-alloy], or maximum number of
 * of atoms in the formula unit [-crystal]
 * <br><pr><i>elements</i>: List of elements to include in phase diagrams</usage>
 * 
 * @author Logan Ward
 */
public class PhaseDiagramCompositionEntryGenerator extends BaseEntryGenerator {
    /** List of elements to use (id is Z-1) */
    protected List<Integer> Elements;
    /** Minimum number of constituents */
    protected int MinOrder = 1;
    /** Maximum number of constituents */
    protected int MaxOrder = 1;
    /** Whether to use even spacing or small integers */
    protected boolean evenSpacing = true;
    /** Either number of stops in each direction or max denominator */
    protected int size = 3;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int minOrder, maxOrder, size;
        Set<String> elements;
        try {
            minOrder = Integer.parseInt(Options.get(0).toString());
            maxOrder = Integer.parseInt(Options.get(1).toString());
            String tag = Options.get(2).toString();
            if (tag.equalsIgnoreCase("-alloy")) {
                setEvenSpacing(true);
                double spacing = Double.parseDouble(Options.get(3).toString());
                size = (int) Math.ceil(100.0 / spacing) + 1;
            } else if (tag.equalsIgnoreCase("-crystal")) {
                setEvenSpacing(false);
                size = Integer.parseInt(Options.get(3).toString());
            } else {
                throw new Exception();
            }
            elements = new TreeSet<>();
            for (Object word : Options.subList(4, Options.size())) {
                elements.add(word.toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setOrder(minOrder, maxOrder);
        setSize(size);
		setElementsByName(elements);
    }

	@Override
	public String printUsage() {
		return "Usage: <min order> <max order> <-alloy|-crystal> <size> [<elements...>]";
	}

    /**
     * Define the lit of elements included in the phase diagrams.
     * @param Elements List of elements by index (Z-1)
     */
    public void setElementsByIndex(Set<Integer> Elements) {
        this.Elements = new ArrayList<>(Elements);
    }
    
    /**
     * Define the lit of elements included in the phase diagrams.
     * @param Elements List of elements by name (ignores elements that don't match)
     */
    public void setElementsByName(Set<String> Elements) {
        this.Elements = new ArrayList<>(Elements.size());
        for (String name : Elements) {
            int id = ArrayUtils.indexOf(LookupData.ElementNames, name);
            if (id >= 0) {
                this.Elements.add(id);
            }
        }
    }

    /** 
     * Define the order of generated phase diagrams
     * @param minOrder Minimum number of constituents
     * @param maxOrder Maximum number of constituents
     * @throws java.lang.Exception
     */
    public void setOrder(int minOrder, int maxOrder) throws Exception {
        if (minOrder < 1 || maxOrder < 1) {
            throw new Exception("Order must be greater than zero");
        }
        this.MinOrder = minOrder;
        this.MaxOrder = maxOrder;
    }

    /**
     * Set whether to use evenly-spaced compositions (or low-denominator).
     * @param evenSpacing Whether to use even spacing
     */
    public void setEvenSpacing(boolean evenSpacing) {
        this.evenSpacing = evenSpacing;
    }

    /**
     * Define the number of points per binary diagram (in using even spacing) or
     *  the maximum denominator (for low-denominator)
     * @param size Desired size parameter
     * @throws java.lang.Exception
     */
    public void setSize(int size) throws Exception {
        if (size < 2) {
            throw new Exception("Size must be greater than 1");
        }
        this.size = size;
    }

	@Override
	public List<BaseEntry> generateEntries() {
		// Get list of compositions at which to create entries
		Map<Integer, List<double[]>> compositions;
		compositions = evenSpacing ? generateAlloyCompositions() : 
				generateCrystalCompositions();
		
		// Generate entries
		List<BaseEntry> output = new LinkedList<>();
		for (Map.Entry<Integer, List<double[]>> entrySet : compositions.entrySet()) {
			Integer or = entrySet.getKey();
			List<double[]> comps = entrySet.getValue();
			for (int[] elemID : new Combinations(Elements.size(), or)) {
				int[] elems = new int[or];
				for (int i=0; i<or; i++) {
					elems[i] = Elements.get(elemID[i]);
				}
				for (double[] comp : comps) {
					CompositionEntry newEntry = new CompositionEntry(elems, comp);
					output.add(newEntry);
				}
			}
		}
		return output;
	}
	
	/**
	 * Generate evenly-spaced compositions. Generates compositions for all diagrams
	 *  up to the user-specified {@link #MinOrder}.
	 * 
	 * <p>For example: If the user wants ternary diagrams with a minimum spacing of 0.25, 
	 * this code will generate the following map:
	 * <br>1 -> ([1.0])
	 * <br>2 -> ([0.25, 0.75], [0.5, 0.5], [0.75, 0.25])
	 * <br>3 -> ([0.5, 0.25, 0.25], [0.25, 0.5, 0.25], [0.25, 0.25, 0.5])
	 * 
	 * @return Map of number of elements -> Possible compositions
	 */
	protected Map<Integer, List<double[]>> generateAlloyCompositions() {
		Map<Integer, List<double[]>> output = new TreeMap<>();
		
		// Add in diagrams of greater orders
		for (int o=MinOrder; o<=MaxOrder; o++) {
            if (o == 1) {
                List<double[]> toAdd = new LinkedList<>();
                toAdd.add(new double[]{1.0});
                output.put(1, toAdd);
                continue;
            }
			List<double[]> toAdd = new LinkedList<>();
			for (int[] compI : new EqualSumCombinations(size-1, o)) {
				if (ArrayUtils.contains(compI, 0)) {
					continue; // Don't add compositions form a lower-order diagram
				}
				double[] comp = new double[o];
				for (int i=0; i<o; i++) {
					comp[i] = (double) compI[i] / (double) (size-1);
				}
				toAdd.add(comp);
			}
			output.put(o, toAdd);
		}
		return output;
	}
	/**
	 * Generate compositions with small denominators. Generates compositions for all diagrams
	 *  up to the user-specified {@link #MinOrder}.
	 * 
	 * <p>For example: If the user wants ternary diagrams with a maximum denominator of 3, 
	 * this code will generate the following map:
	 * <br>1 -> ([1])
	 * <br>2 -> ([1/3, 2/3], [1/2, 1/2], [2/3, 1/3])
	 * <br>3 -> ([1/3, 1/3, 1/3])
	 * 
	 * @return Map of number of elements -> Possible compositions
	 */
	protected Map<Integer, List<double[]>> generateCrystalCompositions() {
		Map<Integer, List<double[]>> output = new TreeMap<>();
		
		// Add in diagrams of greater orders
		for (int o=MinOrder; o<=MaxOrder; o++) {
            if (o == 1) {
                List<double[]> toAdd = new LinkedList<>();
                toAdd.add(new double[]{1.0});
                output.put(1, toAdd);
                continue;
            }
			List<double[]> toAdd = new LinkedList<>();
            List<double[]> reducedExamples = new LinkedList<>();
			for (int d=o; d<=size; d++) {
				for (int[] compI : new EqualSumCombinations(d, o)) {
                    // Don't add compositions form a lower-order diagram
					if (ArrayUtils.contains(compI, 0)) {
						continue; 
					}
                    // Convert to double array
					double[] comp = new double[o];
                    double[] redComp = new double[o];
					for (int i=0; i<o; i++) {
						comp[i] = (double) compI[i];
                        redComp[i] = comp[i] / d;
					}
                    
                    // Check if this composition is already represented 
                    boolean wasFound = false;
                    for (double[] ex : reducedExamples) {
                        if (Arrays.equals(ex, redComp)) {
                            wasFound = true;
                            break;
                        }
                    }
                    if (! wasFound) {
                        toAdd.add(comp);
                        reducedExamples.add(redComp);
                    }
				}
			}
			output.put(o, toAdd);
		}
		return output;
	}
}
