package magpie.data.materials.util;

import magpie.data.materials.CompositionDataset;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Holds data that any {@linkplain CompositionDataset} needs to lookup.
 * 
 * @author Logan Ward
 */
abstract public class LookupData {
	/**
	 * Order in which to sort elements. Mainly important for printing. People are
	 *  accustomed to seeing elements ordered by electronegativity (NaCl, not ClNa).
	 */
	static public int[] SortingOrder = new int[]{91, 1, 26, 62, 85, 102, 109, 111, 112, 2, 24, 53, 64, 74, 90,
            104, 110, 3, 20, 27, 55, 60, 66, 68, 61, 72, 73, 78, 75, 67, 71, 83, 89, 103, 107, 108,
            21, 25, 36, 54, 63, 88, 76, 92, 97, 93, 79, 69, 70, 80, 86, 87, 106, 105, 19, 22, 28, 30,
            31, 32, 4, 33, 5, 34, 35, 37, 38, 39, 40, 6, 41, 43, 58, 100, 77, 94, 95, 98, 101, 81, 65,
            99, 84, 82, 96, 7, 18, 23, 29, 44, 59, 57, 56, 42, 45, 46, 47, 48, 49, 50, 51, 52, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 17};
	
	/**
	 * Name of elements, ordered by atomic number.
	 */
	static public String[] ElementNames = new String[]{"H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na",
            "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr",
            "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb",
            "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn",
            "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu",
            "Gd", "Tb", "Dy", "Ho", "Er", "Tm", "Yb", "Lu", "Hf", "Ta", "W", "Re", "Os",
            "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra",
            "Ac", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm",
            "Md", "No", "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds",
            "Rg", "Cn"};
	
	/**
	 * Holds elemental property data. 
	 */
	static public SortedMap<String,double[]> ElementalProperties = Collections.synchronizedSortedMap(new TreeMap<String,double[]>());
	
	/**
	 * Holds oxidation states of individual elements
	 */
	static public double[][] OxidationStates = null;

	/**
	 * Ionization energies of each element
	 */
	static public double[][] IonizationEnergies = null;
    
    /**
     * Properties of a pair of elements. Key is the name of the property,
     * value is a triangular matrix where [i][j] is the property of element
     * element Z=i+1 and Z=j+1. Ex: [1][2] is the property of He-Li. Unknown values
     * are stored as {@linkplain Double#NaN}
     */
    static public SortedMap<String,double[][]> ElementPairProperties = 
            Collections.synchronizedSortedMap(new TreeMap<String,double[][]>());

    /**
     * Load in an elemental property lookup table
     * @param dataDir Directory containing lookup-data
     * @param property Property to be loaded
     * @return List of elemental properties, ordered by atomic number
     * @throws Exception
     */
    public static double[] loadPropertyLookupTable(String dataDir, String property) throws Exception {
        Path datafile = Paths.get(dataDir);
        BufferedReader is;
        try {
            is = Files.newBufferedReader(datafile.resolve(property + ".table"), Charset.forName("US-ASCII"));
            double[] output = new double[LookupData.ElementNames.length];
            for (int i = 0; i < output.length; i++) {
                try {
                    output[i] = Double.parseDouble(is.readLine());
                } catch (IOException | NumberFormatException e) {
                    output[i] = Double.NaN;
                }
            }
            is.close();
            return output;
        } catch (IOException e) {
            throw new Exception("Property " + property + " failed to read due to " + e);
        }
    }
    
