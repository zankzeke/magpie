
package magpie.data.materials;

import vassal.data.Atom;
import vassal.data.Cell;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Represents a crystal structure.
 * @author Logan Ward
 */
public class AtomicStructureEntry extends CompositionEntry {
	/** Crystal structure */
	final private Cell Structure;
	/** Name of entry */
	private String Name;

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
		
		// Get the composition
		int[] elems = new int[structure.nTypes()];
		double[] count = new double[structure.nTypes()];
		for (int i=0; i < elems.length; i++) {
			elems[i] = ArrayUtils.indexOf(LookupData.ElementNames,
					structure.getTypeName(i));
			if (elems[i] == ArrayUtils.INDEX_NOT_FOUND) {
				throw new Exception("Element name not recognized: " + 
						structure.getTypeName(i));
			}
			// Get the number of that atom
			count[i] = (double) structure.numberOfType(i);
			// Set the atomic radius of that atom
			if (radii != null) {
				Structure.setTypeRadius(i, radii[elems[i]]);
			}
		}
        
        // Before reordering compsoitions
		setComposition(elems, count, false);
	}

    @Override
    public boolean equals(Object other) {
        if (other instanceof AtomicStructureEntry) {
            AtomicStructureEntry you = (AtomicStructureEntry) other;
            return Structure.equals(you.Structure);
        }
        return false;
    }

    @Override
    public int compareTo(Object B) {
        if (B instanceof AtomicStructureEntry) {
            AtomicStructureEntry Bobj = (AtomicStructureEntry) B;
            // First: Check for equality
            if (equals(Bobj)) return 0;
            // Second: Extreme measures
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
        }
        return super.compareTo(B); 
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
    
    
    
}
