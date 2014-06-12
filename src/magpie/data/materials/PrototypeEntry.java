/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.materials;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import magpie.data.materials.util.PrototypeSiteInformation;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Entry in a {@linkplain PrototypeCompositionDataset}. Stores which elements are on
 *  each site, and the corresponding composition.
 *  
 * @author Logan Ward
 * @version 0.1
 */
public class PrototypeEntry extends CompositionEntry {
    /** Link to site information from corresponding dataset */
    final protected PrototypeSiteInformation SiteInfo;
    /** Composition of each site */
    protected CompositionEntry[] SiteComp;

    /**
     * Create a new PrototypeEntry. See {@linkplain PrototypeCompositionDataset}
     * @param SiteInfo Information about each site in prototype crystal
     * @param Composition String describing elements on each site
     * @param ElementList Names of elements (useful for printing)
     * @param SortingOrder In what order to sort elements of the same type (
     */
    public PrototypeEntry(PrototypeSiteInformation SiteInfo, String Composition, String[] ElementList, int[] SortingOrder) {
        // Define the site information 
        this.SiteInfo = SiteInfo;
        this.Elem_List = ElementList;
        this.SortingOrder = SortingOrder;
        
        // Define the array of site compositions
        SiteComp = new CompositionEntry[SiteInfo.NSites()];
        
        // Get composition of each site
        Matcher compMatch;
        compMatch = Pattern.compile("\\{.*\\}|[A-Z][a-z]?").matcher(Composition);
        int pos = 0;
        while (compMatch.find()) {
            if (pos >= SiteInfo.NSites() )
                throw new Error("Composition contains more sites than defined in SiteInfo: " + Composition);
            String comp = compMatch.group();
            CompositionEntry newSite = new CompositionEntry(comp, ElementList, SortingOrder);
            SiteComp[pos++] = newSite;
        }
        if (pos != SiteInfo.NSites()) 
            throw new Error("Composition contains fewer sites than defined in SiteInfo: " + Composition);
        
        // Get composition of this crystal
        calculateComposition();
        
        // Rectify compositions (for printing order)
        rectifyEntry();
    }

    /**
     * Create a Prototype entry without specifying composition. These must be defined
     *  later for the entry to be usable (by using {@linkplain #setSiteComposition(int, magpie.data.oqmd.CompositionEntry) })
     * @param SiteInfo Information about each site in prototype crystal
     * @param ElementList Names of elements (useful for printing)
     * @param SortingOrder In what order to sort elements of the same type
     */
    public PrototypeEntry(PrototypeSiteInformation SiteInfo, String[] ElementList, int[] SortingOrder) {
        this.SiteInfo = SiteInfo;
        // Make all sites have H on them
        this.SiteComp = new CompositionEntry[SiteInfo.NSites()];
        CompositionEntry blank = new CompositionEntry(new int[]{0}, 
                new double[]{1.0}, ElementList, SortingOrder);
        for (int i=0; i<SiteComp.length; i++) {
            this.SiteComp[i] = blank.clone();
        }
        this.Elem_List = ElementList;
        this.SortingOrder = SortingOrder;
    }
    
    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public PrototypeEntry clone() {
        PrototypeEntry x = (PrototypeEntry) super.clone(); 
        x.SiteComp = SiteComp.clone();
        return x;
    }
    
    /**
     * Given the composition of each site and the number of atoms on that site,
     *  calculate the composition of this crystal
     */
    private void calculateComposition() {
        // Sum the amount of each type of elementfrom every site
        Map<Integer, Double> composition = new TreeMap<>();
        for (int s=0; s < SiteInfo.NSites(); s++) {
            int[] elems = SiteComp[s].getElements();
            double[] fracs = SiteComp[s].getFractions();
            for (int e=0; e<elems.length; e++) {
                if (! composition.containsKey(elems[e])) {
                    composition.put(elems[e], fracs[e] * (double) SiteInfo.NOnSite(s));
                } else {
                    Double curFrac = composition.get(elems[e]);
                    curFrac += fracs[e] * (double) SiteInfo.NOnSite(s);
                    composition.put(elems[e], curFrac);
                }
            }
        }
        // Store result
        int nElem = composition.size();
        Element = new int[nElem]; Fraction = new double[nElem];
        int pos = 0;
        for (Map.Entry<Integer,Double> entry : composition.entrySet()) {
            Element[pos] = entry.getKey();
            Fraction[pos] = entry.getValue();
            pos++;
        }
    }
    
    /**
     * Get composition of a certain site
     * @param index Desired site index
     * @return Composition of that site
     */
    public CompositionEntry getSiteComposition(int index) {
        return SiteComp[index].clone();
    }
    
