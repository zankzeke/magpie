package magpie.data.materials;

import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.attributes.generators.composition.*;
import magpie.data.MultiPropertyDataset;
import magpie.data.materials.util.LookupData;
import magpie.data.materials.util.PropertyLists;
import magpie.data.utilities.output.CompositionOutput;
import magpie.utility.tools.OxidationStateGuesser;

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
public class CompositionDataset extends MultiPropertyDataset {

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
    public String DataDirectory = "./lookup-data";
    /**
     * List of properties used when generating attributes
     */
    protected List<String> ElementalProperties = new ArrayList<>();
    /**
     * Map of elemental property names to values
     */
    public SortedMap<String, double[]> PropertyData = LookupData.ElementalProperties;
    /** Oxidation states of every element */
    protected double[][] OxidationStates = LookupData.OxidationStates;

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
     * <li>{@linkplain ValenceShellAttributeGenerator}: Attributes based on the 
     * valence shell electrons of the constituent elements
     * <li>{@linkplain ElementalPropertyAttributeGenerator}: Generate attributes
     * based on properties of constituent elements. Use the list of attributes
     * provided through "attributes properties add" or 
     * {@linkplain #addElementalProperty(java.lang.String) } operations for
     * the commandline and Java interfaces, respectively.
     * <li>{@linkplain IonicityAttributeGenerator}: Attributes designed to represent
     * the "ionicity" of a compound.
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
    public CompositionDataset createTemplate() {
        CompositionDataset data = (CompositionDataset) super.createTemplate();
        
        // Ensure links with LookupData are broken
        if (OxidationStates != null) {
            data.OxidationStates = OxidationStates.clone();
        }
        data.PropertyData = new TreeMap<>(PropertyData);
        
        return data;
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
        BufferedReader is = new BufferedReader(new FileReader(filename));
        String line;
        String[] words;

        // Read in properties from header
        line = is.readLine();
        line = line.trim();
        importPropertyNames(line);

        // Read in each entry
        CompositionEntry entry;
        for (int e = 0; e < Entry_Count; e++) {
            double[] properties;
            // Read a line and tokenize it
            line = is.readLine();
            if (line == null) {
                break;
            }
            words = line.trim().split("\\s+");
            if (words.length < 2) {
                continue;
            }

            // Read properties
            properties = importEntryProperties(words);

            // Make an entry
            try {
                entry = new CompositionEntry(words[0]);
            } catch (Exception ex) {
                continue; // Skip if fails to parse
            }
            entry.setMeasuredProperties(properties);
            
            addEntry(entry);
        }

        // Close the file
        is.close();
    }
	    
    /**
     * Set whether to use composition (i.e. fraction of each element present) as attributes.
     * 
     * <p>By default: This class does not use fractions as attributes.
     * @param decision Whether to use it or not
     */
    public void useCompositionAsAttributes(boolean decision) {
        boolean found = false;
        Iterator<BaseAttributeGenerator> iter = Generators.iterator();
        while (iter.hasNext()) {
            if (iter.next() instanceof ElementFractionAttributeGenerator) {
                // Remove it, if user wants to 
                if (!decision) {
                    iter.remove();
                    return;
                } else {
                    return;
                }
            }
        }
        
        // Add in, if desired
        if (!found && decision) {
            Generators.add(0, new ElementFractionAttributeGenerator());
        } 
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
     */
    public void setDataDirectory(String directory) {
        if (! Files.isDirectory(Paths.get(directory))) {
            throw new IllegalArgumentException("No such directory: " + directory);
        }
        this.DataDirectory = directory;
    }

    /**
     * Get path to directory containing elemental property lookup-data
     * @return directory Path to the lookup-data directory
     */
    public String getDataDirectory() {
        return DataDirectory;
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
     * Add a set of elemental properties to the list of those used when computing attributes.
     * @param setName Name of set
     * @throws java.lang.Exception
     * @see PropertyLists#getPropertySet(java.lang.String) 
     */
    public void addElementalPropertySet(String setName) throws Exception {
        for (String name : PropertyLists.getPropertySet(setName)) {
            addElementalProperty(name);
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
        // Nothing to do
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

        double[] table = LookupData.loadPropertyLookupTable(DataDirectory, PropertyName);
        PropertyData.put(PropertyName, table);
        return table;
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
            throw new RuntimeException("Oxidation states failed to read due to " + e);
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
                CompositionOutput output = new CompositionOutput();
                output.setSelectionMethod(CompositionOutput.ElementSelectionMethod.DYNAMIC);
                new CompositionOutput().writeDataset(this, filename);
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
