
package magpie.data.materials;

import java.util.*;

import magpie.utility.CartesianSumGenerator;
import org.apache.commons.collections4.iterators.PermutationIterator;
import vassal.data.Atom;
import vassal.data.Cell;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;
import vassal.analysis.VoronoiCellBasedAnalysis;
import vassal.io.VASP5IO;

/**
 * Represents a crystal structure.
 * @author Logan Ward
 */
public class CrystalStructureEntry extends CompositionEntry {
	/** Crystal structure */
	private Cell Structure;
	/** Name of entry */
	private String Name;
    /** Link to atomic radii array */
    private double[] Radii;
    /** Voronoi tessellation of this structure */
    private VoronoiCellBasedAnalysis Voronoi;

	/**
	 * Create an entry given its crystal structure
	 * @param structure Structure of this entry
	 * @param radii Radii to use for each element (null to leave radii unchanged)
	 * @param name Name of structure (used for debugging purposes)
	 */
	public CrystalStructureEntry(Cell structure, String name, double[] radii) {
		this.Structure = structure;
		this.Name = name;
		
		// Make sure structure has atoms on it
		if (structure.nAtoms() == 0) {
			throw new IllegalArgumentException("Cannot handle blank crystal structures");
		}
		
        // Store the radii
        Radii = radii;
        
        // Compute the composition of this crystal.
		computeComposition();
	}

    /**
     * Compute the composition of this crystal.
     */
    private void computeComposition() {
        // Get the composition
        int[] elems = new int[Structure.nTypes()];
        double[] count = new double[Structure.nTypes()];
        for (int i=0; i < elems.length; i++) {
            elems[i] = ArrayUtils.indexOf(LookupData.ElementNames,
                    Structure.getTypeName(i));
            if (elems[i] == ArrayUtils.INDEX_NOT_FOUND) {
                throw new RuntimeException("Element name not recognized: " +
                        Structure.getTypeName(i));
            }
            // Get the number of that atom
            count[i] = (double) Structure.numberOfType(i);
            // Set the atomic radius of that atom
            if (Radii != null) {
                Structure.setTypeRadius(i, Radii[elems[i]]);
            }
        }
        
        // Before reordering compsoitions
        setComposition(elems, count, false);
    }
       
    /**
     * Create a new entry by replacing elements on this entry
     * @param replacements Map of elements to replace. Key: Old element, Value: New element
     * @return New entry
     */
    public CrystalStructureEntry replaceElements(Map<String,String> replacements) {
        // Create new entry
        CrystalStructureEntry newEntry = clone();
        newEntry.Structure.replaceTypeNames(replacements);
        newEntry.Structure.mergeLikeTypes();
        newEntry.computeComposition();
        
        // If Voronoi tessellation has already been computed, create a tool
        //  for the new entry w/o recomputing the tessellation
        if (Voronoi != null && (Structure.nTypes() != newEntry.Structure.nTypes())) {
            newEntry.Voronoi = null;
        }
        return newEntry;
    }

    /**
     * Redecorate a structure with a new composition. Creates a list of possible structures given a new composition.
     *
     * <p>Note that this operation may create structures which have different atomic positions, but are symmetrically
     * equivalent.</p>
     *
     * @param composition Desired composition
     * @return List of possible structures. Empty list if stoichiometry of input composition does not match up with structure
     */
    public List<CrystalStructureEntry> changeComposition(CompositionEntry composition) {
        // Check whether the composition of the structure matches
        double[] myStoich = getFractions();
        double[] desiredStoich = composition.getFractions();

        if (myStoich.length != desiredStoich.length) {
            return new LinkedList<>();
        }

        Arrays.sort(myStoich);
        Arrays.sort(desiredStoich);
        for (int i=0; i<myStoich.length; i++) {
            if (Math.abs(myStoich[i] - desiredStoich[i]) > 1e-6) {
                return new LinkedList<>();
            }
        }

        // Get the types of atoms that have the same proportion
        //   This will allow us later to determine the mapping between elements in the desired composition
        //    that have the same proportion as element in our crystal structure.
        //
        //  Example: Our crystal structure has composition ABC3, and we want to give it the composition SrTiO3.
        //    The Sr and Ti could map to sites A and B, and O only matches site C
        //
        double[] desiredFractions = composition.getFractions();
        int[] desiredElements = composition.getElements();
        SortedMap<Integer, List<String>> myTypes = new TreeMap<>();
        SortedMap<Integer, List<String>> desiredTypes = new TreeMap<>();
        for (int i=0; i<Structure.nTypes(); i++) {
            int count = Structure.numberOfType(i);

            if (myTypes.containsKey(count)) {
                // Add this element to the list of those with this number of atoms
                myTypes.get(count).add(Structure.getTypeName(i));
            } else {
                // Initialize both the list holding types with this number of atoms, and
                //   the list of elements in the desired composition that could match to this type
                List<String> temp = new ArrayList<>(desiredElements.length);
                temp.add(Structure.getTypeName(i));
                myTypes.put(count, temp);

                List<String> matchingElements = new ArrayList<>(desiredElements.length);
                double myFraction = (double) count / Structure.nAtoms();
                for (int j=0; j<desiredFractions.length; j++) {
                    if (Math.abs(myFraction - desiredFractions[j]) < 1e-6) {
                        matchingElements.add(LookupData.ElementNames[desiredElements[j]]);
                    }
                }
                if (matchingElements.isEmpty()) {
                    throw new RuntimeException("Did not find matching elements");
                }
                desiredTypes.put(count, matchingElements);
            }
        }

        //  Generate all permutations of elements with the same number of atoms in this structure
        //    For our ABC3 example: A and B have the same fraction. This code will produce A,B and B,A
        List<Collection<List<String>>> myPermutations = new ArrayList<>(myTypes.size());
        for (List<String> bySize : myTypes.values()) {
            List<List<String>> permutations = new ArrayList<>();
            PermutationIterator<String> permIter = new PermutationIterator<>(bySize);
            while (permIter.hasNext()) {
                permutations.add(permIter.next());
            }
            myPermutations.add(permutations);
        }

        // Cache list of new types
        List<List<String>> newPermutations = new ArrayList<>(desiredTypes.values());

        // Generate the new structures
        //
        //  We now have a mapping of all permutations of elements with the same number of atoms, and a list of
        //   which element types in our desired composition and fill those sites. This part generates all unique mappings
        //   of 'elements in current structure' to 'equivalent elements in desired compositions', and uses that mapping
        //   to generate new structures.
        //
        // Example: For the ABC3 and SrTiO3 case, our unique mappings are (A->Sr, B->Ti, C->O) and (A->Ti, B->Sr, C->O)
        List<CrystalStructureEntry> output = new ArrayList<>();
        for (List<List<String>> permutation : new CartesianSumGenerator<>(myPermutations)) {
            // Prepare the mapping
            Map<String, String> elementMapping = new HashMap<>();
            for (int i=0; i<newPermutations.size(); i++) {
                for (int j=0; j<permutation.get(i).size(); j++) {
                    elementMapping.put(permutation.get(i).get(j), newPermutations.get(i).get(j));
                }
            }

            // Make the new structure
            output.add(replaceElements(elementMapping));
        }

        return output;
    }

