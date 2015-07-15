package magpie.data.materials;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import magpie.attributes.generators.composition.*;
import magpie.data.BaseEntry;
import magpie.data.materials.util.CompositionDatasetOutput;
import magpie.data.materials.util.LookupData;
import magpie.data.materials.util.PropertyLists;
import magpie.utility.tools.OxidationStateGuesser;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class stores entries that describe a material based solely on its
 * composition.
 * 
 * <p>Some of the available features include:
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
 * <p><b><u>Attributes</u></b>
 * 
 * Generates several categories of attributes by default, which are described
 * in {@linkplain #CompositionDataset(boolean) }.
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
    protected List<String> ElementalProperties = new ArrayList<>();
    /**
     * Map of elemental property names to values
     */
    public Map<String, double[]> PropertyData = LookupData.ElementalProperties;
	/** Oxidation states of every element */
	protected double[][] OxidationStates = LookupData.OxidationStates;
	
    /** Whether to use composition as attributes */
    protected boolean UseComposition = false;

    /**
     * Create a dataset using the default set of attribute generators. 
     */
    public CompositionDataset() {
        this(true);
    }
    
    /**
     * Create a blank dataset. Default attribute generator set:
     * 
     * <ol>
     * <li>{@linkplain StoichiometricAttributeGenerator}: Generate attributes
     * based on fractions of elements. Does not depend on what the elements actually
     * are. Will use p = 2, 3, 5, 7, 10 norms.
     * <li>{@linkplain ElementalPropertyAttributeGenerator}: Generate attributes
     * based on properties of constituent elements. Use the list of attributes
     * provided through "attributes properties add" or 
     * {@linkplain #addElementalProperty(java.lang.String) } operations for
     * the commandline and Java interfaces, respectively.
     * </ol>
     * @param useDefaultGenerators [in] Whether to use default generators
     */
    public CompositionDataset(boolean useDefaultGenerators) {
        if (!useDefaultGenerators) {
            return;
        }
        
        try {
            // Add stoichiometric generator
            StoichiometricAttributeGenerator sgen = new StoichiometricAttributeGenerator();
            sgen.addPNorm(2); sgen.addPNorm(3); 
            sgen.addPNorm(5); sgen.addPNorm(7); sgen.addPNorm(10);
            addAttribueGenerator(sgen);
            
            // Add elemental property generator
            ElementalPropertyAttributeGenerator pgen;
            pgen = new ElementalPropertyAttributeGenerator();
            addAttribueGenerator(pgen);
            
            // Add valence shell attributes
            ValenceShellAttributeGenerator vgen = new ValenceShellAttributeGenerator();
            addAttribueGenerator(vgen);
            
            // Add ionicity attributes
            IonicityAttributeGenerator igen = new IonicityAttributeGenerator();
            addAttribueGenerator(igen);
        } catch (Exception e) {
            throw new Error(e);
        }
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
        // Clear out entry data
        clearData();
        
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
            try {
                Entry = new CompositionEntry(words[0]);
            } catch (Exception ex) {
                continue; // Skip if fails to parse
            }
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
            } catch (Exception exc) {
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
        // Clear out current property data
        clearPropertyData();
        
        // Initialize regex
		Pattern totalPattern = Pattern.compile("[\\d\\w]+(\\{.*\\})?"), // Captures entire name/classes 
				namePattern = Pattern.compile("^[\\d\\w]+"), // Given name/classes, get name
				classPattern = Pattern.compile("\\{.*\\}"); // Get the possible classes
		Matcher totalMatcher = totalPattern.matcher(line);
		totalMatcher.find(); // First match is composition
        
        // Find all property names
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
     * @param directory Path to the elemental property lookup directory
     * @throws java.lang.Exception
     */
    public void setDataDirectory(String directory) throws Exception {
        if (! Files.isDirectory(Paths.get(directory))) {
            throw new Exception("No such directory: " + directory);
        }
        this.DataDirectory = directory;
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
     * Get list of elemental properties currently being used to generate attributes.
     * @return List of elemental properties
     */
    public List<String> getElementalProperties() {
        return new ArrayList<>(ElementalProperties);
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

    @Override
    protected void calculateAttributes() {
        // --> Create attributes based on elemental fractions, if desired
        if (UseComposition) {
            generateElementFractionAttributes();
        }
    }

    /**
     * Reads in a data file that contains properties for each element.
     * MeasuredProperty list should be contained in a file named
     * PropertyName.table
     *
     * @param PropertyName MeasuredProperty of interest
     * @return That property for each element
     * @throws java.lang.Exception
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
     * Get the list of known oxidation states
     * @return List of oxidation states
     */
    public double[][] getOxidationStates() {
        if (OxidationStates == null) {
            readOxidationStates();
        }   
        return OxidationStates;
    }

    /**
     * Reads in a data file that contains known oxidation states for each
     * element. List should be contained in a file named OxidationStates.table
     */
    protected void readOxidationStates() {
        // Open the file for reading
        Path datafile = Paths.get(DataDirectory);
        BufferedReader is;
        try {
            is = Files.newBufferedReader(
                    datafile.resolve("OxidationStates.table"), Charset.forName("US-ASCII"));

            // Read the file
            int i, j; // Counters
            OxidationStates = new double[ElementNames.length][];
            for (i = 0; i < ElementNames.length; i++) {
                String[] States = is.readLine().split(" ");
                if (States[0].isEmpty()) {
                    OxidationStates[i] = new double[0];
                } else {
                    OxidationStates[i] = new double[States.length];
                    for (j = 0; j < OxidationStates[i].length; j++) {
                        OxidationStates[i][j] = Double.parseDouble(States[j]);
                    }
                }
            }
            is.close();
        } catch (IOException | NumberFormatException e) {
            throw new Error("Oxidation states failed to read due to " + e);
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
     * Generate the percent ionic character for each entry
     */
    protected void generateIonicCharacter() {
        double x;
        
    }

    /**
     * Determine whether each entry can form an ionic compound. Assumes that
     * each element can only take on a single oxidation state
     */
    protected void generateCanFormIonic() {
        AttributeName.add("CanFormIonic");

        for (BaseEntry ptr : getEntries()) {
            CompositionEntry entry = (CompositionEntry) ptr;
            double x = compositionCanFormIonic(entry) ? 1 : 0;
            entry.addAttribute(x);
        }
    }

    /**
     * Whether a composition can form a neutral compound assuming each element
     * takes only a single oxidation state.
     * @param entry Composition to be assessed
     * @return Whether it can form an ionic compound
     */
    public boolean compositionCanFormIonic(CompositionEntry entry) {
        OxidationStateGuesser g = new OxidationStateGuesser();
        
        try {
            g.setElectronegativity(getPropertyLookupTable("Electronegativity"));
            g.setOxidationStates(getOxidationStates());
        } catch (Exception e) {
            throw new Error(e);
        }
        
        return ! g.getPossibleStates(entry).isEmpty();
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

    @Override
    public String printEntryDescription(boolean htmlFormat) {
        return "List of elements and their relative proportions";
    }
    
}
