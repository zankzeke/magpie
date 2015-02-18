package magpie.data.materials;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import magpie.data.BaseEntry;
import magpie.data.materials.util.CompositionDatasetOutput;
import magpie.data.materials.util.LookupData;
import magpie.data.materials.util.PropertyLists;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.*;

/**
 * This class stores entries that describe a material based solely on its
 * composition.<p>
 *
 * Some of the available features include:
 * <ul>
 * <li>Generate attributes based on composition only</li>
 * <li>Read in data from specially-formatted input file</li>
 * <li>Store multiple properties for a each entry</li>
 * </ul>
 *
 * When reading data in from text using the <code>importText</code> function,
 * the data must be whitespace-delimited with a header row giving property names
 * and the composition of the material as the first row:
 *
 * <p>
 * Composition property_1{class1,class2} property_2 ... property_N<br>
 * As,0.2,B,0.8, class1 0.4 0.8 ...<br>
 * AsB4, class2 0.9 none ...<br>
 * Cu2Zr,4, class1 0.1 10.8 ...<br>
 * &lt;as many entries as you desire&gt;<br>
 *
 * <p>
 * A few things to know about this format:
 * 
 * <ol>
 * <li>Format of composition doesn't matter too much. Only real requirement is that
 * element names are capitalized.
 * <li>If a property has discrete classes, you can specify what the allowed values
 * are in the header by adding them to the property name surrounded by "{}"s and separated
 * by commas.
 * <li>If the measured value of a property is not known, put down "None"
 * <li>For entries with duplicate compositions: Either select the values from 
 * lowest-energy duplicate entry or, if energy is not available, the average 
 * value of continuous properties or the lowest-index class value (i.e. which
 * ever one is listed first in the header of the input file)
 * </ol>
 *
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>attributes composition &lt;true|false&gt;</b> - 
 *  Set whether to use composition as attributes
 * <br><pr><i>true|false</i> - Whether to use composition as attributes
 * <br>By default, this class does not use composition (by itself) as attributes</command>
 *
 * <command><p><b>attributes properties</b> - List which elemental properties are used to generate attributes</command>
 *
 * <command><p><b>attributes properties add &lt;names...></b> - Add elemental properties to use when generating attributes</command>
 * <br><pr><i>name...</i>: Name of a elemental properties</command>
 *
 * <command><p><b>attributes properties add set &lt;name&gt;</b> - Add in all elemental properties from a pre-defined set
 * <br><pr><i>name</i>: Name of the pre-defined set</command>
 *
 * <command><p><b>attributes properties remove &lt;names...></b> - Remove properties from list of those used when generating attributes
 * <br><pr><i>names...</i>: Name of properties to remove</command>
 *
 * <command><p><b>attributes properties &lt;directory></b> - Specify directory that contains the elemental property lookup files
 * <br><pr><i>directory</i>: Desired directory</command>
 * 
 * <p><b><u>Implemented Save Formats</u></b>
 * 
 * <save><p><b>comp</b> - All properties with composition written by element fraction
 * <br>Very similar to the "prop" format"</save>
 *
 * @author Logan Ward
 * @version 0.2
 */
public class CompositionDataset extends magpie.data.MultiPropertyDataset {

    /**
     * List of element names
     */
    public String[] ElementNames = LookupData.ElementNames;
    /**
     * Order in which elements are sorted (used when printing)
     */
    protected int[] SortingOrder = LookupData.SortingOrder;
    /**
     * Location of lookup date files
     */
    public String DataDirectory = "./Lookup Data";
    /**
     * List of properties used when generating attributes
     */
    public List<String> ElementalProperties = new LinkedList<>();
    /**
     * Map of elemental property names to values
     */
    public Map<String, double[]> PropertyData = LookupData.ElementalProperties;
	/** Oxidation states of every element */
	public double[][] OxidationStates = LookupData.OxidationStates;
	
    /** Whether to use composition as attributes */
    protected boolean UseComposition = false;

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public CompositionDataset clone() {
        CompositionDataset x = (CompositionDataset) super.clone();
        x.ElementNames = ElementNames.clone();
        return x;
    }