    @Override
    public CrystalStructureEntry clone() {
        CrystalStructureEntry x = (CrystalStructureEntry) super.clone();
        x.Structure = Structure.clone();
        return x;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof CrystalStructureEntry) {
            CrystalStructureEntry you = (CrystalStructureEntry) other;
            return Structure.equals(you.Structure) && super.equals(other);
        }
        return false;
    }

    @Override
    public int compare(Object A, Object B) {
        if (B instanceof CrystalStructureEntry) {
            CrystalStructureEntry Bobj = (CrystalStructureEntry) B;
            CrystalStructureEntry Aobj = (CrystalStructureEntry) A;

            // First: Check for equality
            if (Aobj.equals(Bobj)) return 0;

            // Second: Check the composition / attributes
            int superComp = super.compare(Aobj, Bobj);
            if (superComp != 0) {
                return superComp;
            }

            // Third: Extreme measures
            if (Structure.nAtoms() != Bobj.Structure.nAtoms()) {
                return Integer.compare(Structure.nAtoms(), Bobj.Structure.nAtoms());
            }
            if (Structure.nTypes() != Bobj.Structure.nTypes()) {
                return Integer.compare(Structure.nTypes(), Bobj.Structure.nTypes());
            }
            double[][] myBasis = Structure.getBasis(), 
                    yourBasis = Bobj.Structure.getBasis();
            for (int i=0; i<3; i++) {
                for (int j=0; j<3; j++) {
                    int c = Double.compare(myBasis[i][j], yourBasis[i][j]);
                    if (c != 0) return c;
                }
            }
            for (int i=0; i<Structure.nAtoms(); i++) {
                Atom myAtom = Structure.getAtom(i), 
                        yourAtom = Bobj.Structure.getAtom(i);
                if (myAtom.getType() > yourAtom.getType()) {
                    return Integer.compare(myAtom.getType(), yourAtom.getType());
                }
                double[] myPos = myAtom.getPosition(), 
                        yourPos = yourAtom.getPosition();
                for (int k=0; k<3; k++) {
                    int c = Double.compare(myPos[k], yourPos[k]);
                    if (c != 0) {
                        return c;
                    }
                }
            }
            throw new RuntimeException("These entries were supposed to be unequal");
        } else {
            return super.compare(A, B);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ Structure.hashCode();
    }

    /**
     * Get link to structure
     * @return Structure this entry represents
     */
    public Cell getStructure() {
        return Structure;
    }

    /**
     * Get the composition of this entry.
     * @return The composition as a {@linkplain CompositionEntry} object
     */
    public CompositionEntry getComposition() {
        return new CompositionEntry(getElements(), getFractions());
    }

    /** 
     * Get name of this entry
     * @return Name
     */
    public String getName() {
        return Name;
    }
    
    /**
     * Compute the Voronoi tessellation of this structure.
     * @return Tool used to query properties of the tessellation
     */
    public VoronoiCellBasedAnalysis computeVoronoiTessellation() throws Exception {
        if (Voronoi == null) {
            Voronoi = new VoronoiCellBasedAnalysis(false);
            Voronoi.analyzeStructure(Structure);
        } else if (! Voronoi.tessellationIsConverged()) {
            throw new Exception("Tessellation did not converge");
        }
        return Voronoi;
    }
    
    /**
     * Clear out the representations used when computing attributes.
     */
    public void clearRepresentations() {
        Voronoi = null;
    }

    @Override
    public void reduceMemoryFootprint() {
        clearRepresentations();
        super.reduceMemoryFootprint(); 
    }

    @Override
    public String toString() {
        String comp = super.toString();
        return Name + "_" + comp;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject output = super.toJSON();
        
        // Add in name and poscar
        output.put("name", Name);
        output.put("composition", super.toString());
        try {
            output.put("poscar", new VASP5IO().printStructure(Structure));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return output;
    }
}
