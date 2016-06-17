package magpie.attributes.generators.prototype;

import java.util.ArrayList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.attributes.generators.composition.ElementalPropertyAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.PrototypeEntry;
import magpie.data.materials.util.PrototypeSiteInformation;
import magpie.utility.MathUtils;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Create attributes based on the properties of elements on each group of
 * equivalent sites in a crystal. For an example, check out this paper by
 * <a href="http://www.nature.com/articles/srep19375">Pilania <i>et al.</i></a> 
 * 
 * <p>If there is only one site in a group, the mean elemental property
 * of atoms on that site is used. If there are multiple sites, then 
 * the maximum, minimum, mean, mean absolute deviation, and range are reported 
 * (following a similar strategy to {@linkplain ElementalPropertyAttributeGenerator}).
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class SingleSiteAttributeGenerator extends BaseAttributeGenerator {
    /** List of properties used to compute attributes */
    protected List<String> ElementalProperties = new ArrayList<>();
    /** List of groups with a single site */
    protected List<String> SingleSites = new ArrayList<>();
    /** List of groups with a multiple site */
    protected List<String> MultipleSites = new ArrayList<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void addAttributes(Dataset dataPtr) throws Exception {
        // Check if input datatype is correct
        if (! (dataPtr instanceof PrototypeDataset)) {
            throw new IllegalArgumentException("Data must be a PrototypeDataset");
        }
        PrototypeDataset data = (PrototypeDataset) dataPtr;
        
        // Get the site information
        PrototypeSiteInformation siteInfo = data.getSiteInfo();
        
        // Store properties from which attributes are computed
        ElementalProperties = data.getElementalProperties();
        
        // Store prototype information
        SingleSites.clear();
        MultipleSites.clear();
        for (int g=0; g<siteInfo.NGroups(); g++) {
            String siteLabel = siteInfo.getGroupLabel(g);
            if (siteInfo.getSiteGroup(g).size() == 1) {
                SingleSites.add(siteLabel);
            } else {
                MultipleSites.add(siteLabel);
            }
        }
        
        // Create names of attributes
        List<String> attrNames = generateAttributeNames(siteInfo);
        data.addAttributes(attrNames);
        
        // Compute attributes for each entry
        double[] attrs = new double[attrNames.size()];
        for (BaseEntry entryPtr : data.getEntries()) {
            PrototypeEntry entry = (PrototypeEntry) entryPtr;
            
            // Compute attributes
            generateAttributes(siteInfo, entry, data, attrs);
            
            // Add to entry
            entry.addAttributes(attrs);
        }
    }

    /**
     * Compute attributes for a single entry
     * @param siteInfo Information each site in this protoype
     * @param entry Entry being computed
     * @param data Dataset (link to property lookup tables)
     * @param attrs [out] Temporary storage for attributes (to avoid recreating N double[] arrays)
     * @throws Exception 
     */
    protected void generateAttributes(PrototypeSiteInformation siteInfo,
            PrototypeEntry entry, PrototypeDataset data, double[] attrs) throws Exception {
        // Loop through each site group
        int pos=0;
        for (int g = 0; g<siteInfo.NGroups(); g++) {
            // Check whether this site is marked as "omittted"
            if (! siteInfo.groupIsIncludedInAttributes(g)) {
                continue;
            }
            
            // Get the composition of each site in the crystal
            List<CompositionEntry> siteComps = new ArrayList<>(siteInfo.getSiteGroup(g).size());
            for (int site : siteInfo.getSiteGroup(g)) {
                siteComps.add(entry.getSiteComposition(site));
            }
            
            // Loop through each property
            for (String prop : ElementalProperties) {
                // Get a lookup table
                double[] propValues = data.getPropertyLookupTable(prop);
                
                if (siteComps.size() == 1) {
                    // Only compute the mean
                    attrs[pos++] = siteComps.get(0).getMean(propValues);
                } else {
                    // Compute the mean for each site
                    double[] siteMeans = new double[siteComps.size()];
                    for (int s=0; s<siteMeans.length; s++) {
                        siteMeans[s] = siteComps.get(s).getMean(propValues);
                    }
                    
                    // Compute several statistics
                    attrs[pos++] = StatUtils.min(siteMeans);
                    double mean = StatUtils.mean(siteMeans);
                    attrs[pos++] = mean;
                    attrs[pos++] = StatUtils.max(siteMeans);
                    attrs[pos++] = MathUtils.meanAbsoluteDeviation(siteMeans, mean);
                    attrs[pos++] = StatUtils.max(siteMeans) - StatUtils.min(siteMeans);
                }
            }
        }
    }

    /**
     * Generate names of attributes
     * @param siteInfo Information about sites in this prototype structure
     * @return List of attribute names
     */
    protected List<String> generateAttributeNames(PrototypeSiteInformation siteInfo) {
        // Generate names and store site labels
        List<String> attrNames = new ArrayList<>();
        for (int g=0; g<siteInfo.NGroups(); g++) {
            // Check whether this site is marked as "omittted"
            if (! siteInfo.groupIsIncludedInAttributes(g)) {
                continue;
            }
            
            // Determine which statistics will be computed
            String siteLabel = siteInfo.getGroupLabel(g);
            List<String> statNames = new ArrayList<>();
            if (siteInfo.getSiteGroup(g).size() == 1) {
                statNames.add("mean");
            } else {
                statNames.add("minimum");
                statNames.add("mean");
                statNames.add("maximum");
                statNames.add("variance");
                statNames.add("range");
            }
            
            // Add in labes for each property
            for (String prop : ElementalProperties) {
                for (String stat : statNames) {
                    attrNames.add(String.format("%s_%s_%s", siteLabel,
                            stat, prop));
                }
            }
        }
        return attrNames;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");

        int nAttr = (SingleSites.size() + 5 * MultipleSites.size()) * ElementalProperties.size();
        output += "(" + nAttr + ")";
        output += " Elemental properties for each group of symmetrically-equivalent sites.";
        
        // List out sides
        output += " Computes";
        if (! SingleSites.isEmpty()) {
            output += " the property of elements on the ";
            if (SingleSites.size() == 1) {
                output += SingleSites.get(0) + " site";
            } else if (SingleSites.size() == 2) {
                output += SingleSites.get(0) + " and " + SingleSites.get(1) + " sites";
            } else {
                output += SingleSites.get(0);
                for (String site : SingleSites.subList(1, SingleSites.size() - 1)) {
                    output += ", " + site;
                }
                output += " and " + SingleSites.get(SingleSites.size() - 1) + " sites";
            }
        }
        
        if (! (SingleSites.isEmpty() || MultipleSites.isEmpty())) {
            output += ", and";
        }
        
        if (! MultipleSites.isEmpty()) {
            output += " the minimum, mean, maximum, mean absolute deviation and range "
                    + "of properties on the ";
            if (MultipleSites.size() == 1) {
                output += MultipleSites.get(0) + " sites";
            } else if (MultipleSites.size() == 2) {
                output += MultipleSites.get(0) + " and " + MultipleSites.get(1) + " sites";
            } else {
                output += MultipleSites.get(0);
                for (String site : MultipleSites.subList(1, MultipleSites.size() - 1)) {
                    output += ", " + site;
                }
                output += " and " + MultipleSites.get(MultipleSites.size() - 1) + " sites";
            }
        }
        output += ". ";
        
        return output + printElementalProperties(htmlFormat);
    }

    /**
     * Print list of elemental properties used in computing attributes
     * @param htmlFormat Whether to use HTML format
     * @return List of attributes
     */
    protected String printElementalProperties(boolean htmlFormat) {
        // Print out list of properties
        String output = "Considers the following elemental properties:\n";
        if (htmlFormat) {
            output += "</br>";
        }
        
        // List properties
        boolean started = false;
        for (String prop : ElementalProperties) {
            if (started) {
                output += ", ";
            }
            output += prop;
            started = true;
        }
        
        return output;
    }
}