    @Override
    public CompositionDataset emptyClone() {
        CompositionDataset x = (CompositionDataset) super.emptyClone();
        x.ElementNames = ElementNames.clone();
        return x;
    }

    @Override
    public CompositionEntry getEntry(int index) {
        return (CompositionEntry) super.getEntry(index);
    }

	@Override
	public CompositionEntry addEntry(String input) throws Exception {
		CompositionEntry toAdd = new CompositionEntry(input);
		addEntry(toAdd);
		return toAdd;
	}

    /**
     * Get the order in which elements are sorted when storing a composition. Sorting
     * makes it faster to detect whether compounds are equal
     * @return Sorting order
     */
    public int[] getSortingOrder() {
        return SortingOrder.clone();
    }
    
    /**
     * Read in an dataset from file. See documentation for format information.
     *
     * @param filename Path of file to be imported
     * @throws java.lang.Exception
     */
    @Override
    @SuppressWarnings("empty-statement")
    public void importText(String filename, Object[] options) throws Exception {
        // Count the number of lines (1 per entry + 1 header)
        // Thanks to: http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
        LineNumberReader lr = new LineNumberReader(new FileReader(filename));
        while (lr.skip(Long.MAX_VALUE) > 0) {
        };
        int Entry_Count = lr.getLineNumber() + 1;
        lr.close();

        // Define the reader
        BufferedReader is = Files.newBufferedReader(Paths.get(filename), Charset.forName("US-ASCII"));
        String line;
        String[] words;

        // Read in properties from header
        line = is.readLine();
		importPropertyNames(line);

        // Determine which property is the energy ("energy_pa")
        int energy_id = getPropertyIndex("energy_pa");

        // Read in each entry
        TreeMap<BaseEntry, CompositionEntry> acceptedEntries = new TreeMap<>();
        TreeMap<BaseEntry, List<double[]>> duplicateProperties = new TreeMap<>();
        CompositionEntry Entry;
        for (int e = 0; e < Entry_Count; e++) {
            double[] properties;
            // Read a line and tokenize it
            line = is.readLine();
            if (line == null) {
                break;
            }
            words = line.split("\\s+");
            if (words.length < 2) {
                continue;
            }

            // Read properties
            properties = importEntryProperties(words);

            // Make an entry
            Entry = new CompositionEntry(words[0]);
            Entry.setMeasuredProperties(properties);

            // Add if the set does not already have it
            if (!acceptedEntries.containsKey(Entry)) {
                acceptedEntries.put(Entry, Entry);
            } else {
                // If the entries have a "energy_pa" as a property, supplant the existing
                //   entry if it is higher in energy
                if (energy_id != -1) {
                    CompositionEntry oldBest = (CompositionEntry) acceptedEntries.get(Entry);
                    if (oldBest.getMeasuredProperty(energy_id)
                            > Entry.getMeasuredProperty(energy_id)) {
                        acceptedEntries.remove(oldBest);
                        acceptedEntries.put(Entry, Entry);
                    }
                } else {
                    // Add to list of entries to duplicate properties if there is no "energy" property
                    if (!duplicateProperties.containsKey(Entry)) {
                        // Add in the property data of the accepted entry
                        double[] props = acceptedEntries.get(Entry).getMeasuredProperties();
                        List<double[]> newList = new LinkedList<>();
                        newList.add(props);
                        duplicateProperties.put(Entry, newList);
                    }
                    duplicateProperties.get(Entry).add(Entry.getMeasuredProperties());
                }
            }
        }

        // If we have any duplicate properties, average them
        Iterator<BaseEntry> Eiter = duplicateProperties.keySet().iterator();
        while (Eiter.hasNext()) {
            CompositionEntry E = (CompositionEntry) Eiter.next();
            CompositionEntry accepted = acceptedEntries.get(E);
            List<double[]> dupProps = duplicateProperties.get(E);
            for (int p = 0; p < NProperties(); p++) {
                if (getPropertyClassCount(p) == 1) {
                    double sum = 0, count = 0;
                    for (double[] props : dupProps) {
                        double toAdd = props[p];
                        if (! Double.isNaN(toAdd)) {
                            sum += toAdd; count++;
                        }
                    }
                    if (count > 0) {
                        accepted.setMeasuredProperty(p, sum / count);
                    }
                } else {
                    double value = Double.MAX_VALUE;
                    boolean wasFound = false;
                    for (double[] props : dupProps) {
                        if (! Double.isNaN(props[p])) {
                            wasFound = true;
                            if (props[p] < value) {
                               value = props[p];
                            }
                        }                
                    }
                    if (wasFound) {
                        accepted.setMeasuredProperty(p, value);
                    }
                }
            }
        }

        // Close the file
        is.close();

        // Copy the entries
        this.Entries = new ArrayList<>(acceptedEntries.keySet());
    }

