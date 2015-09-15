package magpie.attributes.generators.composition;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.Dataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.filters.CompositionDistanceFilter;

/**
 * Attributes based on the presence of nearby compounds. Current attributes:
 * 
 * <ol>
 * <li>Mean distance to the 1st, 1st and 2nd, and 1st - 3rd nearest compounds.
 * See: {@linkplain CompositionDistanceFilter}
 * <li><i>Under consideration</i>: Topological instability parameter (&lambda;<sub>min</sub>). See: 
 * <a href="http://www.tandfonline.com/doi/abs/10.1080/09500830802375622">
 * Botta <i>et al</i>. (2008)</a>
 * </ol>
 * 
 */
public class NearbyCompoundAttributeGenerator extends BaseAttributeGenerator {
    /** List of known compounds */
    private Set<CompositionEntry> Compounds;
    /** Name of property to use as atomic radii. Would be used for 
     * computing topological instability parameter
     */
    private String RadiiPropertyName = "CovalentRadius";
    /** Array of atomic radii. [0] -> H, [1] -> He, ... */
    private double[] Radii;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // Options TBD
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Compute the minimum topological instability parameter 
     * @param composition Composition of alloy in question
     * @param knownPhases Composition of known phases
     * @return Minimum instability parameter of the composition in question for
     * all known phases.
     */
    public double computeLambdaMin(CompositionEntry composition, 
            Collection<CompositionEntry> knownPhases) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * Not finished: Compute topological instability parameter for a phase at a certain composition.
     * <p>Following formula described in: 
     * <a href="http://link.aip.org/link/JAPIAU/v111/i2/p023509/s1&Agg=doi">
     * Falc&atilde;o de Oliveira (2012)</a>
     * @param alloyComposition Composition of alloy
     * @param phaseComposition Composition of phase
     * @return Topological instability parameter of phase
     */
    public double computeLambda(CompositionEntry alloyComposition, 
            CompositionEntry phaseComposition) {
        // Determine fraction of solute in phase
        double Rmin = Double.POSITIVE_INFINITY;
        int[] alloyElems = alloyComposition.getElements();
        double[] alloyFracs = alloyComposition.getFractions();
        for (int i=0; i<alloyElems.length; i++) {
            // Get molar fraction
            int elem = alloyElems[i];
            double phaseFrac = phaseComposition.getElementFraction(elem);
            
            // If phase does not contain this element, continue
            if (phaseFrac <= 0) {
                continue;
            }
            
            // Compute R for this phase
            double R = alloyFracs[i] / phaseFrac;
            
            // Update minimum
            Rmin = Math.min(R, Rmin);
        }
        
        // If this phase did not contain any of the same elements as the alloy,
        //  the Rmin will still equal infinity
        if (Double.isInfinite(Rmin)) {
            return Double.POSITIVE_INFINITY; // Return a huge instability
        }
        
        // Now, compute the solute fraction
        double[] c = alloyFracs.clone();
        for (int i=0; i<alloyFracs.length; i++) {
            c[i] -= Rmin * phaseComposition.getElementFraction(alloyElems[i]);
        }
        
        // Compute instability parameter
        double lambda = 0.0;
        int[] phaseElem = phaseComposition.getElements();
        double[] phaseFrac = phaseComposition.getFractions();
        for (int eAlloy=0; eAlloy<alloyFracs.length; eAlloy++) {
            // Compute 
            double instabContrib = 0.0;
            for (int ePhase=0; ePhase<phaseFrac.length; ePhase++) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }
        return Double.NaN;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
