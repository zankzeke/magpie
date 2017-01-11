
package magpie.data.materials;

import java.util.Map;
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
public class AtomicStructureEntry extends CompositionEntry {
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
	 * @throws java.lang.Exception 
	 */
	public AtomicStructureEntry(Cell structure, String name, double[] radii) throws Exception {
		this.Structure = structure;
		this.Name = name;
		
		// Make sure structure has atoms on it
		if (structure.nAtoms() == 0) {
			throw new Exception("Cannot handle blank crystal structures");
		}
		
        // Store the radii
        Radii = radii;
        
        // Compute the composition of this crystal.
		computeComposition();
	}

    /**
     * Compute the composition of this crystal.
     * @throws Exception If the composition fails to parse
     */
    final protected void computeComposition() throws Exception {
        // Get the composition
        int[] elems = new int[Structure.nTypes()];
        double[] count = new double[Structure.nTypes()];
        for (int i=0; i < elems.length; i++) {
            elems[i] = ArrayUtils.indexOf(LookupData.ElementNames,
                    Structure.getTypeName(i));
            if (elems[i] == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Exception("Element name not recognized: " +
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
     * @throws java.lang.Exception If composition fails to parse
     */
    public AtomicStructureEntry replaceElements(Map<String,String> replacements)
            throws Exception {
        // Create new entry
        AtomicStructureEntry newEntry = clone();
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

    @Override
    public AtomicStructureEntry clone() {
        AtomicStructureEntry x = (AtomicStructureEntry) super.clone(); 
        x.Structure = Structure.clone();
        return x;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AtomicStructureEntry) {
            AtomicStructureEntry you = (AtomicStructureEntry) other;
            return Structure.equals(you.Structure) && super.equals(other);
        }
        return false;
    }

    @Override
    public int compare(Object A, Object B) {
        if (B instanceof AtomicStructureEntry) {
            AtomicStructureEntry Bobj = (AtomicStructureEntry) B;
            AtomicStructureEntry Aobj = (AtomicStructureEntry) A;

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
                }
            }
            throw new Error("These entries were supposed to be unequal");
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
            Voronoi.analyzeStructre(Structure);
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
        return Name + ":" + comp;
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
        }
        
        return output;
    }
}
