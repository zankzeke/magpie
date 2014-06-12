/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.materials.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import magpie.utility.DistinctPermutationGenerator;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Stores information about all sites in a prototype crystal structure. 
 *  Has the capacity to store the following information:
 * <ol>
 * <li>Number of atoms on each site</li>
 * <li>Which groups are used to calculate attributes</li>
 * <li>Which sites in the crystal are equivalet</li>
 * </ol>
 * @author Logan Ward
 * @version 0.1
 */
public class PrototypeSiteInformation implements java.io.Serializable, Cloneable {
    /** Number of atoms on each site */
    private double[] NAtoms = new double[0];
    /** Which sites are included in attribute calculations */
    private boolean[] IsIncluded = new boolean[0];
	/** Groups of sites. Each entry in this list contains the index # of all
	 * sites in a group of equivalent sites.
	 */
	private List<Set<Integer>> SiteGroup = new LinkedList<>();
	/** List of all orders of sites that are equivalent */
	private List<int[]> equivalentArrangements = null;

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof PrototypeSiteInformation))
            return false;
        PrototypeSiteInformation B = (PrototypeSiteInformation) obj;
        if (! Arrays.equals(NAtoms, B.NAtoms))
            return false;
		if (! Arrays.equals(IsIncluded, B.IsIncluded))
			return false;
		return SiteGroup == B.SiteGroup;
    }

    @Override
    protected Object clone() {
        PrototypeSiteInformation x;
        try {
            x = (PrototypeSiteInformation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
        x.IsIncluded = Arrays.copyOf(IsIncluded, IsIncluded.length);
        x.NAtoms = Arrays.copyOf(NAtoms, NAtoms.length);
        x.SiteGroup = new LinkedList<>();
        for (Set<Integer> set : SiteGroup) {
            x.SiteGroup.add(new TreeSet<>(set));
        }
        x.equivalentArrangements = new LinkedList<>();
        for (int[] arrangement : equivalentArrangements) {
            x.equivalentArrangements.add(Arrays.copyOf(arrangement, arrangement.length));
        }
        return x;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Arrays.hashCode(this.NAtoms);
        return hash;
    }

    /**
     * Get the number of sites. Note: this is different than the number of groups 
	 *  of equivalent sites
     * @return Number of sites
	 * @see #NGroups()
     */
    public int NSites() {
        return NAtoms.length;
    }
	
	/**
	 * Get the number of groups of equivalent sites.
	 * @return Number of groups
	 * @see #NSites()
	 */
	public int NGroups() {
		return SiteGroup.size();
	}
	
	/** 
	 * Get indices of sites in a particular group.
	 * @param group Index of group
	 * @return Set of indicies corresponding to sites that are equivalent
	 */
	public Set<Integer> getSiteGroup(int group) {
		return SiteGroup.get(group);
	}
	
	/**
	 * Get all equivalent orders of sites. For example, if sites B and C are equivalent in 
	 *  a prototype with three sites: ABC and ACB are equivalent.
	 * @return List of equivalent arragments
	 */
	public List<int[]> getEquivalentArragements() {
		// If this has already been computed, return it
		if (equivalentArrangements != null) return equivalentArrangements;
		// Get the number of possible permuations for each site
		List<int[]> originalOrder = new ArrayList<>(NGroups());
		List<List<int[]>> siteArrangements = new ArrayList<>(NGroups());
		for (Set<Integer> group : SiteGroup) {
			int[] sites = ArrayUtils.toPrimitive(group.toArray(new Integer[0]));
			originalOrder.add(sites);
			List<int[]> arr = new ArrayList<>(DistinctPermutationGenerator.generatePermutations(sites));
			siteArrangements.add(arr);
		}
		
		// --> Stitch all possibilities together 
		List<int[]> output = new LinkedList<>();
		int[] choice = new int[siteArrangements.size()];
		while (true) {
			// Create site list corresponding to that order
			int[] newArr = new int[NSites()];
			for (int g=0; g<choice.length; g++) {
				for (int s=0; s<SiteGroup.get(g).size(); s++) {
					newArr[originalOrder.get(g)[s]] = siteArrangements.get(g).get(choice[g])[s];
				}
			}
			// Add it to list of known arrangements
			output.add(newArr);
			// Increment guess
			boolean allDone = true;
			for (int g=0; g<SiteGroup.size(); g++) {
				choice[g]++;
				if (choice[g] == siteArrangements.get(g).size()) {
					choice[g] = 0;
				} else {
					allDone = false;
					break;
				}
			}
			if (allDone) break;
		}
		
		// Save, then output
		equivalentArrangements = output;
		return output;
	}
    
    /**
     * Get number of atoms on a certain site
     * @param index Index of desired site
     * @return Number of atoms on that site
     */
    public double NOnSite(int index) {
        return NAtoms[index];
    }
    
    /**
     * @param index Index of desired site
     * @return Whether to include this site when generating attributes
     */
    public boolean siteIsIncludedInAttributes(int index) {
        return IsIncluded[index];
    }
    
    /**
     * Determine whether a group sites of should be included in attributes. This
     *  is only true if none of the individual sites are marked as omitted.
     * @param index Index of desired group
     * @return Whether to include this group when generating attributes
     */
    public boolean groupIsIncludedInAttributes(int index) {
        for (int site : SiteGroup.get(index)) {
            if (! siteIsIncludedInAttributes(site))
                return false;
        }
        return true;
    }
    
    /**
     * Define whether a group of sites should be included when generating attributes.
     * 
     * @param index Index of desired group
     * @param isIncluded Whether to include it when generating statistics
     */
    public void setGroupIncludedInAttribute(int index, boolean isIncluded) {
        for (int site : SiteGroup.get(index)) {
            IsIncluded[site] = isIncluded;
        }
    }
    
    /**
     * Generate a label for a certain group of sites.
     * @param index Index of desired group
     * @return Appropriate label (ex. AB for a group containing only sites 1 and 2)
     */
    public String getGroupLabel(int index) {
        String output = "";
        for (int site : SiteGroup.get(index)) {
            output += Character.toString((char) (65 + site));
        }
        return output;
    }
    
    /**
     * Add a new crystallographic site.
     * @param NAtoms Number of atoms on this site
     * @param isIncluded Whether this site should be included when generating attributes
	 * @param equivalentSites List of sites that are equivalent to this one (can be empty)
     */
    public void addSite(double NAtoms, boolean isIncluded, List<Integer> equivalentSites) {
		equivalentArrangements = null; // Invalidate list 
		// Add information about this particular site
        this.NAtoms = ArrayUtils.add(this.NAtoms, NAtoms);
        this.IsIncluded = ArrayUtils.add(this.IsIncluded, isIncluded);
        // This site's ID number
        int thisSite = this.NAtoms.length - 1;
        boolean alreadyAdded = false;
        for (Set<Integer> group : SiteGroup) {
            // Look whether this site is already include in another group
            if (group.contains(thisSite)) {
                alreadyAdded = true;
            }
            for (Integer site : equivalentSites) {
                if (group.contains(site)) {
                    alreadyAdded = true;
                    break;
                }
            }
            // If this site (or its equivalent sites) are in an already-defined group
            if (alreadyAdded) {
                group.add(thisSite);
                group.addAll(equivalentSites);
            }
        }
        if (!alreadyAdded) {
            Set<Integer> newGroup = new TreeSet<>();
            newGroup.addAll(equivalentSites);
            newGroup.add(thisSite);
            SiteGroup.add(newGroup);
        }
    }
}
