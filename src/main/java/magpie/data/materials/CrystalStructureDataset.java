
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
 * Holds a entries that represent crystal structures.
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
     * <li>{@linkplain EffectiveCoordinationNumberAttributeGenerator}
     * <li>{@linkplain StructuralHeterogeneityAttributeGenerator}
     * <li>{@linkplain ChemicalOrderingAttributeGenerator}
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
            Generators.add(0, new ChemicalOrderingAttributeGenerator());
            Generators.add(0, new StructuralHeterogeneityAttributeGenerator());
            Generators.add(0, new EffectiveCoordinationNumberAttributeGenerator());
        }
    }
    

	/**
	 * Import all structure files in a directory.
	 * @param directory Directory containing VASP5-formatted files
	 * @param options No options
	 * @throws Exception If parsing fails
	 */
	@Override
	public void importText(String directory, Object[] options) throws Exception {
		// Get all files in that directory
        File dir = new File(directory);
        if (! dir.exists()) {
            throw new IllegalArgumentException("No such directory.");
        }
        if (! dir.isDirectory()) {
            throw new IllegalArgumentException("Expected an input directory, not file");
        }

        // Prepare list to store entries
        List<CrystalStructureEntry> toAdd = new ArrayList<>(dir.listFiles().length - 1);

        // Prepare file parser
        VASP5IO io = new VASP5IO();

        // Get radii of each element (used for some structure analyses
        double[] radii;
        try {
            radii = getPropertyLookupTable("CovalentRadius");
        } catch (Exception e) {
            radii = null;
        }

        // Read in entries from the 'properties.txt' file
        File propFile = new File(dir, "properties.txt");
        Set<String> filenames = new HashSet<>(); // List of files that were already read
        if (propFile.isFile()) {
            try (BufferedReader fp = new BufferedReader(new FileReader(propFile))) {
                String line = fp.readLine();
                importPropertyNames(line);
                while ((line = fp.readLine()) != null) {
                    // Split up the line
                    String[] words = line.split("\\s+");

                    // Try to read the structure file
                    String filename = words[0];
                    filenames.add(filename);
                    Cell strc;
                    try {
                        strc = io.parseFile(new File(dir, filename).getAbsolutePath());
                    } catch (Exception e) {
                        System.err.format("File failed to parse: %s\n", filename);
                        continue; // No need to read the properties
                    }

                    // Read in the properties
                    double[] props = importEntryProperties(words);

                    // Create the entry
                    CrystalStructureEntry entry = new CrystalStructureEntry(strc, filename, radii);
                    entry.setMeasuredProperties(props);
                    toAdd.add(entry);
                }
            }
        }


        // Read in the files that are not in "properties.txt"
        File[] files = dir.listFiles();
        for (File file : files) {
            // Check if file was already read
            if (filenames.contains(file.getName())) {
                continue;
            }

            // If not, read in the entry and add it to the dataset
            try {
                Cell strc = io.parseFile(file.getAbsolutePath());
                CrystalStructureEntry entry =
						new CrystalStructureEntry(strc, file.getName(), radii);
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
    public CrystalStructureEntry addEntry(String input) throws Exception {
        // Look up radii 
        double[] radii;
        try {
			radii = getPropertyLookupTable("CovalentRadius");
		} catch (Exception e) {
			radii = null;
		}
        
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
            name = new File(input).getName();
        }
        
        // Create the entry
        CrystalStructureEntry newEntry = new CrystalStructureEntry(strc, name, radii);
        
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
                    CrystalStructureEntry p = (CrystalStructureEntry) e;
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
     * @throws java.lang.Exception if writing fails
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
            CrystalStructureEntry entry = getEntry(e);
            String filename = String.format("%d-%s.vasp", e, getEntry(e).toString());
            
            // Get path to output file
            File file = new File(outputDir, filename);
            
            // Write out the POSCAR
            io.writeStructureToFile(entry.getStructure(), file.getPath());
            
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
            
            fp.println();
        }
        
        // Close up
        fp.close();
    }
}
