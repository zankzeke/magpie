package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.materials.util.LookupData;
import magpie.models.regression.crystal.PRDFRegression;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Compute attributes based on the Pair Radial Distribution Function (PRDF). Based
 * on work by <a href="http://link.aps.org/doi/10.1103/PhysRevB.89.205118">Schutt
 * <i>et al.</i></a>.
 * 
 * <usage><p><b>Usage</b>: &lt;cutoff distance&gt; &lt;n steps&gt; $&lt;dataset&gt;
 * <pr><br><i>cutoff distance</i>: Maximum distance to consider
 * <pr><br><i>n steps</i>: Number of distance points along axis to sample
 * <pr><br><i>dataset</i>: {@linkplain CompositionDataset} containing list
 of ElementList to be considered in PRDF. Usually, this is the dataset
 you will be computing attributes for
 </usage>
 * 
 * @author Logan Ward
 */
public class PRDFAttributeGenerator extends BaseAttributeGenerator implements Citable {
    /** Cutoff distance for PRDF */
    protected double CutoffDistance = 10;
    /** Number of distance points to evaluate */
    protected int NPoints = 20;
    /** List of ElementList to use in PRDF */
    final protected SortedSet<Integer> ElementList = new TreeSet<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        double cutoffDistance;
        int nPoints;
        CompositionDataset elemData;
        
        try {
            cutoffDistance = Double.parseDouble(Options.get(0).toString());
            nPoints = Integer.parseInt(Options.get(1).toString());
            elemData = (CompositionDataset) Options.get(2);
            if (Options.size() > 3) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        setCutoffDistance(cutoffDistance);
        setNPoints(nPoints);
        setElements(elemData);
    }

    @Override
    public String printUsage() {
        return "Usage: <cutoff distance> <# points> $<dataset>";
    }

    /**
     * Define the maximum distance to consider when computing the PRDF
     * @param cutoffDistance Desired cutoff distance 
     */
    public void setCutoffDistance(double cutoffDistance) {
        this.CutoffDistance = cutoffDistance;
    }

    /**
     * Define the number of points on each PRDF to store
     * @param NPoints Number of evaluation points
     */
    public void setNPoints(int NPoints) {
        this.NPoints = NPoints;
    }
    
    /**
     * Clear list of elements used when computing PRDF
     */
    public void clearElementList() {
        ElementList.clear();
    }
    
    /**
     * Set the elements when computing PRDF
     * @param data Dataset containing each element to be  
     */
    public void setElements(CompositionDataset data) {
        clearElementList();
        
        for (BaseEntry entry : data.getEntries()) {
            for (int elem : ((CompositionEntry) entry).getElements()) {
                addElement(elem);
            }
        }
    }
    
    /**
     * Add element to list used when computing PRDF
     * @param elem ID of element (Atomic number - 1)
     */
    public void addElement(int elem) {
        ElementList.add(elem);
    }
    
    /**
     * Add element to list used when computing PRDF
     * @param elem Abbreviation of element (e.g., "Al")
     */
    public void addElement(String elem) {
        int id = ArrayUtils.indexOf(LookupData.ElementNames, elem);
        if (id == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalArgumentException("No such element: " + elem);
        }
        addElement(id);
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check if input has crystals structure data
        if (! (data instanceof CrystalStructureDataset)) {
            throw new IllegalArgumentException("Input data must be a CrystalStructureDataset");
        }
        
        // Get names of elemnets in set
        List<String> elemNames = new ArrayList<>();
        List<Integer> elemID = new ArrayList<>(ElementList);
        for (Integer id : elemID) {
            elemNames.add(LookupData.ElementNames[id]);
        }
        
        // Get the step size for the PRDF
        double stepSize = CutoffDistance / NPoints;
        
        // Make variable names
        List<String> names = new ArrayList<>();
        for (int elemA=0; elemA<elemNames.size(); elemA++) {
            for (int elemB=0; elemB<elemNames.size(); elemB++) {
                for (int step=1; step<=NPoints; step++) {
                    names.add(String.format("%s_%s_R=%.3f", elemNames.get(elemA),
                            elemNames.get(elemB), stepSize * step));
                }
            }
        }
        data.addAttributes(names);
        
        // Initailize PRDF computer
        PRDFRegression computer = new PRDFRegression();
        computer.setCutoff(CutoffDistance);
        computer.setNBins(NPoints);
        
        
        // Compute attributes for each entry
        double[] newAttrs = new double[names.size()];
        for (BaseEntry entry : data.getEntries()) {
            // Compute the PRDF 
            Map<Pair<Integer,Integer>,double[]> prdf = (Map<Pair<Integer,Integer>,double[]>)
                    computer.computeRepresentation(((CrystalStructureEntry) entry).getStructure());
            
            // Store the attributes
            Arrays.fill(newAttrs, 0);
            for (Pair<Integer,Integer> pair : prdf.keySet()) {
                // Determine position in output
                int elemA = elemID.indexOf(pair.getLeft());
                int elemB = elemID.indexOf(pair.getRight());
                
                int prdfPos = (elemA * ElementList.size() + elemB) * NPoints;
                
                // Store that in the output
                System.arraycopy(prdf.get(pair), 0, newAttrs, prdfPos, NPoints);
            }
            
            // Add to entry
            entry.addAttributes(newAttrs);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(" + (ElementList.size() * ElementList.size() * NPoints) + ")";
        output += " Pair radial distribution function calculated with a cutoff distance of ";
        output += String.format("%.3f at %d steps (units dependant on input data).", 
                CutoffDistance, NPoints);
        output += " Evaluated all pairs of " + ElementList.size() + "different elements:\n";
        
        // Print out elements
        if (htmlFormat) {
            output += "<br>";
        }
        boolean started = false;
        for (Integer elem : ElementList) {
            if (started) {
                output += ", ";
            }
            output += LookupData.ElementNames[elem];
            started = true;
        }

        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String,Citation>> output = new ArrayList<>(1);
        
        output.add(new ImmutablePair<>("Introduced use of PRDF in machine learning models",
                new Citation(this.getClass(), "Article", 
                        new String[]{"K.T. Schutt", "H. Glawe", "et al."},
                        "How to represent crystal structures for machine learning: Towards fast prediction of electronic properties",
                        "http://link.aps.org/doi/10.1103/PhysRevB.89.205118",
                        null)));
        
        return output;
    }
}

