package magpie.attributes.generators.composition;

import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.GCLPCalculator;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Computes attributes based on the T=0K ground state. Only works on extensions
 * of {@linkplain CompositionDataset}.
 * 
 * <p><b>Attributes</b>
 * <ul>
 * <li>Formation energy
 * <li>Number of phases in equilibrium
 * <li>Distance from closest composition (i.e., ||x<sub>i</sub> - x<sub>i,f</sub>||<sub>2</sub>
 * for each component i for phase f)
 * <li>Average distance from all neighbors
 * <li>Quasi-entropy (sum x<sub>i</sub> * ln(x<sub>i</sub>), where x<sub>i</sub> is fraction of phase)
 * </ul>
 * 
 * <usage><p><b>Usage</b>: $&lt;phases&gt; [-noCount]
 * <br><pr><i>phases</i>: Phases to consider when computing ground states
 * <br><pr><i>noCount</i>: Do not use attributes that depend on number of components.
 * Certain values of the number of phases in equilibrium and "quasi-entropy" are
 * only accessible to systems with larger number of elements. Useful
 * if you do not want to consider the number of components in an alloy as a predictive
 * variable.
 * </usage>
 * @author Logan Ward
 */
public class GCLPAttributeGenerator extends BaseAttributeGenerator {
    /** Tool used to compute ground states */
    private GCLPCalculator Calculator;
    /** Whether to include the number of phases at equilibrium */
    private boolean CountPhases = true;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            CompositionDataset data = (CompositionDataset) Options.get(0);
            setPhases(data);
            if (Options.size() > 1) {
                // Check for -nocount flag
                if (Options.get(1).toString().equalsIgnoreCase("-nocount")) {
                    setCountPhases(false);
                }
            } else if (Options.size() > 2) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<phases> [-noCount]";
    }

    /**
     * Define phases used when computing ground states
     * @param phases Phases to consider
     */
    public void setPhases(CompositionDataset phases) {
        Calculator = new GCLPCalculator();
        Calculator.addPhases(phases);
    }

    /** 
     * Set whether to count number of phases at equilibrium.
     * 
     * In some cases, you may want to exclude this as an attribute because 
     * it is tied to the number of components in the compound.
     * 
     * @param countPhases Desired setting
     */
    public void setCountPhases(boolean countPhases) {
        this.CountPhases = countPhases;
    }

    @Override
    public void addAttributes(Dataset data) {
        // Check if data is a composition dataset
        if (! (data instanceof CompositionDataset)) {
            throw new Error("Data must extend CompositionDataset");
        }
        CompositionDataset ptr = (CompositionDataset) data;
        
        // Check if GCLP calculation has been defined
        if (Calculator == null) {
            throw new Error("GCLP calculator has not been setup.");
        }
        
        // Generate attribute names
        List<String> newAttributeNames = new LinkedList<>();
        newAttributeNames.add("T0K:Enthalpy");
        if (CountPhases) {
            newAttributeNames.add("T0K:NPhasesEquilibirum");
        }
        newAttributeNames.add("T0K:ClosestPhaseDistance");
        newAttributeNames.add("T0K:MeanPhaseDistance");
        if (CountPhases) {
            newAttributeNames.add("T0K:QuasiEntropy");
        }
        
        // Compute attributes for each entry
        double[][] newAttributes = new double[newAttributeNames.size()][data.NEntries()];
        for (int e=0; e<data.NEntries(); e++) {
            CompositionEntry entry = ptr.getEntry(e);
            int a = 0;
            
            try {
                // Run GCLP
                Pair<Double,Map<CompositionEntry,Double>> result = 
                        Calculator.runGCLP(entry);
                
                // Compute formation energy
                newAttributes[a++][e] = result.getLeft();
                
                // Compute number of phases
                Map<CompositionEntry, Double> phases = result.getRight();
                if (CountPhases) {
                    newAttributes[a++][e] = phases.size();
                }
                
                // Compute distances
                double[] phaseDist = new double[phases.size()];
                int[] myElems = entry.getElements();
                double[] myFracs = entry.getFractions();
                int p=0;
                for (CompositionEntry phase : phases.keySet()) {
                    double dist=0;
                    for (int el=0; el<myElems.length; el++) {
                        double diff = phase.getElementFraction(myElems[el]) - myFracs[el];
                        dist += diff * diff;
                    }
                    phaseDist[p++] = Math.sqrt(dist);
                }
                newAttributes[a++][e] = StatUtils.min(phaseDist);
                newAttributes[a++][e] = StatUtils.mean(phaseDist);
                
                // Compute quasi-entropy
                if (CountPhases) {
                    double entropy = 0;
                    for (Double frac : phases.values()) {
                        entropy += frac * Math.log(frac);
                    }
                    newAttributes[a++][e] = entropy;
                }
                
            } catch (Exception ex) {
                throw new Error(ex);
            }
        }
        
        // Add in attributes
        for (int a=0; a<newAttributes.length; a++) {
            data.addAttribute(newAttributeNames.get(a), newAttributes[a]);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Add information about these attributes
        output += (CountPhases ? "(5)" : "(3)")
                + " Attributes based on the T=0K phase stability"
                + " computed using Grand Canonical Linear Programming: ";
        if (CountPhases) {
            output += (htmlFormat ? "&Delta;" : "d") + "H, number of phases at"
                + " equilibrium, distance from composition to closest phase in equilibirum, "
                + " mean distance from all phases in equilibirum at composition,"
                + " and quasi-entropy computed from the fractions of phases in equilibirum."; 
        } else {
            output += (htmlFormat ? "&Delta;" : "d") + "H,"
                    + " distance from composition to closest phase in equilibirum, and "
                    + " mean distance from all phases in equilibirum at composition.";
        }
        
        return output;
    }
}