    /**
     * Used by {@linkplain #importText(java.lang.String, java.lang.Object[]) } 
     * to import property measurments for each entry.
     * 
     * @param words Line describing entry, split into words
     * @return Property measurements for this entry
     */
    protected double[] importEntryProperties(String[] words) {
        double[] properties;
        // Get the properties
        properties = new double[NProperties()];
        for (int p = 0; p < NProperties(); p++) {
            try {
                if (getPropertyClassCount(p) == 1) {
                    properties[p] = Double.parseDouble(words[p + 1]);
                } else {
                    int index = ArrayUtils.indexOf(getPropertyClasses(p), words[p+1]);
                    if (index == -1) {
                        index = Integer.parseInt(words[p+1]);
                    }
                    properties[p] = index;
                }
            } catch (NumberFormatException exc) {
                // System.err.println("Warning: Entry #"+i+" has an invalid property.");
                properties[p] = Double.NaN;
            }
        }
        return properties;
    }

	
	/**
	 * Given the line describing property names in the input file, read in property
	 * names and possible classes.
	 * @param line Line describing property names
	 * @see CompositionDataset
	 */
	protected void importPropertyNames(String line) {
		Pattern totalPattern = Pattern.compile("[\\d\\w]+(\\{.*\\})?"), // Captures entire name/classes 
				namePattern = Pattern.compile("^[\\d\\w]+"), // Given name/classes, get name
				classPattern = Pattern.compile("\\{.*\\}"); // Get the possible classes
		Matcher totalMatcher = totalPattern.matcher(line);
		totalMatcher.find(); // First match is composition
		while (totalMatcher.find()) {
			String total = totalMatcher.group();
			Matcher tempMatcher = namePattern.matcher(total); tempMatcher.find();
			String name = tempMatcher.group();
			if (! total.contains("{")) {
				addProperty(name);
			} else {
				tempMatcher = classPattern.matcher(total); tempMatcher.find();
				String classList = tempMatcher.group();
				// Trim off the "{,}"
				classList = classList.substring(1);
				classList = classList.substring(0, classList.length()-1);
				// Get the class names
				String[] classes = classList.split(",");
				for (int i=0; i<classes.length; i++) {
					classes[i] = classes[i].trim();
				}
				addProperty(name, classes);
			}
		}
	}
    
    /**
     * Set whether to use composition (i.e. fraction of each element present) as attributes.
     * 
     * <p>By default: This class does not use fractions as attributes.
     * @param decision Whether to use it or not
     */
    public void useCompositionAsAttributes(boolean decision) {
        this.UseComposition = decision;
    }

    /**
     * Define whether to look for elemental property lookup tables.
     *
     * <p>
     * Elemental property tables should have the properties for each element on
     * a separate line, sorted by Atomic Number. If the property is not known,
     * write "None". You will be alerted if that property was needed generating
     * attributes.
     *
     * @param DataDirectory PAth to the elemental property lookup directory
     */
    public void setDataDirectory(String DataDirectory) {
        this.DataDirectory = DataDirectory;
    }

    /**
     * Define a new elemental property to use when generating attributes.
     *
     * @param Name Name of property
     */
    public void addElementalProperty(String Name) {
        if (! ElementalProperties.contains(Name)) {
            ElementalProperties.add(Name);
        }
    }

