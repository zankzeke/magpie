package magpie.attributes.generators.crystal;

import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.StatUtils;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute attributes based on the difference in elemental properties between
 * neighboring atoms. For an atom, its "local property difference" is computed by:
 * 
 * <br>\(\frac{\sum_{n}{f_{n} * \left|p_{atom}-p_{n}\right|}}{\sum_{n}{f_n}}\)
 * 
 * <p>where \(f_n\) is the area of the face associated with neighbor n,
 * \(p_{atom}\) the the elemental property of the central atom, and 
 * \(p_n\) is the elemental property of the neighbor atom.
 * 
 * <p>For shells past the 1st nearest neighbor shell, the neighbors are identified
 * by finding all of the unique faces on the outside of the polyhedron formed by 
 * the previous neighbor shell. This list of faces will faces corresponding to all 
 * of the atoms in the desired shell and the total weight for each atom is
 * defined by the total area of the faces corresponding to that atom (there
 * may be more than one).
 *
 * <p>By default, this class considers the only the 1st nearest neighbor shell.
 * 
 * <p>This parameter is computed for all elemental properties stored in 
 * {@linkplain CrystalStructureDataset#ElementalProperties}.
 * 
 * <usage><p><b>Usage</b>: &lt;shells...&gt;
 * <br><pr><i>shells</i>: Which nearest neighbor shells to consider
 * </usage>
 * 
 * @author Logan Ward
 */
public class LocalPropertyDifferenceAttributeGenerator extends BaseAttributeGenerator {
    /** Elemental properties used to generate attributes */
    private List<String> ElementalProperties = null;
    /** Shells to consider. */
    private final Set<Integer> Shells = new TreeSet<>();
    /** Property name */
    protected String AttrName = "NeighDiff";
    /** Property description (used in description output) */
    protected String AttrDescription = "difference between the elemental properties between an atom and neighbors";

    /**
     * Create Default attribute generator. Will consider 1st and 2nd shells
     */
    public LocalPropertyDifferenceAttributeGenerator() {
        Shells.add(1);
    }
    
    /**
     * Create attribute generator that considers a certain list of shells.
     * @param shells List of shells to be considered
     */
    public LocalPropertyDifferenceAttributeGenerator(int... shells) {
        for (int shell : shells) {
            addShell(shell);
        }
    }
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // Parse incoming ints
        List<Integer> shells = new ArrayList<>(Options.size());
        try {
            for (Object opt : Options) {
                shells.add(Integer.parseInt(opt.toString()));
            }
        } catch (NumberFormatException e) {
            throw new Exception(printUsage());
        }
        
        // Define settings
        clearShells();
        addShells(shells);
    }
    
    /**
     * Clear list of shells to use when computing attributes
     */
    public void clearShells() {
        Shells.clear();
    }

    /**
     * Add shell to list of used when computing attributes
     * @param shell Index of nearest neighbor shell
     */
    public final void addShell(int shell) {
        if (shell <= 0) {
            throw new IllegalArgumentException("Shell must be > 0");
        }
        Shells.add(shell);
    }
    
    /**
     * Add several shells to list of used when computing attributes
     * @param shells Indices of nearest neighbor shells
     */
    public void addShells(Collection<Integer> shells) {
        for (int shell : shells) {
            addShell(shell);
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check to make sure dataset hold crystal structures
        if (! (data instanceof CrystalStructureDataset)) {
            throw new Exception("Dataset doesn't contain crystal structures");
        }
        CrystalStructureDataset dPtr = (CrystalStructureDataset) data;
        
        // Create attribute names
        ElementalProperties = dPtr.getElementalProperties();
        List<String> newAttr = createNames();
        data.addAttributes(newAttr);
        
        // Compute attributes
        double[] temp = new double[newAttr.size()];
        for (BaseEntry ptr : data.getEntries()) {
            // Get the Voronoi tessellation
            AtomicStructureEntry entry = (AtomicStructureEntry) ptr;
            VoronoiCellBasedAnalysis voro;
            try {
                voro = entry.computeVoronoiTessellation();
            } catch (Exception e) {
                Arrays.fill(temp, Double.NaN); // If tessellation fails
                entry.addAttributes(temp);
                continue;
            }
            
            // Compute the attributes
            int pos = 0;
            
            // Get the elements corresponding to each type
            int[] elemIndex = new int[entry.getStructure().nTypes()];
            for (int i=0; i<elemIndex.length; i++) {
                elemIndex[i] = ArrayUtils.indexOf(LookupData.ElementNames, 
                        entry.getStructure().getTypeName(i));
            }
            double[] propValues = new double[elemIndex.length];
            
            // Loop through each shell
            for (Integer shell : Shells) {
                // Get face information for each shell
                Pair<int[][], double[][]> faceInfo = 
                        voro.getNeighborShellWeights(shell);
                
                // Loop through each elemental property
                for (String prop : ElementalProperties) {
                    // Get properties for elements in this structure
                    double[] lookupTable;
                    try {
                        lookupTable = dPtr.getPropertyLookupTable(prop);
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                    for (int i=0; i<propValues.length; i++) {
                        propValues[i] = lookupTable[elemIndex[i]];
                    }

                    // Compute neighbor differences
                    double[] neighDiff = getAtomProperties(voro, faceInfo, propValues);

                    // Compute statistics
                    temp[pos++] = StatUtils.mean(neighDiff);
                    double[] meanDeviation = neighDiff.clone();
                    for (int i=0; i<meanDeviation.length; i++) {
                        meanDeviation[i] = Math.abs(meanDeviation[i] 
                                - temp[pos - 1]);
                    }
                    temp[pos++] = StatUtils.mean(meanDeviation);
                    temp[pos++] = StatUtils.min(neighDiff);
                    temp[pos++] = StatUtils.max(neighDiff);
                    temp[pos++] = StatUtils.max(neighDiff) - StatUtils.min(neighDiff);
                }
            }
            
            // Add to the entry
            entry.addAttributes(temp);
        }
    }

    /**
     * Provided the Voronoi tessellation and properties of each atom type, 
     * compute the properties of a certain neighbor cell for each atom.
     * 
     * <p>For {@linkplain LocalPropertyDifferenceAttributeGenerator}, this 
     * produces the local property difference for each atom.
     * @param voro Voronoi tessellation
     * @param faceInfo Areas and types on outside of each face for desired shell. Computed
     * using {@link VoronoiCellBasedAnalysis#getNeighborShellWeights(int)}
     * @param propValues Properties of each atom type
     * @return Properties of each atom
     */
    protected double[] getAtomProperties(VoronoiCellBasedAnalysis voro,
            Pair<int[][],double[][]> faceInfo,
            double[] propValues) {
        // Compute the neighbor differences for each atom
        double[] neighDiff;
        neighDiff = voro.neighborPropertyDifferences(propValues,
                faceInfo.getRight(), faceInfo.getLeft());
        return neighDiff;
    }

    /**
     * Create names for the attributes
     * @return 
     */
    protected List<String> createNames() {
        List<String> newAttr;
        newAttr = new ArrayList<>();
        for (Integer shell : Shells) {
            for (String prop : ElementalProperties) {
                newAttr.add("mean_" + AttrName + "_shell" + shell + "_" + prop);
                newAttr.add("var_" + AttrName + "_shell" + shell + "_" + prop);
                newAttr.add("min_" + AttrName + "_shell" + shell + "_" + prop);
                newAttr.add("max_" + AttrName + "_shell" + shell + "_" + prop);
                newAttr.add("range_" + AttrName + "_shell" + shell + "_" + prop);
            }
        }
        return newAttr;
    }
    
    

    @Override
    public String printDescription(boolean htmlFormat) {
            String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Print out number of attributes
        output += " (" + (ElementalProperties.size() * Shells.size() * 4) + ") ";
        
        // Get the shell counts
        String shellList = "";
        if (Shells.size() == 1) {
            shellList = Shells.iterator().next() + " nearset neighbor shell";
        } else {
            Iterator<Integer> iter = Shells.iterator();
            shellList = iter.next().toString();
            do {
                Integer num = iter.next();
                if (iter.hasNext()) {
                    shellList += ", ";
                } else {
                    shellList += " and ";
                }
                shellList += num.toString();
            } while (iter.hasNext());
            shellList += " nearest neighbor shells";
        }
        
        // Print out description
        output += "Mean, maximum, minium, range, and mean absolute deviation in the "
                + AttrDescription 
                + " in the " + shellList
                + " for " + ElementalProperties.size() + " elemental properties:\n";
        
        // Print out elemental properties
        if (htmlFormat) {
            output += "<br>";
        }
        boolean started = false;
        for (String prop : ElementalProperties) {
            if (started) {
                output += ", ";
            }
            output += prop;
            started = true;
        }
        
        return output;
    }
    
    
}