    /**
     * Set the composition of a certain site
     * @param index Index of site in question
     * @param composition Desired composition
     */
    public void setSiteComposition(int index, CompositionEntry composition) {
        SiteComp[index] = composition.clone();
        calculateComposition();
        rectifyEntry();
    }

    @Override
    public boolean equals(Object other) {
        if (! (other instanceof PrototypeEntry))
            return false;
        PrototypeEntry B = (PrototypeEntry) other;
        if (! SiteInfo.equals(B.SiteInfo))
            return false;
		// See if groups of sites are equivalent
		for (int i=0; i<SiteInfo.NGroups(); i++) {
			Set<Integer> siteIDs = SiteInfo.getSiteGroup(i);
			// Get composition of sites in that group in this prototype
			List<CompositionEntry> siteCompositions = new LinkedList<>();
			for (Integer site : siteIDs) siteCompositions.add(SiteComp[site]);
			// Look to see if the other entry has corresponding sites
			for (Integer site : siteIDs) {
				if (! siteCompositions.contains(B.getSiteComposition(site)))
					return false;
			}
		}
		// If it passes all the tests
        return true;
    }
	
	/**
	 * Generate a list of prototypes equivalent to this entry.
	 * @return List of all entries that would be equivalent to this one
	 */
	public List<PrototypeEntry> getEquivalentPrototypes() {
		List<int[]> equivArrangments = SiteInfo.getEquivalentArragements();
		// Generate list
		List<PrototypeEntry> output = new LinkedList<>();
		for (int[] arr : equivArrangments) {
			PrototypeEntry equiv = this.clone();
			for (int i=0; i<NSites(); i++)
				equiv.setSiteComposition(i, getSiteComposition(arr[i]));
			output.add(equiv);
		}
		return output;
	}

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.SiteInfo);
        hash = 89 * hash + Arrays.deepHashCode(this.SiteComp);
        return hash;
    }

    @Override
    public int compare(Object A_obj, Object B_obj) {
        // Make sure both are PrototypeEntry objects
        if (! (A_obj instanceof PrototypeEntry))
            return -1;
        if (! (B_obj instanceof PrototypeEntry))
            return 1;
        PrototypeEntry A = (PrototypeEntry) A_obj;
        PrototypeEntry B = (PrototypeEntry) B_obj;
        
        // First: Compare number of sites
        int comp = Integer.compare(A.SiteInfo.NSites(), B.SiteInfo.NSites());
        if (comp != 0) 
            return comp;
		
		// Next: Make sure they are not equivalent
		if (A.equals(B)) return 0;
        
        // Next: Compare composition of each site 
        for (int i=0; i<A.SiteInfo.NSites(); i++) {
            comp = A.SiteComp[i].compareTo(B.SiteComp[i]);
            if (comp != 0) 
                return comp;
        }
        
        // We are complete
        return 0;
    }
    
    /**
     * @return Number of sites in prototype crystal structure
     */
    public int NSites() {
        return SiteInfo.NSites();
    }
    
    /**
     * Calculate the mean of an elemental property for a certain site
     * @param index Desired site index
     * @param lookup Lookup table of elemental properties
     * @return Composition-weighted mean of supplied elemental property on that site
     */
    public double getSiteMean(int index, double[] lookup) {
        return SiteComp[index].getMean(lookup);
    }
    
    /**
     * Calculate the mean of an elemental property for a certain site
     * @param index Desired site group index
     * @param lookup Lookup table of elemental properties
     * @return Composition-weighted mean of supplied elemental property on that site
     */
    public double getSiteGroupMean(int index, double[] lookup) {
        double[] res = new double[SiteInfo.getSiteGroup(index).size()];
        int pos=0;
        for (int site : SiteInfo.getSiteGroup(index)) {
            res[pos++] = getSiteMean(site, lookup);
        }
        return StatUtils.mean(res);
    }

    @Override
    public String toHTMLString() {
        String output = "";
        DecimalFormat df = new DecimalFormat("#.00");
        for (int i=0; i<NSites(); i++) {
            if (SiteComp[i].Element.length > 1) {
                output += "{" + SiteComp[i].toHTMLString() + "}";
            } else {
                output += Elem_List[SiteComp[i].Element[0]];
            }
            if (SiteInfo.NOnSite(i) != 1)
                output += df.format(SiteInfo.NOnSite(i));
        }
        return output;
    }

    @Override
    public String toString() {
        String output = "";
        DecimalFormat df = new DecimalFormat("#.00");
        for (int i=0; i<NSites(); i++) {
            if (SiteComp[i].Element.length > 1) {
                output += "{" + SiteComp[i].toString() + "}";
            } else {
                output += Elem_List[SiteComp[i].Element[0]];
            }
            if (SiteInfo.NOnSite(i) != 1)
                output += df.format(SiteInfo.NOnSite(i));
        }
        return output;
    }
    
    
}