    /**
     * Remove an elemental property from the list used when generating
     * attributes
     *
     * @param Name Name of property
     * @return Whether the property was found and removed
     */
    public boolean removeElementalProperty(String Name) {
        if (ElementalProperties.contains(Name)) {
            ElementalProperties.remove(Name);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Calculate attributes for each entry. Currently, this set is equipped to
     * calculate four kinds of properties:
     *
     * <ol>
     * <li><b>Fraction Only:</b> Only depends on the fraction of elements
     * present, and not what they are.
     * <li><b>Elemental Properties:</b> Various statistics based on the supplied
     * properties of each present element
     * <li><b>Band structure:</b> Fraction of electrons expected to be in each
     * orbital (s/p/d/f)
     * <li><b>Other:</b> Bond ionicity, sum of oxidation states, etc
     * </ol>
     * 
     * <p>Depending on value of {@linkplain #UseComposition}, may also use fractions
     * of each element as attributes.
     */
    @Override
    protected void calculateAttributes() {
        // --> Create attributes based on elemental fractions, if desired
        if (UseComposition) {
            generateElementFractionAttributes();
        }

        // --> Add attributes that are dependant on fraction only (not element type)
        generateCompositionAttributes();

        // --> Add attributes related to properties
        generatePropertyBasedAttributes();

        // --> Add attributes related to valance shell occupation
        generateValenceShellAttributes();

        // --> Calculate the percent ionic character for each entry
        generateIonicCharacter();

        // --> Add attribute based on whether it can form an ionic compound
        generateCanFormIonic();
    }

    /**
     * Reads in a data file that contains properties for each element.
     * MeasuredProperty list should be contained in a file named
     * PropertyName.table
     *
     * @param PropertyName MeasuredProperty of interest
     * @return That property for each element
     */
    public double[] getPropertyLookupTable(String PropertyName) throws Exception {
        // Check if it has been loaded in yet
        if (PropertyData.containsKey(PropertyName)) {
            return PropertyData.get(PropertyName);
        }

        // If not, load it in
        // Open the file for reading
        Path datafile = Paths.get(DataDirectory);
        BufferedReader is;
        try {
            is = Files.newBufferedReader(
                    datafile.resolve(PropertyName + ".table"), Charset.forName("US-ASCII"));

            // Read the file
            double[] output = new double[ElementNames.length];
            for (int i = 0; i < ElementNames.length; i++) {
                try {
                    output[i] = Double.parseDouble(is.readLine());
                } catch (IOException | NumberFormatException e) {
                    output[i] = Double.NaN;
                }
            }
            is.close();

            /// Return the data table, and save a copy
            PropertyData.put(PropertyName, output);
            return output;
        } catch (IOException e) {
            throw new Exception("Property " + PropertyName + " failed to read due to " + e);
        }
    }

    /**
     * Reads in a data file that contains known oxidation states for each
     * element. List should be contained in a file named OxidationStates.table
     *
     * @param DataDirectory Location of data files
     * @return List of possible oxidation states for each element
     */
    protected double[][] getOxidationStates(String DataDirectory) {
        // Open the file for reading
        Path datafile = Paths.get(DataDirectory);
        BufferedReader is;
        try {
            is = Files.newBufferedReader(
                    datafile.resolve("OxidationStates.table"), Charset.forName("US-ASCII"));

            // Read the file
            int i, j; // Counters
            double[][] output = new double[ElementNames.length][];
            for (i = 0; i < ElementNames.length; i++) {
                String[] States = is.readLine().split(" ");
                if (States[0].isEmpty()) {
                    output[i] = null;
                } else {
                    output[i] = new double[States.length];
                    for (j = 0; j < output[i].length; j++) {
                        output[i][j] = Double.parseDouble(States[j]);
                    }
                }
            }
            is.close();
            return output;
        } catch (IOException | NumberFormatException e) {
            throw new Error("Oxidation states failed to read due to " + e);
        }
    }

    /**
     * Generate attributes that only deal with composition (ignores element
     * types)
     */
    protected void generateCompositionAttributes() {
        double x;

        // --> Generate the variables only dependant on composition
        // Number of components
        AttributeName.add("NComp");
        for (int i = 0; i < NEntries(); i++) {
            getEntry(i).addAttribute((double) getEntry(i).getElements().length);
        }
        // L2 Norm of composition
        AttributeName.add("Comp_L2Norm");
        for (int i = 0; i < NEntries(); i++) {
            x = StatUtils.sumSq(getEntry(i).getFractions());
            getEntry(i).addAttribute(Math.sqrt(x));
        }

        // Some other norms  
        double[] norms = new double[]{3, 5, 7, 10};
        for (int i = 0; i < norms.length; i++) {
            AttributeName.add("Comp_L" + ((int) norms[i]) + "Norm");
        }

        for (int j = 0; j < NEntries(); j++) {
            double[] fractions = getEntry(j).getFractions();
            double[] newAttributes = new double[norms.length];
            for (int n = 0; n < norms.length; n++) {
                newAttributes[n] = 0.0;
                for (int k = 0; k < fractions.length; k++) {
                    newAttributes[n] += Math.pow(fractions[k], norms[n]);
                }
                newAttributes[n] = Math.pow(newAttributes[n], 1.0 / norms[n]);
            }
            getEntry(j).addAttributes(newAttributes);
        }
    }

    /**
     * Generate attributes that are simply the fraction of each element present
     */
    protected void generateElementFractionAttributes() {
        for (String ElementName : ElementNames) {
            AttributeName.add("X_" + ElementName);
        }

        // Determine the composition for each element
        double[] composition = new double[ElementNames.length], fractions;
        int[] elements;
        for (int i = 0; i < NEntries(); i++) {
            Arrays.fill(composition, 0);
            elements = getEntry(i).getElements();
            fractions = getEntry(i).getFractions();
            for (int j = 0; j < elements.length; j++) {
                composition[elements[j]] = fractions[j];
            }
            // Copy it into the feature array
            getEntry(i).addAttributes(composition);
        }
    }

    /**
     * Generate attributes that are based on elemental properties
     */
    protected void generatePropertyBasedAttributes() {
        // Create list of entries with missing elemental properties and properties
        //   with key missing values
        // Dev Note: I am not sure what to do with these entries, options: 
        //   1) Remove them from the dataset, which is a viable option
        //   2) Remove offending properties, which means different datasets will have different attributes
        // For now, I am passing the problem onto the user by issuing a warning
        Set<String> MissingData = new TreeSet<>();

        // Add in property names
        for (String prop : ElementalProperties) {
            AttributeName.add("mean_" + prop);
            AttributeName.add("maxdiff_" + prop);
            AttributeName.add("dev_" + prop);
            AttributeName.add("max_" + prop);
            AttributeName.add("min_" + prop);
            AttributeName.add("most_" + prop);
        }

        // Generate attributes for each entry
        double[] toAdd = new double[ElementalProperties.size() * 6];
        for (int e = 0; e < NEntries(); e++) {
            CompositionEntry entry = getEntry(e);
            int count = 0;

            // Generate data for each property
            for (String prop : ElementalProperties) {
                // Get the lookup table for this property
				double[] lookup;
				try {
					lookup = getPropertyLookupTable(prop);
				} catch (Exception ex) {
					throw new Error("Failed to retrieve property: " + prop);
				}

                // Check if any required lookup data is missing;
                for (int i = 0; i < entry.getElements().length; i++) {
                    if (lookup[entry.Element[i]] == Double.NaN) {
                        MissingData.add(ElementNames[entry.Element[i]] + ":" + prop);
                    }
                }

                // Calculate the mean
                double mean = entry.getMean(lookup);
                toAdd[count++] = mean;

                // Calculate the maximum diff
                toAdd[count++] = entry.getMaxDifference(lookup);
                // Calculate the mean deviation
                toAdd[count++] = entry.getAverageDeviation(lookup, mean);
                toAdd[count++] = entry.getMaximum(lookup);
                toAdd[count++] = entry.getMinimum(lookup);
                toAdd[count++] = entry.getMost(lookup);
            }

            // Add attributes to entry
            entry.addAttributes(toAdd);
        }

        // Print out warning of which properties have missing (see def of 
        //    OffendingProperties)
        if (MissingData.size() > 0) {
            System.err.println("WARNING: There are " + MissingData.size()
                    + " missing elmental properties:");
            int i = 0;
            Iterator<String> iter = MissingData.iterator();
            while (iter.hasNext()) {
                System.err.format("%32s", iter.next());
                if (i % 2 == 1) {
                    System.err.println();
                }
                i++;
            }
            if (i % 2 == 1) {
                System.err.println();
            }
            System.err.println();
            System.err.flush();
        }
    }

    /**
     * Generate attributes related to valence shell occupation
     */
    protected void generateValenceShellAttributes() {
        // Load in the number of electrons in each shell
        Character[] shell = new Character[]{'s', 'p', 'd', 'f'};
        double[][] n_valance = new double[4][];
		try {
			for (int i = 0; i < 4; i++) {
				n_valance[i] = getPropertyLookupTable("N" + shell[i] + "Valence");
			}
		} catch (Exception ex) {
			throw new Error("Failed to import number of valence electrons");
		}

        // Determine the fraction of electrons in each valence cell
        for (int i = 0; i < 4; i++) {
            AttributeName.add("frac_" + shell[i] + "Valence");
        }
        for (int i = 0; i < NEntries(); i++) {
            double[] total_e = new double[4];
            double sum_e = 0.0;
            // First, get the average number of electrons in each shell
            for (int j = 0; j < 4; j++) {
                total_e[j] = getEntry(i).getMean(n_valance[j]);
                sum_e += total_e[j];
            }

            // Convert to fractions
            for (int j = 0; j < 4; j++) {
                total_e[j] /= sum_e;
            }

            // Add to entry
            getEntry(i).addAttributes(total_e);
        }
    }

    /**
     * Generate the percent ionic character for each entry
     */
    protected void generateIonicCharacter() {
        double x;
        // --> Calculate the maximum and mean %Ionic character
		double[] en;
		try {
			en = getPropertyLookupTable("Electronegativity");
		} catch (Exception e) {
			throw new Error("Failed to import electronegativity");
		}
        AttributeName.add("MaxIonicChar");
        AttributeName.add("MeanIonicChar");
        for (int i = 0; i < NEntries(); i++) {
            getEntry(i).addAttribute(1
                    - Math.exp(-0.25 * Math.pow(getEntry(i).getMaxDifference(en), 2.0)));
            int[] elem = getEntry(i).getElements();
            double[] frac = getEntry(i).getFractions();
            x = 0.0;
            for (int j = 0; j < elem.length; j++) {
                for (int k = 0; k < elem.length; k++) {
                    x += frac[j] * frac[k] * (1 - Math.exp(-0.25
                            * Math.pow(en[elem[j]] - en[elem[k]], 2.0)));
                }
            }
            getEntry(i).addAttribute(x);
        }
    }

    /**
     * Determine whether each entry can form an ionic compound. Assumes that
     * each element can only take on a single oxidation state
     */
    protected void generateCanFormIonic() {
        double x, y;
        AttributeName.add("CanFormIonic");

		if (OxidationStates == null) {
			OxidationStates = getOxidationStates(DataDirectory);
			LookupData.OxidationStates = OxidationStates;
		}
        for (int i = 0; i < NEntries(); i++) {
            int[] elem = getEntry(i).getElements();
            double[] frac = getEntry(i).getFractions();
            x = 0; // Initially assume that it cannot form

            // If any of the compounds are noble gasses, it cannot form an ionic compound
            for (int j = 0; j < elem.length; j++) {
                if (OxidationStates[elem[j]] == null) {
                    x = -1;
                    break;
                }
            }
            if (x == -1) {
                x = 0;
                getEntry(i).addAttribute(x);
                continue;
            }

            // Loop through each possible combination
            int[] guess = new int[elem.length]; // Initialize a guess
            boolean was_incremented = true;
            while (was_incremented) {
                // Calculate the charge
                y = 0; // Start the charge out at zero
                for (int j = 0; j < elem.length; j++) {
                    y += frac[j] * OxidationStates[elem[j]][guess[j]];
                }

                // If the charge is equal to zero, we have found a valid compound
                if (y == 0) {
                    x = 1;
                    break;
                }

                // If not, increment the compound
                was_incremented = false;
                for (int j = 0; j < guess.length; j++) {
                    guess[j]++;
                    if (guess[j] == OxidationStates[elem[j]].length) {
                        guess[j] = 0;
                    } else {
                        was_incremented = true;
                        break;
                    }
                }
            }
            getEntry(i).addAttribute(x);
        }
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
		if (Command.isEmpty()) return super.runCommand(Command);
        String Action = Command.get(0).toString();
        switch (Action.toLowerCase()) {
            default:
                return super.runCommand(Command); 
        }
        
    }

    @Override
    protected Object runAttributeCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            return super.runAttributeCommand(Command);
        }
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "properties": case "prop": {
                return runPropertyCommand(Command.subList(1, Command.size()));
            }
            case "composition": {
                boolean useComp;
                try {
                    switch (Command.get(1).toString().toLowerCase()) {
                        case "true": useComp = true; break;
                        case "false": useComp = false; break;
                        default: throw new Exception();
                    }
                } catch (Exception e) {
                    throw new Exception("Usage: <dataset> attributes composition <true|false>");
                }
                useCompositionAsAttributes(useComp);
                if (useComp) {
                    System.out.println("\tSet dataset to use composition as attributes");
                } else {
                    System.out.println("\tSet dataset to not use composition as attributes");
                }
                return null;
            }
            default:
                return super.runAttributeCommand(Command);
        }
    }

