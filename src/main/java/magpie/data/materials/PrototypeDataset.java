
package magpie.data.materials;

import magpie.data.materials.util.PrototypeSiteInformation;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import magpie.attributes.generators.prototype.SingleSiteAttributeGenerator;
import magpie.data.BaseEntry;
import org.json.JSONObject;

/**
 * Store data about compounds based on a single crystal structure prototype. User 
 * must specify the sites in the crystal, the number of atoms on each site, 
 * and which sites are equivalent. For example, this class could be used to represent a
 * dataset composed of Heusler compounds, where there are 3 sites and two are
 * equivalent.
 * 
 * <p>Information about each site must be specified with a file in the following format:
 * 
 * <p>&lt;# of atoms on Site1> [-omit] [-equiv &lt;site #s...>]<br>
 * &lt;# of atoms on Site2> [-omit] [-equiv &lt;site #s...>]
 * 
 * <p>Here sites (aka sublattices) can be occupied by a single kind of atom, and are treated as symmetrically
 * distinct unless otherwise specified. Information about each site is used to calculate attributes unless otherwise
 * indicated by an "-omit" flag. It is possible to mark equivalent sites using the "-equiv" flag.
 * 
 * <p>The required format of data files is very similar to that of a {@linkplain  CompositionDataset}:
 * 
 * <p>Identity [&lt;property1 Name>] [&lt;property2 Name>] [...]<br>
 * &lt;A site Element>&lt;B site Element>&lt;...> &lt;property1 value> [...]<br>
 * {&lt;Composition of Site A>}{&lt;Composition of Site B} &lt;property1 value> [...]<br>
 * [and so on]<br>
 * 
 * <p>It is important that you can define the identity of an crystal by either specifying the 
 * element present on each site, or by specifying the composition of the elements occupying that
 * site. The latter option allows this class to handle mixing on a certain atomic site. Note that this
 * does <i>not</i> consider the ordering (or lack there of) on that sublattice.
 * 
 * <usage><p><b>Usage</b>: &lt;Structure description filename>
 * <br><pr><i>Structure description filename</i>: File containing description of atomic sites in structure</usage>
 * 
 * @author Logan Ward
 */
public class PrototypeDataset extends CompositionDataset {
    /** Stores information about each site */
    protected PrototypeSiteInformation SiteInfo = new PrototypeSiteInformation();

    /**
     * Create a prototype dataset with default attribute
     */
    public PrototypeDataset() {
        this(true);
    }
    
    /**
     * Create a prototype dataset. Gives user options of whether to use
     * the "default" attribute generators for this class:
     * 
     * <ol>
     * <li>{@linkplain SingleSiteAttributeGenerator}: Statistics about the properties
     * of elements on each site.
     * </ol>
     * @param useDefaultGenerators Whether 
     */
    public PrototypeDataset(boolean useDefaultGenerators) {
        super(useDefaultGenerators);
        
        if (useDefaultGenerators) {
            Generators.add(0, new SingleSiteAttributeGenerator());
        }
    }
    

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() != 1)
            throw new IllegalArgumentException(printUsage());
        readStructureInformation(Options.get(0).toString());
    }

    @Override
    public String printUsage() {
        return "Usage: <Structure description filename>";
    }

    @Override
    public PrototypeDataset emptyClone() {
        PrototypeDataset x = (PrototypeDataset) super.emptyClone();
        x.SiteInfo = this.SiteInfo;
        return x; 
    }
    
    /**
     * Read information about a prototype crystal structure from file. 
     * See documentation for this class for format information.
     * @param filename File containing structure information.
     * @throws Exception If file fails to parse
     */
    public void readStructureInformation(String filename) throws Exception {
        SiteInfo = PrototypeSiteInformation.readFromFile(filename);
    }

    /**
     * Define information about the prototypes contained by this dataset. 
     * <p>NOTE: This will clear all existing data
     * @param SiteInfo Object describing the number of atoms on each site, etc.
     */
    public void setSiteInfo(PrototypeSiteInformation SiteInfo) {
        clearData();
        this.SiteInfo = SiteInfo;
    }

    /**
     * Get information about each site in this prototype structure
     * @return Site information for this dataset
     */
    public PrototypeSiteInformation getSiteInfo() {
        return SiteInfo;
    }    
        
    @Override
    public void importText(String filename, Object[] options) throws Exception {
        // Define the reader
        BufferedReader is = Files.newBufferedReader(Paths.get(filename), Charset.forName("US-ASCII"));
        String line;
        String[] words;
        
        // Read in the header
        line = is.readLine();
        importPropertyNames(line);
        
        // Read in each entry
        ArrayList<BaseEntry> acceptedEntries = new ArrayList<>();
        while ((line = is.readLine()) != null) {
            double[] properties;
            // Read a line and tokenize it
            words = line.trim().split("\\s+");

            // Get the properties
            properties = importEntryProperties(words);
            
            // Make an entry
            PrototypeEntry entry = new PrototypeEntry(SiteInfo, words[0]);
            entry.setMeasuredProperties(properties);
            
            acceptedEntries.add(entry);
        }
        // Close the file
        is.close();
        
        // Copy the entries
        this.Entries = acceptedEntries;
    }
    
    /**
     * @return Number of sites in prototype crystal
     */
    public int NSites() {
        return SiteInfo.NSites();
    }

    @Override
    public PrototypeEntry getEntry(int index) {
        return (PrototypeEntry) super.getEntry(index); 
    }

	@Override
	public PrototypeEntry addEntry(String input) throws Exception {
		PrototypeEntry toAdd = new PrototypeEntry(SiteInfo, input);
		addEntry(toAdd);
		return toAdd;
	}
}
