/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.materials;

import magpie.data.materials.util.PrototypeSiteInformation;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import magpie.data.BaseEntry;
import magpie.utility.UtilityOperations;

/**
 * Dataset designed to store composition data about compounds based on a single crystal structure
 *  prototype. Statistics are only based on composition (i.e. nothing based on the
 *  actual structure). User must specify the sites in the crystal, and provide the 
 *  identity of the species listed on each site.
 * 
 * <p>Information about each site must be specified with a file in the following format:
 * 
 * <p>&lt;# of atoms on Site1> [-omit] [-equiv &lt;site #s...>]<br>
 * &lt;# of atoms on Site2> [-omit] [-equiv &lt;site #s...>]
 * 
 * <p>Here sites (aka sublattices) can be occupied by a single kind of atom, and are treated as symmetrically
 * distinct unless otherwise specified. Information about each site is used to calculate statistics unless otherwise
 * indicated by an "-omit" flag. It is possible to mark equivalent sites using the "-equiv" flag.
 * 
 * <p>Data is read into this object using the {@linkplain #importText(java.lang.String)} command. 
 * The required format of data files is very similar to that of a {@linkplain  CompositionDataset}:
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
 * <p>In addition to attributes based solely on the composition (see {@linkplain CompositionDataset#generateAttributes(java.lang.String[], boolean, java.lang.String)}),
 * this class generates other statistics, as described in {@linkplain #generateAttributes(java.lang.String[], boolean, java.lang.String) }
 * 
 * <usage><p><b>Usage</b>: &lt;Structure description filename>
 * <br><pr><i>Structure description filename</i>: File containing description of atomic sites in structure</usage>
 * 
 * 
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PrototypeDataset extends CompositionDataset {
    /** Stores information about each site */
    protected PrototypeSiteInformation SiteInfo = new PrototypeSiteInformation();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() != 1)
            throw new Exception(printUsage());
        readStructureInformation(Options.get(0).toString());
    }

    @Override
    public String printUsage() {
        return "Usage: <Structure description filename>";
    }

    @Override
    public PrototypeDataset clone() {
        PrototypeDataset x = (PrototypeDataset) super.clone();
        x.SiteInfo = this.SiteInfo;
        return x; 
    }
    
    /**
     * Read information about a prototype crystal structure from file. See documentation
     *  for this class for format information.
     * @param filename File containing structure information.
     */
    public void readStructureInformation(String filename) {
        // Open file
        BufferedReader is;
        try { 
            is = Files.newBufferedReader(Paths.get(filename), Charset.forName("US-ASCII"));
        } catch (IOException e) {
            throw new Error(e);
        }
        
        // Read in information about each site
        while (true) {
            String Line;
            try {
                Line = is.readLine();
            } catch (IOException e) {
                throw new Error(e);
            }
            if (Line == null) break;
            String[] Words = Line.split(" ");
            
            // Get the number of atoms on this site
            double NAtoms;
            try {
                NAtoms = Double.parseDouble(Words[0]);
            } catch (NumberFormatException n) {
                throw new Error("Site information file format error");
            }
            
            // Check for any other flags (setting equivalent sites, etc.)
            int p=1;
            boolean isOmitted = false;
			List<Integer> equivSites = new LinkedList<>();
            while (p < Words.length) {
                switch (Words[p].toLowerCase()) {
                    case "-omit":
                        isOmitted = true;
                        break;
                    case "-equiv":
						p++;
                        while (p < Words.length
                                && UtilityOperations.isInteger(Words[p+1])) {
                            equivSites.add(Integer.parseInt(Words[p++]));
                            if (p == Words.length) {
                                break;
                            }
                        }
                        break;
                    default:
                        throw new Error("Site information file format error.");
                }
                p++;
            }
            
            // Store the site in list
            SiteInfo.addSite(NAtoms, !isOmitted, equivSites);
        }
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
        // Count the number of lines (1 per entry + 1 header)
        // Thanks to: http://stackoverflow.com/questions/453018/number-of-lines-in-a-file-in-java
        LineNumberReader lr = new LineNumberReader(new FileReader(filename));
        while (lr.skip(Long.MAX_VALUE) > 0 ) {};
        int Entry_Count = lr.getLineNumber() + 1;
        lr.close();
        
        // Define the reader
        BufferedReader is = Files.newBufferedReader(Paths.get(filename), Charset.forName("US-ASCII"));
        String Line; String[] Words;
        
        // Read in the header
        Line = is.readLine();
        Words = Line.split("[\t ]");
        for (int i=1; i<Words.length; i++)  addProperty(Words[i]);
                
        // Read in each entry
        TreeMap<BaseEntry,CompositionEntry> acceptedEntries = new TreeMap<>();
        CompositionEntry Entry;
        for (int i=0; i<Entry_Count; i++){
            double[] properties;
            // Read a line and tokenize it
            Line = is.readLine();
            if (Line == null) 
                break;
            Words=Line.split("[ \t]");
            if (Words.length == 1) // For blank lines
                continue;
            
            // Get the properties
            properties = new double[NProperties()];
            for (int j=0; j<NProperties(); j++) {
                try { properties[j]=Double.parseDouble(Words[j+1]); }
                catch (NumberFormatException e) { 
                    properties[j]=Double.NaN;
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("\tEntry with name \"" + Words[0] 
                        + "\" only has " + (Words.length - 1) + " properties" );
                }
            }
            
            // Make an entry
            Entry = new PrototypeEntry(SiteInfo, Words[0]);
            Entry.setMeasuredProperties(properties);
            
            acceptedEntries.put(Entry, Entry);
        }
        // Close the file
        is.close();
        
        // Copy the entries
        this.Entries = new ArrayList<>(acceptedEntries.keySet());
    }
    
    /**
     * @return Number of sites in prototype crystal
     */
    public int NSites() {
        return SiteInfo.NSites();
    }

    @Override
    public void generateAttributes() {
        // Generate attritubutes based on elemental properties for each site
        generateSingleSiteAttributes();
        
        // Generate attributes based on differences between properities of two sites
        generatePairSiteAttributes();
        
        // Generate attibutes based on composition only
        super.generateAttributes();
    }
    
    /**
     * Generate attributes based on the mean elemental property for each site group.
     *  (i.e. the average electronegativity of elements for sites in group 1)
     */
    protected void generateSingleSiteAttributes() {
        // Determine number of sites that can be used for generating attributes
        int nGroups = 0; 
        for (int i=0; i < SiteInfo.NGroups(); i++) 
            if (SiteInfo.groupIsIncludedInAttributes(i)) nGroups++;
        
        // Add attribute names
        for (String p : ElementalProperties)
            for (int g = 0; g<SiteInfo.NGroups(); g++)
                if (SiteInfo.groupIsIncludedInAttributes(g))
                    AttributeName.add(SiteInfo.getGroupLabel(g) + ":mean_" + p);
            
        // Calculate attributes for each entry
        int nAttr = nGroups * ElementalProperties.size();
        for (int e=0; e<NEntries(); e++) {
            PrototypeEntry E = getEntry(e);
            int pos = 0;
            double[] newAttr = new double[nAttr];
            for (String p : ElementalProperties) {
                double[] lookup = getPropertyLookupTable(p);
                for (int g=0; g<SiteInfo.NGroups(); g++)
                    if (SiteInfo.groupIsIncludedInAttributes(g))
                        newAttr[pos++] = E.getSiteGroupMean(g, lookup);
            }
            E.addAttributes(newAttr);
        }
    }
    
    /**
     * Generate attributes related to the differences between a pair of sites
     */
    protected void generatePairSiteAttributes() {
        // Determine number of sites that can be used for generating attributes
        int nGroups = 0; 
        for (int i=0; i < SiteInfo.NGroups(); i++) 
            if (SiteInfo.groupIsIncludedInAttributes(i)) nGroups++;
        int nPairs = nGroups * (nGroups - 1) / 2;
        
        // Add attribute names
        for (String p : ElementalProperties)
            for (int g1 = 0; g1<SiteInfo.NGroups(); g1++)
                for (int g2 = g1 + 1; g2<SiteInfo.NGroups(); g2++)
                    if (SiteInfo.groupIsIncludedInAttributes(g1) && 
                            SiteInfo.groupIsIncludedInAttributes(g2))
                        AttributeName.add(SiteInfo.getGroupLabel(g1) +
                                "-" + SiteInfo.getGroupLabel(g2) + ":mean_" + p);
            
        // Calculate attributes for each entry
        int nAttr = nPairs * ElementalProperties.size();
        for (int e=0; e<NEntries(); e++) {
            PrototypeEntry E = getEntry(e);
            int pos = 0;
            double[] newAttr = new double[nAttr];
            for (String p : ElementalProperties) {
                double[] lookup = getPropertyLookupTable(p);
                for (int g1=0; g1<SiteInfo.NGroups(); g1++)
                    for (int g2=g1+1; g2<SiteInfo.NGroups(); g2++)
                        if (SiteInfo.groupIsIncludedInAttributes(g1) && 
                                SiteInfo.groupIsIncludedInAttributes(g2))
                            newAttr[pos++] = E.getSiteGroupMean(g1, lookup) 
                                    - E.getSiteGroupMean(g2, lookup);
            }
            E.addAttributes(newAttr);
        }
    }

    @Override
    public PrototypeEntry getEntry(int index) {
        return (PrototypeEntry) super.getEntry(index); 
    }

	@Override
	public void addEntry(String input) {
		addEntry(new PrototypeEntry(SiteInfo, input));
	}
	
	
}