    /**
     * Run commands that control which elemental properties are used when
     * generating attributes
     *
     * @param Command Command to act on
     * @return Any output from operation (null if no output)
     * @throws Exception
     */
    protected Object runPropertyCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            Iterator<String> iter = ElementalProperties.iterator();
            int count = 0;
            while (iter.hasNext()) {
                count++;
                System.out.print("\t" + iter.next());
                if (count % 5 == 0) {
                    System.out.println();
                }
            }
            if (count % 5 != 0) {
                System.out.println();
            }
            return null;
        }
        String Action = Command.get(0).toString();
        switch (Action.toLowerCase()) {
            case "add": {
                // Usage: add <name> or add set <name>
                if (Command.size() < 2) {
                    throw new Exception("Usage: \"<dataset> attributes properties add set <set name>\""
                            + " or properties add <property names...>");
                }
                // Add in new properties
                int originalSize = ElementalProperties.size();
                if (Command.get(1).toString().equalsIgnoreCase("set")) {
                    // Add properties from a known set
                    String[] Properties = PropertyLists.getPropertySet(Command.get(2).toString());
                    for (String p : Properties) {
                        addElementalProperty(p);
                    }
                } else {
                    // Add in properties one by one
                    for (int i = 1; i < Command.size(); i++) {
                        addElementalProperty(Command.get(i).toString());
                    }
                }
                System.out.println("\tAdded " + (ElementalProperties.size() - originalSize) 
                    + " new properties.");
            }
            break;
            case "remove": {
                // Usage: remove <names...>
                if (Command.size() < 2) {
                    throw new Exception("Usage: <dataset> attributes properties remove <property names...>");
                }
                // Remove those property from the set
                String output = "\tRemoved properties:";
                int nRemoved = 0;
                for (int i = 1; i < Command.size(); i++) {
                    boolean wasRemoved = removeElementalProperty(Command.get(i).toString());
                    if (wasRemoved) {
                        output += " " + Command.get(i).toString();
                        nRemoved++;
                    }
                }
                if (nRemoved > 0) {
                    System.out.println(output);
                } else {
                    System.out.println("\tWARNING: No attributes were removed.");
                }
                break;
            }
            case "directory": {
                // Define the lookup directory
                if (Command.size() < 2) {
                    throw new Exception("Usage: <dataset> attributes properties directory <directory name>");
                }
                DataDirectory = Command.get(1).toString();
                for (int i = 2; i < Command.size(); i++) {
                    DataDirectory += " " + Command.get(i).toString();
                }
            }
            break;
            default:
                throw new Exception("ERROR: Property command not recognized: " + Action);
        }
        return null;
    }

    @Override
    public String saveCommand(String Basename, String Format) throws Exception {
        String filename;
        switch (Format.toLowerCase()) {
            case "comp":
                filename = Basename + ".csv";
                CompositionDatasetOutput.saveCompositionProperties(this,
                        filename);
                return filename;
            default:
                return super.saveCommand(Basename, Format); 
        }
    }

    

    
}
