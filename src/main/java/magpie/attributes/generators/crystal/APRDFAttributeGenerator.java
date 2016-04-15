package magpie.attributes.generators.crystal;

import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.util.LookupData;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import vassal.analysis.APRDFAnalysis;
import vassal.data.Cell;

/**
 * Attributes based on the Atomic Property Weighted Radial Distribution Function
 * (AP-RDF) approach of <a href="http://pubs.acs.org/doi/abs/10.1021/jp404287t">
 * Fernandez <i>et al.</i></a>. 
 * 
 * <p>User can specify the cutoff distance for the AP-RDF, the number of points
 * to evaluate it, the smoothing factors for the RDF peaks, and the properties used 
 * for weighting. The recommended values of these parameters have yet to be determined, please
 * contact Logan Ward or the authors of this paper if you have questions or ideas
 * for these parameters.
 * 
 * <usage><p><b>Usage</b>: &lt;cutoff&gt; &lt;# points&gt; &lt;smoothing&gt;
 * &lt;properties&gt;
 * <br><pr><i>cutoff</i>: Cutoff distance for the AP-RDF
 * <br><pr><i># points</i>: Number of unique distances to evaluate
 * <br><pr><i>smoothing</i>: Parameter used to smooth function
 * <br><pr><i>properties</i>: Properties used to weight the RDF</usage>
 * 
 * @author Logan Ward
 */
public class APRDFAttributeGenerator extends BaseAttributeGenerator 
        implements Citable {
    /** Cutoff distance for RDF */
    protected double CutoffDistance = 10.0;
    /** Number of points to evaluate */
    protected int NumPoints = 6;
    /** Smoothing parameter for AP-RDF */
    protected double SmoothParameter = 4.0;
    /** List of elemental properties to use for weighting */
    protected Set<String> ElementalProperties = new TreeSet<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        double cutoff, smooth;
        int nPoints;
        
        // Clear old properites
        clearElementalProperties();
        
        // Parse options
        try {
            cutoff = Double.parseDouble(Options.get(0).toString());
            nPoints = Integer.parseInt(Options.get(1).toString());
            smooth = Double.parseDouble(Options.get(2).toString());
            
            for (Object obj : Options.subList(3, Options.size())) {
                addElementalProperty(obj.toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set options 
        setCutoffDistance(cutoff);
        setNumPoints(nPoints);
        setSmoothParameter(smooth);
    }

    @Override
    public String printUsage() {
        return "Usage: <cutoff> <# points> <smoothing> <properties...>";
    }

    /**
     * Define cutoff distance for AP-RDF. 
     * @param cutoff Desired cutoff
     */
    public void setCutoffDistance(double cutoff) {
        this.CutoffDistance = cutoff;
    }

    /**
     * Set number of distances to evaluated for AP-RDF
     * @param numPoints Desired number of points
     */
    public void setNumPoints(int numPoints) {
        this.NumPoints = numPoints;
    }

    /**
     * Set smoothing parameter for RDF. Controls how s
     * @param SmoothParameter 
     */
    public void setSmoothParameter(double SmoothParameter) {
        this.SmoothParameter = SmoothParameter;
    }
    
    /**
     * Clear list of elemental properties.
     */
    public void clearElementalProperties() {
        ElementalProperties.clear();
    }
    
    /**
     * Add elemental property to set of those used for generating attributes
     * @param propertyName Property name
     */
    public void addElementalProperty(String propertyName) {
        ElementalProperties.add(propertyName);
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check whether dataset is a crystal-structure dataset
        if (! (data instanceof CrystalStructureDataset)) {
            throw new RuntimeException("Data must be a CrystalStructureDataset");
        }
        CrystalStructureDataset dataPtr = (CrystalStructureDataset) data;
        
        // Create tool to compute AP-RDF
        APRDFAnalysis tool = new APRDFAnalysis();
        tool.setCutoffDistance(CutoffDistance);
        tool.setNWindows(NumPoints);
        tool.setSmoothingFactor(SmoothParameter);
        
        // Create attribute names
        List<String> names = new ArrayList<>(NumPoints * ElementalProperties.size());
        double[] evalDist = tool.getEvaluationDistances();
        for (String prop : ElementalProperties) {
            for (double dist : evalDist) {
                names.add(String.format("APRDF_%s_R=%f_B=%f", prop,
                        dist, SmoothParameter));
            }
        }
        data.addAttributes(names);
        
        // Loop through each entry, compute attributes
        double[] newAttrs = new double[names.size()];
        for (BaseEntry entry : data.getEntries()) {
            // Get the structure
            Cell strc = ((AtomicStructureEntry) entry).getStructure();
            
            // Prepare the APRDF tool
            tool.analyzeStructre(strc);
            
            // Loop through each property
            int pos = 0;
            for (String prop : ElementalProperties) {
                // Get the elemental properties for each atom type
                double[] propLookup = dataPtr.getPropertyLookupTable(prop);
                double[] atomProp = new double[strc.nTypes()];
                
                for (int t=0; t<atomProp.length; t++) {
                    atomProp[t] = propLookup[ArrayUtils.indexOf(LookupData.ElementNames, strc.getTypeName(t))];
                }
                
                // Compute the APRDF
                double[] apRDF = tool.computeAPRDF(atomProp);
                for (double ap : apRDF) {
                    newAttrs[pos++] = ap;
                }
            }
            
            // Store the attributes
            entry.addAttributes(newAttrs);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Add number of properties
        output += "(" + (NumPoints * ElementalProperties.size()) + ")";
        
        // Add description
        output += " Computes the Atomic-Property-Weighted Radial Distribution "
                + "Function (AP-RDF) for a material at " + NumPoints + " intervals ranging from ";
        output += String.format("%.2f to %.2f Angstrom ", CutoffDistance / NumPoints, CutoffDistance);
        output += String.format("with a smoothing factor of B=%.1f 1/Angstrom.", SmoothParameter);
        
        // Add properties
        output += " The AP-RDF is weighted by " + ElementalProperties.size()
                + " different elemental properties:\n";
        if (htmlFormat) {
            output += "<br>";
        }
        Iterator<String> iter = ElementalProperties.iterator();
        output += iter.next();
        while (iter.hasNext()) {
            output += ", " + iter.next();
        }
        
        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = new LinkedList<>();
        
        // Add in citation to Fernandez paper
        output.add(new ImmutablePair<>("Introduced AP-RDF for MOFs",
                new Citation(this.getClass(),
                        "Article", 
                        new String[]{"M. Fernandez", "et al."},
                        "Atomic Property Weighted Radial Distribution Functions "
                                + "Descriptors of Metal Organic Frameworks for "
                                + "the Prediction of Gas Uptake Capacity",
                        "http://pubs.acs.org/doi/abs/10.1021/jp404287t",
                        null)
        ));
        
        return output;
    }
}
