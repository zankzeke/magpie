package magpie.data.materials.util;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;

/**
 * Grand-Canonical Linear Programming (GCLP) method to compute phase equilibria.
 * 
 * <p>How to use this class:
 * 
 * <ol>
 * <li>Load in phase equilibria data with {@linkplain #addPhase(magpie.data.materials.CompositionEntry, double) }, 
 * {@linkplain #setMu(java.lang.String, double) }, or {@linkplain #addPhases(magpie.data.materials.CompositionDataset) }.
 * <li>Compute equilibrium with {@link #runGCLP(magpie.data.materials.CompositionEntry)}
 * <li>Access ground state energy with {@linkplain #getGroundStateEnergy() }
 * and equilibrium with {@linkplain #getPhaseEquilibria() }
 * </ol>
 * 
 * <p>Reference: <a href="http://onlinelibrary.wiley.com/doi/10.1002/adma.200700843">
 * Akbarzadeh, Ozolins, Wolverton. <u>Advanced Materials</u>.
 * 19 (2007), 3233.</a>
 * @author Logan Ward
 */
public class GCLPCalculator implements java.io.Serializable {
    /** 
     * Phases to consider for equilibria and their energy. Only contains
     * the lowest-energy phase at each entry
     */
    protected Map<CompositionEntry, Double> Phases = new TreeMap<>();
    /**
     * Composition for which equilibrium is currently computed.
     */
    protected CompositionEntry CurrentComposition = null;

    /**
     * Initialize a GCLP calculator. Sets the chemical potential of each
     * element to be 0.
     */
    public GCLPCalculator() {
        // Add for each element
        int[] elem = new int[1];
        double[] frac = new double[1];
        frac[0] = 1.0;
        for (int i=0; i<LookupData.ElementNames.length; i++) {
            elem[0] = i;
            CompositionEntry entry = new CompositionEntry(elem, frac);
            Phases.put(entry, 0.0);
        }
    }
    
    /**
     * Define the chemical potential of an element
     * @param elem Abbreviation of element
     * @param mu Desired chemical potential
     * @throws Exception 
     */
    public void setMu(String elem, double mu) throws Exception {
        CompositionEntry entry = new CompositionEntry(elem);
        if (entry.getElements().length != 1) {
            throw new Exception("Not an element: " + elem);
        }
        Phases.put(entry, mu);
    }
    
    /**
     * Set many phase energies. Assumes that measured class values are the
     * energy of interest.
     * @param phases Dataset containing energy values
     */
    public void addPhases(CompositionDataset phases) {
        for (BaseEntry ptr : phases.getEntries()) {
            CompositionEntry entry = (CompositionEntry) ptr;
            if (ptr.hasMeasurement()) {
                addPhase(entry, entry.getMeasuredClass());
            }
        }
    }
    
    /**
     * Add new phase to the list of phases to consider.
     * @param composition Composition of phase
     * @param energy Energy of phase
     */
    public void addPhase(CompositionEntry composition, double energy) {
        Double curEnergy = Phases.get(composition);
        if (curEnergy == null) {
            // Add if there is no current entry at this composition
            Phases.put(composition, energy);
        } else if (curEnergy > energy) {
            // If there is a phase, update only if new energy is lower than current
            Phases.put(composition, energy);
        }
    }
 
    /**
     * Get the number of phases being considered for GCLP
     * @return Number of phases
     */
    public int numPhases() {
        return Phases.size();
    }
    
    /**
     * Compute the ground state phase equilibria for a certain composition.
     * @param composition Composition to be considered
     * @return Map describing ground state equilibrium: 
     * (GCLP energy, {'phase composition' -> fraction}
     * @throws java.lang.Exception
     */
    public Pair<Double,Map<CompositionEntry,Double>> 
            runGCLP(CompositionEntry composition) throws Exception {
        // Set the composition and pull up lookup data
        CurrentComposition = composition.clone();
        int[] curElems = CurrentComposition.getElements();
        double[] curFrac = CurrentComposition.getFractions();
        
        // Get the current possible phases (i.e., those that contain exclusively 
        //  the elements in the current compound).
        List<CompositionEntry> components = new LinkedList<>();
        List<Double> energies = new LinkedList<>();
        for (Map.Entry<CompositionEntry, Double> phase : Phases.entrySet()) {
            CompositionEntry component = phase.getKey();
            Double energy = phase.getValue();

            // Check whether this entry is in the target phase diagram
            int[] thisElems = component.getElements();
            boolean isIn = true;
            for (int thisElem : thisElems) {
                if (! ArrayUtils.contains(curElems, thisElem)) {
                    isIn = false;
                    break;
                }
            }
            
            // If it is, add it to the list
            if (isIn) {
                components.add(component);
                energies.add(energy);
            }
        }
        
        // Set up constraints 
        // Type #1 : Mass Conservation
        List<LinearConstraint> constraints = new ArrayList<>(curElems.length);
        for (int e=0; e<curElems.length; e++) {
            double[] coeff = new double[components.size()];
            for (int i=0; i<components.size(); i++) {
                coeff[i] = components.get(i).getElementFraction(curElems[e]);
            }
            constraints.add(new LinearConstraint(coeff, Relationship.EQ, curFrac[e]));
        }
        
        // Type #2 : Normalization
        double[] coeff = new double[components.size()];
        Arrays.fill(coeff, 1.0);
        constraints.add(new LinearConstraint(coeff, Relationship.EQ, 1.0));
        
        // Make the constratint set object
        LinearConstraintSet constraintSet = new LinearConstraintSet(constraints);
        
        // Set up objective function
        double[] energyTerms = new double[energies.size()];
        for (int i=0; i<energyTerms.length; i++) {
            energyTerms[i] = energies.get(i);
        }
        LinearObjectiveFunction objFun = new LinearObjectiveFunction(energyTerms, 0);
        
        // Call LP solver
        SimplexSolver solver = new SimplexSolver();
        PointValuePair result = solver.optimize(objFun,
                constraintSet, 
                new NonNegativeConstraint(true));
        
        // Store result
        Map<CompositionEntry, Double> Equilibrium = new TreeMap<>();
        double[] equilFracs = result.getPoint();
        for (int i=0; i<components.size(); i++) {
            if (equilFracs[i] > 1e-6) {
                Equilibrium.put(components.get(i), equilFracs[i]);
            }
        }
        double GroundStateEnergy = result.getValue();
        
        return new ImmutablePair<>(GroundStateEnergy, Equilibrium);
    }
}
