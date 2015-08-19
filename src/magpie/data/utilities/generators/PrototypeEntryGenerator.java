package magpie.data.utilities.generators;

import magpie.data.materials.PrototypeEntry;
import magpie.data.materials.util.PrototypeSiteInformation;
import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.*;

/**
 * Generate {@linkplain PrototypeEntry} with certain elements (or combination 
 * of elements on each site). 
 * 
 * <usage><p><b>Usage</b>: &lt;site info&gt; -site &lt;order&gt; &lt;spacing&gt;
 * &lt;elements...&gt; [-site &lt;...&gt;]
 * <br><pr><i>site info</i>: Path to file describing site information
 * <br><pr><i>order</i>: Maximum number of elements mixing on A site (&ge; 1)
 * <br><pr><i>spacing</i>: Spacing (in at%) between mixed composition on each site.
 * Example, 50 will generate A, AB, and B.
 * <br><pr><i>elements</i>: List of elements, by abbreviation that could exist 
 * on each site.
 * <br>Note: You must define the "-site" information for each site in the prototype
 * in the order they are listed in the "site info" file.</usage>
 * 
 * @author Logan Ward 
 * @see PrototypeDataset
 */
public class PrototypeEntryGenerator extends BaseEntryGenerator {
    /**
     * Description of sites on prototype
     */
    protected PrototypeSiteInformation SiteInfo;
    /**
     * Lists of possible elements on each site
     */
    final protected List<Set<Integer>> SiteElements = new LinkedList<>();
    /**
     * Allowed mixing on each site. Listed as maximum number of elements, and
     * the desired spacing between prototypes.
     */
    final protected List<Pair<Integer, Double>> AllowedMixing = new LinkedList<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() < 2 && 
                ! Options.get(1).toString().equalsIgnoreCase("-site")) {
            throw new Exception(printUsage());
        }
        
        // Read in the site info file
        PrototypeSiteInformation siteInfo = PrototypeSiteInformation.
                readFromFile(Options.get(0).toString());
        setSiteInfo(siteInfo);
        
        // Get the allowed elements on each site
        int pos = 1;
        int siteIndex = -1;
        while (pos < Options.size()) {
            siteIndex++;
            pos++;
            int order;
            double spacing;
            try {
                order = Integer.parseInt(Options.get(pos++).toString());
                spacing = Double.parseDouble(Options.get(pos++).toString());
            } catch (Exception e) {
                throw new Exception(printUsage());
            }
            Set<String> elems = new TreeSet<>();
            while (pos < Options.size() && 
                   ! Options.get(pos).toString().equalsIgnoreCase("-site")) {
                elems.add(Options.get(pos++).toString());
            }
            defineSitePossibilities(siteIndex, order, spacing, elems);
        }
        
        // Check whether everything is ready
        if (! isSet()) {
            throw new Exception("Missing some site information");
        }
    }

    /**
     * Define the information about the sites in the prototype. Resets the 
     * information about what elements can be placed on each site.
     * @param info Definition for the prototype 
     */
    public void setSiteInfo(PrototypeSiteInformation info) {
        this.SiteInfo = info;
        SiteElements.clear();
        for (int i=0; i<info.NSites(); i++) {
            SiteElements.add(new TreeSet<Integer>());
            AllowedMixing.add(new MutablePair<Integer,Double>());
        }
    }
    
    @Override
    public String printUsage() {
        return "Usage: <site info> -site <order> <spacing> <elements...> [-site <...>]";
    }
    
    /**
     * Define the possible compositions for each site.
     * @param index Index of site
     * @param order Maximum number of elements on sites
     * @param spacing Spacing between elements in at%
     * @param possibSiteElements Abbreviations of possible elements for each site
     * @throws java.lang.Exception
     */
    public void defineSitePossibilities(int index, int order, 
            double spacing, Collection<String> possibSiteElements) 
            throws Exception {
        if (SiteInfo == null) {
            throw new Exception("Prototype has not been defined");
        }
        if (index > SiteInfo.NSites()) {
            throw new Exception("Only " + SiteInfo.NSites() + " in prototype");
        }
        
        // Set information
        AllowedMixing.set(index, new ImmutablePair<>(order,spacing));
        
        // Get indicies of each element
        Set<Integer> elems = new TreeSet<>();
        for (String elem : possibSiteElements) {
            int id = ArrayUtils.indexOf(LookupData.ElementNames, elem);
            if (id == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Exception("No such element: " + elem);
            }
            elems.add(id);
        }
        SiteElements.set(index, elems);
    }
    
    /**
     * Check whether this generator is ready to go.
     * @return Whether it is ready
     */
    public boolean isSet() {
        if (SiteInfo == null) {
            return false;
        }
        for (Set<Integer> x : SiteElements) {
            if (x.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<BaseEntry> iterator() {
        // Generate all possible combinations
        List<List<CompositionEntry>> siteCombinations = 
                generateCombinations(SiteElements, AllowedMixing);
        
        // Turn them into PrototypeEntries
        List<BaseEntry> output = new ArrayList<>(siteCombinations.size());
        for (List<CompositionEntry> comps : siteCombinations) {
            PrototypeEntry entry = new PrototypeEntry(SiteInfo);
            for (int i=0; i<comps.size(); i++) {
                entry.setSiteComposition(i, comps.get(i));
            }
            output.add(entry);
        }
        return output.iterator();
    }
    
    /**
     * Generate all possible combinations of compositions generated using 
     * a list of settings. The output will be a collection including all 
     * possible combinations of all possible compositions on each site.
     * 
     * @param possibleElements Possible elements on each site
     * @param generationSettings Order of mixing to have on each site, and 
     * desired spacing
     * @return List of all possible combinations of sites
     */
    static public List<List<CompositionEntry>> generateCombinations(
            List<Set<Integer>> possibleElements, 
            List<Pair<Integer,Double>> generationSettings) {
        
        // Create generator for the first memeber in list
        List<CompositionEntry> newEntries = new LinkedList<>();
        PhaseDiagramCompositionEntryGenerator gen = new PhaseDiagramCompositionEntryGenerator();
        gen.setElementsByIndex(possibleElements.get(0));
        gen.setEvenSpacing(true);
        try {
            gen.setOrder(1, generationSettings.get(0).getLeft());
            gen.setSize((int) Math.round(100.0 / generationSettings.get(0).getRight()) + 1);
        } catch (Exception e) {
            throw new Error(e);
        }

        // Generate new entries
        for (BaseEntry e : gen.generateEntries()) {
            newEntries.add((CompositionEntry) e);
        }
        
        List<List<CompositionEntry>> output = new ArrayList<>(newEntries.size());
        if (possibleElements.size() == 1) {
            // Trivial case
            for (CompositionEntry e : newEntries) {
                List<CompositionEntry> t = new LinkedList<>();
                t.add(e);
                output.add(t);
            }
        } else {
            // Generate lower-order lists
            List<List<CompositionEntry>> lowerComb = generateCombinations(
                    possibleElements.subList(1, possibleElements.size()),
                    generationSettings.subList(1, generationSettings.size()));
            for (List<CompositionEntry> prev : lowerComb) {
                for (CompositionEntry e : newEntries) {
                    List<CompositionEntry> toAdd = new LinkedList<>(prev);
                    toAdd.add(0, e);
                    output.add(toAdd);
                }
            }
        }
        return output;
    }
}
