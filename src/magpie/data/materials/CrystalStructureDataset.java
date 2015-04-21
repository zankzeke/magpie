
package magpie.data.materials;

import vassal.analysis.VoronoiCellBasedAnalysis;
import java.io.BufferedReader;
import java.util.*;
import java.io.File;
import java.io.FileReader;
import vassal.data.Cell;
import vassal.io.VASP5IO;
import magpie.data.BaseEntry;
import magpie.utility.MathUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Holds a entries that represent crystal structures. Here, the attributes are
 * designed to be insensitive to the length scale of the structure. 
 * As an example, this would be useful to predict the properties of a new material
 * in an already-known structure (e.g., the energy of a perovskite crystal
 * with a yet-unstudied composition). In order to model the effects of 
 * changing length scales, consider {@linkplain AtomicStructureDataset}.
 * 
 * <p><b>How to Import Files:</b>
 * 
 * <p>Rather than importing data from a single file with the 
 * {@linkplain #importText(java.lang.String, java.lang.Object[]) } operation, 
 * this class imports all the files in a given directory. These files must be
 * VASP 5 POSCAR files. Any files that are not in this format will be ignored.
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
 * <p>The crystal-structure-based attributes are design to be similar (if not
 * equal) for all crystals based on the same prototype (e.g. coordination number 
 * statistics). The intention for these attributes is that they can be used 
 * before determining the equilibrium lattice parameter or atom positions in a 
 * crystal using a tool like DFT. For example, this would be useful if you wanted 
 * to predict the formation energy of a hypothetical structure without have to first
 * find the equilibrium lattice parameter. The attributes are also design to be
 * insensitive to unit cell selection.
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
        if (! dir.exists()) {
            throw new Exception("No such directory.");
        }
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
        List<AtomicStructureEntry> toAdd = new LinkedList<>();
        for (File file : files) {
            try {
                Cell strc = new VASP5IO().parseFile(file.getAbsolutePath());
                AtomicStructureEntry entry = 
						new AtomicStructureEntry(strc, file.getName(), radii);
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
    public AtomicStructureEntry getEntry(int index) {
        return (AtomicStructureEntry) super.getEntry(index); 
    }

    @Override
    protected void calculateAttributes() {       
        // Generate Voronoi-based attributes
        generateVoronoiCellBasedAttributes();
        
        // Calculate composition-based attributes
        super.calculateAttributes(); 
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
     * <li>Bond length statistics (variation between and within cells)
     * <li>Variance in cell size as fraction of entire cell
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
        AttributeName.add("var_MeanBondLength");
        AttributeName.add("min_MeanBondLength");
        AttributeName.add("max_MeanBondLength");
        AttributeName.add("mean_BondLengthVariation");
        AttributeName.add("var_BondLengthVariation");
        AttributeName.add("min_BondLengthVariation");
        AttributeName.add("max_BondLengthVariation");
        AttributeName.add("var_CellVolume");
        AttributeName.add("UniquePolyhedronShapesPerAtom");
        AttributeName.add("MaxPackingEfficiency");
        AttributeName.add("dissimilarity_FCC");
        AttributeName.add("dissimilarity_BCC");
        AttributeName.add("dissimilarity_SC");
        AttributeName.add("mean_WCMagnitude_1stShell");
        AttributeName.add("mean_WCMagnitude_2ndShell");
        AttributeName.add("mean_WCMagnitude_3rdShell");
        for (String prop : ElementalProperties) {
            AttributeName.add("mean_NeighDiff_" + prop);
            AttributeName.add("var_NeighDiff_" + prop);
            AttributeName.add("min_NeighDiff_" + prop);
            AttributeName.add("max_NeighDiff_" + prop);
        }
        
        // Evaluate each entry
        attributesAdded = AttributeName.size() - attributesAdded;
        double[] newAttr = new double[attributesAdded];
//        int entryCount = 0;
        for (BaseEntry entry : Entries) {
            AtomicStructureEntry ptr = (AtomicStructureEntry) entry;
//            System.out.println("\tEvalauting entry #" + entryCount++ 
//                    + ": " + ptr.getName());
            
            // Call analysis
            VoronoiCellBasedAnalysis tool = new VoronoiCellBasedAnalysis(false);
            try {
                tool.analyzeStructre(ptr.getStructure());
            } catch (Exception e) {
                System.out.format("\tVoronoi error for %s (#%d). Setting NaN for all attributes.",
                        ptr.getName(), Entries.indexOf(entry), e.getMessage());
                Arrays.fill(newAttr, Double.NaN);
                entry.addAttributes(newAttr);
            }
            
            // Coordination number attributes
            int counter=0;
            newAttr[counter++] = tool.faceCountAverage();
            newAttr[counter++] = tool.faceCountVariance();
            newAttr[counter++] = tool.faceCountMinimum();
			newAttr[counter++] = tool.faceCountMaximum();
            
            // Bond length attributes
            //    Variation between cells
            double[] meanBondLengths = tool.meanBondLengths();
            double lengthScale = StatUtils.mean(meanBondLengths);
            for (int i=0; i<meanBondLengths.length; i++) {
                meanBondLengths[i] /= lengthScale; // Normalize bond lengths
            }
            newAttr[counter++] = MathUtils.meanAbsoluteDeviation(meanBondLengths);
            newAttr[counter++] = StatUtils.min(meanBondLengths);
            newAttr[counter++] = StatUtils.max(meanBondLengths);
            
            //     Variation within a single cell
            double[] bondLengthVariation = tool.bondLengthVariance(meanBondLengths);
            for (int i=0; i<bondLengthVariation.length; i++) {
                // Normalize bond length variation by mean bond length of each cell
                bondLengthVariation[i] /= meanBondLengths[i];
            }
            newAttr[counter++] = StatUtils.mean(bondLengthVariation);
            newAttr[counter++] = MathUtils.meanAbsoluteDeviation(bondLengthVariation);
            newAttr[counter++] = StatUtils.min(bondLengthVariation);
            newAttr[counter++] = StatUtils.max(bondLengthVariation);
            
            // Cell volume / shape attributes
            newAttr[counter++] = tool.volumeVariance() * 
                    ptr.getStructure().nAtoms() / ptr.getStructure().volume();
			newAttr[counter++] = (double) tool.getUniquePolyhedronShapes().size() 
                    / ptr.getStructure().nAtoms();
			newAttr[counter++] = tool.maxPackingEfficiency();
            newAttr[counter++] = tool.meanFCCDissimilarity();
            newAttr[counter++] = tool.meanBCCDissimilarity();
            newAttr[counter++] = tool.meanSCDissimilarity();
            
            // Ordering attributes
            newAttr[counter++] = tool.warrenCowleyOrderingMagnituide(1);
            newAttr[counter++] = tool.warrenCowleyOrderingMagnituide(2);
            newAttr[counter++] = tool.warrenCowleyOrderingMagnituide(3);
            
            // Neighbor property difference attributes
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