    /**
     * Load in a binary property table
     * @param dataDir Directory containing property lookup data
     * @param property Name of property
     * @return 2D triangular array containing property data
     * @throws Exception 
     */
    public static double[][] loadPairPropertyTable(String dataDir, 
            String property) throws Exception {
        // Open up the file
        File file = new File(new File(dataDir, "pair"), property + ".table");
        if (! file.isFile()) {
            throw new FileNotFoundException("No lookup file not found for " + property);
        }
        
        // Open the file
        BufferedReader fp = new BufferedReader(new FileReader(file));
        
        // Initialize output
        double[][] output = new double[ElementNames.length][];
        for (int row=0; row<ElementNames.length; row++) {
            output[row] = new double[row];
        }
        
        // Read it in
        while (true) {
            String line = fp.readLine();
            
            // If null, done reading from this file
            if (line == null) {
                break;
            }
            
            // Check that this line has the required input size
            String[] words = line.split("\\s+");
            if (words.length < 3) {
                continue;
            }
            
            // Read in the line
            int elemA = ArrayUtils.indexOf(ElementNames, words[0]);
            int elemB = ArrayUtils.indexOf(ElementNames, words[1]);
            if (elemA == ArrayUtils.INDEX_NOT_FOUND || 
                    elemB == ArrayUtils.INDEX_NOT_FOUND) {
                continue;
            }
            try {
                double value = Double.parseDouble(words[2]);
                output[Math.max(elemA, elemB)][Math.min(elemA, elemB)] = value;
            } catch (NumberFormatException e) {
            }
        }
        return output;
    }
    
    /**
     * Helper function for reading from a binary property lookup table
     * @param table Table to be read
     * @param elemA Symbol of one element
     * @param elemB Symbol of a second element
     * @return Property, {@linkplain Double#NaN} if not found
     */
    static public double readPairTable(double[][] table, String elemA, String elemB) {
        // Parse element names
        int elemAVal = ArrayUtils.indexOf(ElementNames, elemA);
        if (elemAVal == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalArgumentException("No such element: " + elemA);
        }
        int elemBVal = ArrayUtils.indexOf(ElementNames, elemB);
        if (elemBVal == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalArgumentException("No such element: " + elemB);
        }
        
        // Lookup table
        return LookupData.readPairTable(table, elemAVal, elemBVal);
    }
    
    /**
     * Helper function for reading from a binary property lookup table
     * @param table Table to be read
     * @param elemA Index of one element
     * @param elemB Index of a second element
     * @return Property, {@linkplain Double#NaN} if not found
     */
    static public double readPairTable(double[][] table, int elemA, int elemB) {
        return table[Math.max(elemA, elemB)][Math.min(elemA, elemB)];
    }
    
    /**
     * Read in ionization energies
     * @param path Path to lookup table
     * @throws java.io.IOException
     */
    static public void readIonizationEnergies(String path) throws IOException {
        // Open file
        BufferedReader fp = new BufferedReader(new FileReader(path));
        
        // Read file
        List<double[]> temp = new LinkedList<>();
        while (true) {
            String line = fp.readLine();
            if (line == null) {
                break;
            }
            
            String[] words = line.split("[ \t]");
            double[] energies = new double[words.length];
            for (int w=0; w<words.length; w++) {
                try {
                    energies[w] = Double.parseDouble(words[w]);
                } catch (NumberFormatException e) {
                    energies = new double[0];
                    break;
                }
            }
            
            // Store result
            temp.add(energies);
        }
        
        // Transfer result to array
        IonizationEnergies = new double[temp.size()][];
        for (int i=0; i<temp.size(); i++) {
            IonizationEnergies[i] = temp.get(i);
        }
    }

    /**
     * Reads in a data file that contains known oxidation states for each
     * element. List should be contained in a file named OxidationStates.table
     * 
     * @param dataDir Path to directory holding the lookup data
     * 
     * @return Oxidation states for each element
     */
    static synchronized public double[][] readOxidationStates(String dataDir) {
        Path datafile = Paths.get(dataDir);
        BufferedReader is;
        try {
            is = Files.newBufferedReader(
                    datafile.resolve("OxidationStates.table"), Charset.forName("US-ASCII"));
            // Read the file
            int i;
            int j; // Counters
            OxidationStates = new double[ElementNames.length][];
            for (i = 0; i < ElementNames.length; i++) {
                String[] states = is.readLine().split(" ");
                if (states[0].isEmpty()) {
                    OxidationStates[i] = new double[0];
                } else {
                    OxidationStates[i] = new double[states.length];
                    for (j = 0; j < OxidationStates[i].length; j++) {
                        OxidationStates[i][j] = Double.parseDouble(states[j]);
                    }
                }
            }
            is.close();
        } catch (IOException | NumberFormatException e) {
            throw new RuntimeException("Oxidation states failed to read due to " + e);
        }
        return OxidationStates;
    }
}
