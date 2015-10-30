
package magpie.data.materials.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Collections;
import magpie.data.materials.CompositionDataset;

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
     * Load in an elemental property lookup table
     * @param dataDir Directory containing lookup data
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
}
