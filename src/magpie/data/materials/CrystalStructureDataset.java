
package magpie.data.materials;

import vassal.analysis.SimpleStructureAnalysis;
import vassal.analysis.VoronoiCellBasedAnalysis;
import java.io.BufferedReader;
import java.util.*;
import java.io.File;
import java.io.FileReader;
import vassal.data.Cell;
import vassal.io.VASP5IO;
import magpie.data.BaseEntry;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Holds a entries that represent structures.
 * 
 * <p><b>How to Import Files:</b>
 * 
 * <p>Rather than importing data from a single file with the 
 * {@linkplain #importText(java.lang.String, java.lang.Object[]) } operation, 
 * this class imports all the files in a given directory. These files must be
 * VASP 5 POSR files. Any files that are not in this format or simply fail
 * to be parsed properly will be ignored.
 * 
 * <p>In order to provide properties about these structures, add a "properties.txt"
 * file to the POSCAR-containing directory. This file should be in the following format
 * 
 *  <p>filename &lt;property #1 name&gt; &lt;...&gt;
 * <br>&lt;name of POSCAR&gt; &lt;property #1 of that structure&gt; &lt;...&gt;
 * 
 * <p>This file follows similar rules to that of {@linkplain CompositionDataset}
 * 
 * <p><b>Crystal Structure Attributes</b>
 * 
 * <p>Entries in this dataset have two different kinds of attributes:
 * 
 * <ol>
 * <li>Attributes based on only composition. See: {@linkplain CompositionDataset}
 * <li>Attributes based on the crystal structure. 
 * </ol>
 * 
 * <p>Attributes based on the crystal structure fall in to two categories as well.
 *
 * <p>First, there are attributes that do not depend on lattice parameters and should
 * be similar for all crystals based on the same prototype (e.g. number of 
 * atoms, coordination number statistics). The intention for these attributes
 * is that they can be used before determining the equilibrium lattice parameter
 * or atom positions in a crystal using a tool like DFT. For example, this would
 * be useful if you wanted to predict the formation energy of a hypothetical 
 * structure.
 * 
 * <p>If atomic positions and lattice parameter are known, a second set of 
 * structural attributes can be used. These attributes include measures such
 * as bond distances and packing efficiency.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * @author Logan Ward
 */
public class CrystalStructureDataset extends CompositionDataset {

	/**
	 * Import all structure files in a directory.
	 * @param directory Directory containing VASP5-formatted files
	 * @param options No options
	 * @throws Exception 
	 */
	@Override
	public void importText(String directory, Object[] options) throws Exception {
		// Get all files in that directory
        File dir = new File(directory);
        if (! dir.isDirectory()) {
            throw new Exception("Expected an input directory, not file");
        }
        File[] files = dir.listFiles();
		
		// Read in properties
		Map<String, double[]> properties = new TreeMap<>();
		for (File file : files) {
			if (! file.getName().equals("properties.txt")) {
				continue;
			}
			BufferedReader fp = new BufferedReader(new FileReader(file));
			String line = fp.readLine();
			importPropertyNames(line);
			while (true) {
				line = fp.readLine();
				if (line == null) break;
				String[] words = line.split("\\s+");
				double[] props = new double[NProperties()];
				Arrays.fill(props, Double.NaN);
				for (int i=1; i<words.length; i++) {
					try {
						props[i-1] = Double.parseDouble(words[i]);
					} catch (NumberFormatException e) {
						// Do nothing
					}
				}
				properties.put(words[0], props);
			}
			fp.close();
		}
		
		// Get radii of each element
		double[] radii;
		try {
			radii = getPropertyLookupTable("CovalentRadius");
		} catch (Exception e) {
			radii = null;
		}
        
        // Import each file
        List<CrystalStructureEntry> toAdd = new LinkedList<>();
        for (File file : files) {
            try {
                Cell strc = new VASP5IO().parseFile(file.getAbsolutePath());
                CrystalStructureEntry entry = 
						new CrystalStructureEntry(strc, file.getName(), radii);
				// See if we have properties for this entry
				if (properties.containsKey(file.getName())) {
					entry.setMeasuredProperties(properties.get(file.getName()));
				}
                toAdd.add(entry);
            } catch (Exception e) {
                // Do nothing
            }
        }
        
        // Add all to dataset
        Entries.addAll(toAdd);
	}

    @Override
    public CrystalStructureEntry getEntry(int index) {
        return (CrystalStructureEntry) super.getEntry(index); 
    }

    @Override
    protected void calculateAttributes() {
        // Generate simple attributes
        generateSimpleStructuralAttributes();
        
        // Generate Voronoi-based attributes
        generateVoronoiCellBasedAttributes();
        
        // Calculate composition-based attributes
        super.calculateAttributes(); 
    }
    
    /**
     * Compute attributes based on simple measures of the structure.
     * 
     * <p>Current list:
     * <ol>
     * <li>Number of atoms
     * </ol>
     */
    private void generateSimpleStructuralAttributes() {
        int attributesAdded = AttributeName.size();
        // Add names
        AttributeName.add("NAtoms");
        
        // Evaluate each entry
        attributesAdded = AttributeName.size() - attributesAdded;
        double[] newAttr = new double[attributesAdded];
        for (BaseEntry entry : Entries) {
            CrystalStructureEntry ptr = (CrystalStructureEntry) entry;
            
            // Call analysis
            SimpleStructureAnalysis tool = new SimpleStructureAnalysis();
            try {
                tool.analyzeStructre(ptr.getStructure());
            } catch (Exception e) {
                throw new Error(e);
            }
            
            // Get results
            newAttr[0] = ptr.getStructure().nAtoms();
            entry.addAttributes(newAttr);
        }
    }
    
    /**
     * Compute attributes based on the Voronoi tessellation of a crystal.
     * 
     * <p>Current attributes:
     * <ol>
     * <li>Mean coordination number of all atoms
     * <li>Variance in coordination number
     * <li>Minimum coordination number 
     * <li>Maximum coordination number
     * <li>Variance in cell size as fraction of entire cell
     * <li>Minimum cell size
     * <li>Maximum cell size
     * <li>Number of unique coordination polyhedron shapes
     * <li>Maximum packing efficiency
     * <li>Mean Warren-Cowley ordering parameter magnitude for 1st - 3rd shells
     * <li>Dissimilarity between cell shapes and BCC/FCC/SC
     * <li>Statistics regarding differences between elemental properties of neighbors of atoms
     * </ol>
     */
    private void generateVoronoiCellBasedAttributes() {
        int attributesAdded = AttributeName.size();
        // Add names
        AttributeName.add("mean_Coordination");
        AttributeName.add("var_Coordination"); 
		AttributeName.add("min_Coordination"); 
		AttributeName.add("max_Coordination"); 
		AttributeName.add("var_CellVolumeFraction");
        AttributeName.add("min_CellVolumeFraction");
        AttributeName.add("max_CellVolumeFraction");
		AttributeName.add("NUniquePolyhedronShapes");
		AttributeName.add("MaxPackingEfficiency");
        AttributeName.add("mean_WCMagnitude_1stShell");
        AttributeName.add("mean_WCMagnitude_2ndShell");
        AttributeName.add("mean_WCMagnitude_3rdShell");
        AttributeName.add("dissimilarity_FCC");
        AttributeName.add("dissimilarity_BCC");
        AttributeName.add("dissimilarity_SC");
        for (String prop : ElementalProperties) {
            AttributeName.add("mean_NeighDiff_" + prop);
            AttributeName.add("var_NeighDiff_" + prop);
            AttributeName.add("min_NeighDiff_" + prop);
            AttributeName.add("max_NeighDiff_" + prop);
        }
        
        // Evaluate each entry
        attributesAdded = AttributeName.size() - attributesAdded;
        double[] newAttr = new double[attributesAdded];
        for (BaseEntry entry : Entries) {
            CrystalStructureEntry ptr = (CrystalStructureEntry) entry;
            
            // Call analysis
            VoronoiCellBasedAnalysis tool = new VoronoiCellBasedAnalysis(false);
            try {
                tool.analyzeStructre(ptr.getStructure());
            } catch (Exception e) {
                throw new Error(String.format("Voronoi error for %s (#%d): %s",
                        ptr.getName(), Entries.indexOf(entry), e.getMessage()));
            }
            
            
            // Get results
            newAttr[0] = tool.faceCountAverage();
            newAttr[1] = tool.faceCountVariance();
            newAttr[2] = tool.faceCountMinimum();
			newAttr[3] = tool.faceCountMaximum();
            newAttr[4] = tool.volumeFractionVariance();
            newAttr[5] = tool.volumeFractionMinimum();
            newAttr[6] = tool.volumeFractionMaximum();
			newAttr[7] = (double) tool.getUniquePolyhedronShapes().size();
			newAttr[8] = tool.maxPackingEfficiency();
            newAttr[9] = tool.warrenCowleyOrderingMagnituide(1);
            newAttr[10] = tool.warrenCowleyOrderingMagnituide(2);
            newAttr[11] = tool.warrenCowleyOrderingMagnituide(3);
            newAttr[12] = tool.meanFCCDissimilarity();
            newAttr[13] = tool.meanBCCDissimilarity();
            newAttr[14] = tool.meanSCDissimilarity();
            
            // Compute neighbor property difference
            int counter = 15;
            int[] elemIndex = new int[ptr.getStructure().nTypes()];
            for (int i=0; i<elemIndex.length; i++) {
                elemIndex[i] = ArrayUtils.indexOf(ElementNames, 
                        ptr.getStructure().getTypeName(i));
            }
            double[] propValues = new double[elemIndex.length];
            for (String prop : ElementalProperties) {
                // Get properties for elements in this structure
                double[] lookupTable;
                try {
                    lookupTable = getPropertyLookupTable(prop);
                } catch (Exception e) {
                    throw new Error(e);
                }
                for (int i=0; i<propValues.length; i++) {
                    propValues[i] = lookupTable[elemIndex[i]];
                }
                
                // Compute the neighbor differences for each atom
                double[] neighDiff;
                try {
                    neighDiff = tool.neighborPropertyDifferences(propValues);
                } catch (Exception e) {
                    throw new Error(e);
                }
                newAttr[counter++] = StatUtils.mean(neighDiff);
                double[] meanDeviation = neighDiff.clone();
                for (int i=0; i<meanDeviation.length; i++) {
                    meanDeviation[i] = Math.abs(meanDeviation[i] 
                            - newAttr[counter - 1]);
                }
                newAttr[counter++] = StatUtils.mean(meanDeviation);
                newAttr[counter++] = StatUtils.min(neighDiff);
                newAttr[counter++] = StatUtils.max(neighDiff);
            }
            entry.addAttributes(newAttr);
        }
    }
    
}
