package magpie.attributes.generators;

import java.util.*;
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
 * <usage><p><b>Usage</b>: $&lt;phases&gt;
 * <br><pr><i>phases</i>: Phases to consider when computing ground states</usage>
 * @author Logan Ward
 */
public class GCLPAttributeGenerator extends BaseAttributeGenerator {
    /** Tool used to compute ground states */
    private GCLPCalculator Calculator;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            CompositionDataset data = (CompositionDataset) Options.get(0);
            setPhases(data);
            if (Options.size() != 1) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<phases>";
    }

    /**
     * Define phases used when computing ground states
     * @param phases Phases to consider
     */
    public void setPhases(CompositionDataset phases) {
        Calculator = new GCLPCalculator();
        Calculator.addPhases(phases);
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
        newAttributeNames.add("T0K:NPhasesEquilibirum");
        newAttributeNames.add("T0K:ClosestPhaseDistance");
        newAttributeNames.add("T0K:MeanPhaseDistance");
        newAttributeNames.add("T0K:QuasiEntropy");
        
        // Compute attributes for each entry
        double[][] newAttributes = new double[newAttributeNames.size()][data.NEntries()];
        for (int e=0; e<data.NEntries(); e++) {
            CompositionEntry entry = ptr.getEntry(e);
            int a = 0;
            
            try {
                // Run GCLP
                Pair<Double,Map<CompositionEntry,Double>> result = 
                        Calculator.runGCLP(entry);
                
                // Compute simple
                newAttributes[a++][e] = result.getLeft();
                Map<CompositionEntry, Double> phases = result.getRight();
                newAttributes[a++][e] = phases.size();
                
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
                double entropy = 0;
                for (Double frac : phases.values()) {
                    entropy += frac * Math.log(frac);
                }
                newAttributes[a++][e] = entropy;
                
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
        output += "(5) Attributes based on the T=0K phase stability"
                + " computed using Grand Canonical Linear Programming: "
                + (htmlFormat ? "&Delta;" : "d") + "H, number of phases at"
                + " equilibrium, distance from composition to closest phase in equilibirum, "
                + " mean distance from all phases in equilibirum at composition,"
                + " and quasi-entropy computed from the fractions of phases in equilibirum.";
        
        return output;
    }
}
