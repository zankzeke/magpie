package magpie.attributes.generators.prototype;

import java.util.ArrayList;
import java.util.List;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.PrototypeEntry;
import magpie.data.materials.util.PrototypeSiteInformation;
import magpie.utility.CartesianSumGenerator;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.Combinations;

/**
 * Generate attributes based on the differences in properties between pairs of sites.
 * 
 * <p>If there is only one site in a group, the mean elemental property
 * of atoms on that site is used to describe it. If there are multiple sites in a
 * group, then the maximum, mean, and minimum of that property is used to describe the site.
 * The difference between all descriptions of each site group are used to compute
 * attributes. Both the absolute value of the difference and the magnitude of the difference
 * are generated.
 * 
 * <p>Example: For a material with stoichiometry (AA')B<sub>2</sub> there are two
 * groups of sites: (1) The group including the A and A' sites, and (2) the B site.
 * This class will generate 3 attributes. B-min(AA'), B-mean(AA'), and B-max(AA')
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 * @see PrototypeDataset
 */
public class PairSiteAttributeGenerator extends SingleSiteAttributeGenerator {
    /** Information of this particular prototype */
    protected PrototypeSiteInformation SiteInfo;

    @Override
    protected List<String> generateAttributeNames(PrototypeSiteInformation siteInfo) {
        if (siteInfo.NGroups() < 2) {
            throw new RuntimeException("Prototype must have more than 2 groups of sites");
        }
        
        // Store site information
        SiteInfo = siteInfo;
        
        List<String> attrNames = new ArrayList<>();
        for (String prop : ElementalProperties) {
            // Loop through each pair of sites
            for (int[] pair : new Combinations(siteInfo.NGroups(), 2)) {
                // Get list of stats to compute for the left and right sites
                List<String> leftProps = new ArrayList<>(3);
                if (siteInfo.getSiteGroup(pair[0]).size() == 1) {
                    leftProps.add("");
                } else {
                    leftProps.add("maximum_");
                    leftProps.add("mean_");
                    leftProps.add("minimum_");
                }

                List<String> rightProps = new ArrayList<>(3);
                if (siteInfo.getSiteGroup(pair[1]).size() == 1) {
                    rightProps.add("");
                } else {
                    rightProps.add("maximum_");
                    rightProps.add("mean_");
                    rightProps.add("minimum_");
                }

                // Generate all combinations of these properties
                for (List<String> stats : new CartesianSumGenerator<>(leftProps, rightProps)) {
                    attrNames.add(String.format("(%s%s-%s%s)_%s", 
                        stats.get(0), siteInfo.getGroupLabel(pair[0]),
                        stats.get(1), siteInfo.getGroupLabel(pair[1]), 
                        prop));
                    attrNames.add(String.format("|%s%s-%s%s|_%s", 
                        stats.get(0), siteInfo.getGroupLabel(pair[0]),
                        stats.get(1), siteInfo.getGroupLabel(pair[1]), 
                        prop));
                }
            }
        }
        return attrNames;
    }

    @Override
    protected void generateAttributes(PrototypeSiteInformation siteInfo, 
            PrototypeEntry entry, PrototypeDataset data, double[] attrs) throws Exception {
        int pos = 0;
        for (String prop : ElementalProperties) {
            // Get the lookup table
            double[] propLookup = data.getPropertyLookupTable(prop);
            
            // Get the mean property of each site group
            List<double[]> siteGroupProps = new ArrayList<>();
            for (int g=0; g<siteInfo.NGroups(); g++) {
                double[] siteProps = new double[siteInfo.getSiteGroup(g).size()];
                int gPos = 0;
                for (Integer siteID : siteInfo.getSiteGroup(g)) {
                    siteProps[gPos++] = entry.getSiteMean(siteID, propLookup);
                }
                siteGroupProps.add(siteProps);
            }
            
            // Loop through each pair of sites
            for (int[] pair : new Combinations(siteInfo.NGroups(), 2)) {
                // Get list of stats to compute for the left and right sites
                List<Double> leftProps = new ArrayList<>(3);
                double[] groupPropValues = siteGroupProps.get(pair[0]);
                if (groupPropValues.length == 1) {
                    leftProps.add(groupPropValues[0]);
                } else {
                    leftProps.add(StatUtils.max(groupPropValues));
                    leftProps.add(StatUtils.mean(groupPropValues));
                    leftProps.add(StatUtils.min(groupPropValues));
                }

                List<Double> rightProps = new ArrayList<>(3);
                groupPropValues = siteGroupProps.get(pair[1]);
                if (groupPropValues.length == 1) {
                    rightProps.add(groupPropValues[0]);
                } else {
                    rightProps.add(StatUtils.max(groupPropValues));
                    rightProps.add(StatUtils.mean(groupPropValues));
                    rightProps.add(StatUtils.min(groupPropValues));
                }

                // Generate all combinations of these properties
                for (List<Double> stats : new CartesianSumGenerator<>(leftProps, rightProps)) {
                    double diff = stats.get(1) - stats.get(0);
                    attrs[pos++] = diff;
                    attrs[pos++] = Math.abs(diff);
                }
            }
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");

        // Determine the number of attributes
        int nAttr = SingleSites.size() > 1 ? SingleSites.size() * (SingleSites.size() - 1) / 2 : 0;
        nAttr += SingleSites.size() * MultipleSites.size() * 3;
        nAttr += MultipleSites.size() > 1 ? MultipleSites.size() * (MultipleSites.size() - 1) * 9 / 2 : 0;
        nAttr *= ElementalProperties.size() * 2;
        
        // Print out short summary
        output += "(" + nAttr + ")";
        output += " Difference between elemental properties of each pair of groups of symmetrically-equivalent sites.";
        
        // List out sides
        output += " Computes the difference between the ";
        boolean started = false;
        for (int g=0; g<SiteInfo.NGroups(); g++) {
            // List separator 
            if (started) {
                if (g == SiteInfo.NGroups() - 1) {
                    output += "; and ";
                } else {
                    output += ", ";
                }
            } 
            
            started = true;
            
            // which stats
            if (SiteInfo.getSiteGroup(g).size() == 1) {
                output += SiteInfo.getGroupLabel(g) + " site";
            } else {
                output += "maximum, mean, and minimum of the "
                        + SiteInfo.getGroupLabel(g) + " sites";
            }
        }
        
        return output + ". " + printElementalProperties(htmlFormat);
    }
    
    
}
