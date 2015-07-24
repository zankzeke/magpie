package magpie.data.materials;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import magpie.attributes.generators.element.ElementalPropertyAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import magpie.data.materials.util.LookupData;
import magpie.data.materials.util.PropertyLists;

/**
 * Store elemental properties. For example, this could be the dilute solution 
 * solution energies of elements in zirconia (see: <a href="http://pubs.acs.org/doi/abs/10.1021/cm403727z">
 * Meredig and Wolvertion, 2014</a>). 
 * 
 * <p>Input files should have element abbreviations as the first column, and measured
 * property values as the next columns. If a property is known for a certain entry,
 * list a string that doesn't parse to a number (e.g., "None"). The header should
 * be a list of property names. If a property has multiple classes, list them
 * inside {}'s (ex: stability{Yes,No}). An example data file could look like:
 * 
 * <div style="margin-left: 25px; font-family:monospace">
 * <p>element sln_energy valency{2+,3+,4+}
 * <br>Al 0.78 3+
 * <br>Ni None 2+
 * <br>Fe 0.39 None
 * </div>
 * 
 * <p>By default, this class uses the properties of an element as attributes.
 * 
 * <p><b><u>Implemented Commands:</u></b>
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
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class ElementDataset extends MultiPropertyDataset {
    /** Directory containing elemental property lookup tables */
    public String LookupDirectory = "./Lookup Data/";
    /** List of elemental properties used when computing attributes */
    public List<String> ElementalProperties = new ArrayList<>();
    /** Lookup table of elemental properties */
    public Map<String, double[]> PropertyData = LookupData.ElementalProperties;

    /**
     * Create a dataset using the default attribute generators:
     * 
     * <ol>
     * <li>{@linkplain ElementalPropertyAttributeGenerator}
     * </ol>
     * 
     */
    public ElementDataset() {
        this(true);
    }
    
    /**
     * Create a blank dataset.
     * @param useDefaultGenerators Whether to use default attribute generators
     * @see #ElementDataset() 
     */
    public ElementDataset(boolean useDefaultGenerators) {
        if (useDefaultGenerators) {
            Generators.add(new ElementalPropertyAttributeGenerator());
        } 
    }

    @Override
    public ElementDataset emptyClone() {
        ElementDataset x = (ElementDataset) super.emptyClone(); 
        x.ElementalProperties = new ArrayList<>(AttributeName);
        return x;
    }

    @Override
    public void importText(String filename, Object[] options) throws Exception {
        // Attempt to open file
        BufferedReader fp = new BufferedReader(new FileReader(filename));
        
        // Read in the header
        String header = fp.readLine();
        importPropertyNames(header);
        
        // Read in data
        String line = fp.readLine();
        while (line != null) {            
            // Read in a new entry
            String[] words = line.split("\\s+");
            ElementEntry entry = new ElementEntry(words[0]);
            double[] props = importEntryProperties(words);
            entry.setMeasuredProperties(props);
            addEntry(entry);
            
            // Get next 
            line = fp.readLine();
        }
        
        // Close file
        fp.close();
    }

    @Override
    public BaseEntry addEntry(String entry) throws Exception {
        ElementEntry e = new ElementEntry(entry);
        addEntry(e);
        return e;
    }

    @Override
    public ElementEntry getEntry(int index) {
        return (ElementEntry) super.getEntry(index); 
    }

    /**
     * Define the list of elemental properties to be used to compute attributes.
     * @param properties List of elemental properties
     */
    public void setElementalProperties(List<String> properties) {
        this.ElementalProperties = new ArrayList<>(properties);
    }

    /**
     * Get the list of elemental properties to be used to compute attributes.
     * @return List of elemental properties
     */
    public List<String> getElementalProperties() {
        return new ArrayList<>(ElementalProperties);
    }
    
    /**
     * Add an elemental property to the list of those used when computing attributes.
     * @param name Name of elemental property
     */
    public void addElementalProperty(String name) {
        if (! ElementalProperties.contains(name)) {
            ElementalProperties.add(name);
        }
    }
    
    /**
     * Remove an elemental property from the list of those used when computing attributes.
     * @param name Name of elemental property to be removed
     * @return Whether anything was moved
     */
    public boolean removeElementalProperty(String name) {
        return ElementalProperties.remove(name);
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
     * Get an elemental property lookup table
     * @param prop Name of property
     * @return Elemental properties, ordered by atomic number
     * @throws Exception 
     */
    public double[] getLookupTable(String prop) throws Exception {
        double[] table = PropertyData.get(prop);
        if (table == null) {
            table = LookupData.loadPropertyLookupTable(LookupDirectory, prop);
            PropertyData.put(prop, table);
        }
        return table;
    }

    @Override
    protected void calculateAttributes() throws Exception {
        // Nothing special to do
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
                LookupDirectory = Command.get(1).toString();
                for (int i = 2; i < Command.size(); i++) {
                    LookupDirectory += " " + Command.get(i).toString();
                }
            }
            break;
            default:
                throw new Exception("ERROR: Property command not recognized: " + Action);
        }
        return null;
    }

    @Override
    public String printEntryDescription(boolean htmlFormat) {
        return "Element from periodic table";
    }
    
}
