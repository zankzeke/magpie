
package magpie.data.materials;

import java.io.BufferedReader;
import java.util.*;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import magpie.attributes.generators.crystal.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.io.FileUtils;
import vassal.data.Cell;
import vassal.io.VASP5IO;

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
 * <p><b><u>Implemented Save Formats</u></b>
 * 
 * <save><p><b>poscar</b> - Save dataset as a directory full of POSCARs.
 * <br>Properties of each entry will be saved in a file in that 
 * directory named "properties.txt"</save>
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * @author Logan Ward
 */
public class CrystalStructureDataset extends CompositionDataset {

    /**
     * Create a instance with the default attribute generators
     * @see CrystalStructureDataset#CrystalStructureDataset(boolean) 
     */
    public CrystalStructureDataset() {
        this(true);
    }
    
    /**
     * Create a new instance.
     * 
     * <p>Default attribute generators are:
     * <ol>
     * <li>{@linkplain CoordinationNumberAttributeGenerator}
     * <li>{@linkplain StructuralHeterogeneityAttributeGenerator}
     * <li>{@linkplain ChemicalOrderingAttributeGenerator}
     * <li>{@linkplain LatticeSimilarityAttributeGenerator}
     * <li>{@linkplain PackingEfficiencyAttributeGenerator}
     * <li>{@linkplain LocalPropertyDifferenceAttributeGenerator}
     * <li>Default generators from {@linkplain CompositionDataset#CompositionDataset(boolean) }
     * </ol>
     * 
     * @param useDefaultGenerators Whether to use the default attribute generators
     */
    public CrystalStructureDataset(boolean useDefaultGenerators) {        
        super(useDefaultGenerators);
        
        // Add in the generators
        if (useDefaultGenerators) {
            Generators.add(0, new LocalPropertyDifferenceAttributeGenerator());
            Generators.add(0, new PackingEfficiencyAttributeGenerator());
            Generators.add(0, new LatticeSimilarityAttributeGenerator());
            Generators.add(0, new ChemicalOrderingAttributeGenerator());
            Generators.add(0, new StructuralHeterogeneityAttributeGenerator());
            Generators.add(0, new CoordinationNumberAttributeGenerator());
        }
    }
    

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
        VASP5IO io = new VASP5IO();
        for (File file : files) {
            try {
                Cell strc = io.parseFile(file.getAbsolutePath());
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
    public AtomicStructureEntry addEntry(String input) throws Exception {
        // Look up radii 
        double[] radii = getPropertyLookupTable("CovalentRadius");
        
        Cell strc;
        String name;
        if (input.contains("\n")) {
            // Assume input is a complete VASP file
            List<String> inputFile = Arrays.asList(input.split("\n"));
            strc = new VASP5IO().parseStructure(inputFile);
            name = inputFile.get(0);
        } else {
            // Assume it is a filename
            strc = new VASP5IO().parseFile(input);
            name = input;
        }
        
        // Create the entry
        AtomicStructureEntry newEntry = new AtomicStructureEntry(strc, name, radii);
        
        // Add and return it
        addEntry(newEntry);
        return newEntry;
    }

    @Override
    public void runAttributeGenerators() throws Exception {
        // Since the reporesentations used to generate attributes (e.g., Voronoi tessellations)
        //   take large amounts of memory, the idea is to split the dataset into 
        //   smaller chunks and generate attributes in batches.
        if (NEntries() > 1000) {
            // Split dataset into groups of less than 1000 entries
            Dataset[] splits = splitForThreading(NEntries() / 1000 + 1);

            // Run attribute generator on each split
            for (Dataset d : splits) {
                // Generate attributes
                d.runAttributeGenerators(); 
                
                // Delete representations
                for (BaseEntry e : d.getEntries()) {
                    AtomicStructureEntry p = (AtomicStructureEntry) e;
                    p.clearRepresentations();
                }
                System.gc();
            }
            
            // Transfer attribute names
            setAttributeNames(Arrays.asList(splits[0].getAttributeNames()));
        } else {
            super.runAttributeGenerators();
        }
    }

    @Override
    public String saveCommand(String Basename, String Format) throws Exception {
        if (Format.equalsIgnoreCase("poscar")) {
            writePOSCARs(Basename);
            return Basename;
        } else {
            return super.saveCommand(Basename, Format); 
        }
    }
    
    /**
     * Save dataset as a directory full of POSCARs. Will delete directory at the 
     * specified path if it exists. File names: [Entry #]-[Composition].vasp
     * 
     * Also writes out the measured and predicted class of these entries to a
     * file named "properties.txt" in this directory
     * 
     * @param directory Path to output directory
     * @throws java.lang.Exception
     */
    public void writePOSCARs(String directory) throws Exception {
        // Delete directory, if it exists
        File outputDir = new File(directory);
        if (outputDir.isDirectory()) {
            FileUtils.deleteDirectory(outputDir);
        }
        
        // Create the directory
        outputDir.mkdirs();
        
        // Create the properties output file
        PrintWriter fp = new PrintWriter(new File(outputDir, "properties.txt"));
        fp.print("filename");
        for (int p=0; p<NProperties(); p++) {
            String pName = getPropertyName(p);
            fp.format(" %s_measured %s_predicted", pName, pName);
        }
        if (getTargetPropertyIndex() == -1) {
            fp.println(" class_measured class_predicted");
        } else {
            fp.println();
        }
        
        // Write all the entries
        VASP5IO io = new VASP5IO();
        for (int e=0; e<NEntries(); e++) {
            // Get the filename
            AtomicStructureEntry entry = getEntry(e);
            String filename = String.format("%d-%s.vasp", e, getEntry(e).toString());
            
            // Get path to output file
            File file = new File(outputDir, filename);
            
            // Write out the POSCAR
            io.writeStructureToFile(entry.getStructure(), file.getCanonicalPath());
            
            // Write path to output file
            fp.print(filename);
            
            // Write out properties
            for (int p=0; p<NProperties(); p++) {
                if (entry.hasMeasuredProperty(p)) {
                    fp.format(" %.6e", entry.getMeasuredProperty(p));
                } else {
                    fp.print(" None");
                }
                if (entry.hasPredictedProperty(p)) {
                    fp.format(" %.6e", entry.getPredictedProperty(p));
                } else {
                    fp.print(" None");
                }
            }
            if (getTargetPropertyIndex() == -1) {
                if (entry.hasMeasurement()) {
                    fp.format(" %.6e", entry.getMeasuredClass());
                } else {
                    fp.print(" None");
                }
                if (entry.hasPrediction()) {
                    fp.format(" %.6e", entry.getPredictedClass());
                } else {
                    fp.print(" None");
                }
            }
        }
        
        // Close up
        fp.close();
    }
}
