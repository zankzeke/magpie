package magpie.utility.tools;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.utilities.generators.PhaseDiagramCompositionEntryGenerator;
import magpie.utility.interfaces.Commandable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Given a nominal composition, find nearby compositions that can be charge neutral.
 * 
 * <p>Works by finding all combinations of elements in the supplied composition
 * that are within a certain distance of the target composition with less than
 * a certain number of atoms per unit cell.
 * 
 * <p>Distance is computed as the L<sub>1</sub> distance of the composition vector
 * for N - 1 elements (N is the number of compounds). Example: Fe3Al and FeAl 
 * are 0.25 apart.
 * 
 * <p><b><u>Implemented Command</u></b>
 * 
 * <command><p><b>run &lt;composition&gt; &lt;distance&gt; &lt;size&gt;</b> -
 * Find all neutral compounds within a certain distance of a target
 * <br><pr><i>composition</i>: Target composition
 * <br><pr><i>distance</i>: Maximum allowed distance
 * <br><pr><i>size</i>: Maximum number of atoms in formula unit.</command>
 * 
 * @author Logan Ward
 */
public class IonicCompoundFinder implements Commandable {
    /** Nominal composition */
    private CompositionEntry NominalComposition;
    /** Maximum acceptable distance from nominal composition */
    private double MaximumDistance = 0.1;
    /** Maximum number of atoms in formula unit */
    private int MaxFormulaUnitSize = 5;

    /**
     * Set the target composition of the ionic compound.
     * @param comp Desired nominal composition
     * @throws java.lang.Exception
     */
    public void setNominalComposition(CompositionEntry comp) throws Exception {
        if (comp.getElements().length < 2) {
            throw new Exception("Must be at least a binary compound");
        }
        this.NominalComposition = comp;
    }

    /**
     * Set the maximum allowed distance from the target value. Note, distance
     * is computed as the L<sub>1</sub> norm of the composition vector assuming
     * one of the elements is a balance (i.e., only sum the difference for N-1 elements).
     * @param dist Maximum allowed distance
     */
    public void setMaximumDistance(double dist) {
        this.MaximumDistance = dist;
    }

    /** 
     * Set maximum number of atoms in formula unit. Example: NaCl has 2 
     * @param size Maximum allowed size
     */
    public void setMaxFormulaUnitSize(int size) {
        this.MaxFormulaUnitSize = size;
    }
    
    /**
     * Locate all ionic compounds within a certain distance of the target composition.
     * Subject to the constraints set by {@linkplain #setMaximumDistance(double) }
     * and {@linkplain #setMaxFormulaUnitSize(int) }.
     * @return List of possible compositions
     * @throws java.lang.Exception
     */
    public List<CompositionEntry> findAllCompounds() throws Exception {
        // Get elements in nominal compound
        Set<Integer> elemSet = new TreeSet<>();
        for (int i : NominalComposition.getElements()) {
            elemSet.add(i);
        }
        
        // Get list of all possible compositions
        PhaseDiagramCompositionEntryGenerator gen = new PhaseDiagramCompositionEntryGenerator();
        gen.setElementsByIndex(elemSet);
        gen.setEvenSpacing(false);
        gen.setOrder(1, elemSet.size());
        gen.setSize(MaxFormulaUnitSize);
        List<BaseEntry> allPossibilities = gen.generateEntries();
        
        // Find which ones fit the desired tolerance
        CompositionDataset dataset = new CompositionDataset();
        List<Pair<CompositionEntry, Double>> hits = new LinkedList<>();
        int[] elems = NominalComposition.getElements();
        double[] fracs = NominalComposition.getFractions();
        for (BaseEntry ptr : allPossibilities) {
            CompositionEntry cand = (CompositionEntry) ptr;
            
            // See if it is is close enough in composition
            double dist = 0.0;
            for (int e=1; e<elems.length; e++) {
                dist += Math.abs(fracs[e] - cand.getElementFraction(elems[e]));
            }
            if (dist > MaximumDistance) {
                continue;
            }
            
            // See if it is ionically neutral
            if (dataset.compositionCanFormIonic(cand)) {
                hits.add(new ImmutablePair<>(cand, dist));
            }
        }
        
        // Sort such that closest is first
        Collections.sort(hits, new Comparator<Pair<CompositionEntry, Double>> () {
            @Override
            public int compare(Pair<CompositionEntry, Double> o1, Pair<CompositionEntry, Double> o2) {
                return Double.compare(o1.getRight(), o2.getRight());
            }
        });
        
        // Get only the compositions
        List<CompositionEntry> accepted = new LinkedList<>();
        for (Pair<CompositionEntry, Double> hit : hits) {
            accepted.add(hit.getKey());
        }
        return accepted;
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("Only runs one command: find");
        }
        
        String action = Command.get(0).toString();
        if (! action.equalsIgnoreCase("run")) {
            throw new Exception("Only runs one command: find");
        }
        
        // Parse user request
        String comp;
        double dist;
        int size;
        try {
            comp = Command.get(1).toString();
            dist = Double.parseDouble(Command.get(2).toString());
            size = Integer.parseInt(Command.get(3).toString());
        } catch (Exception e) {
            throw new Exception("Usage: run <comp> <max dist> <max size>");
        }
        
        // Set finder
        setNominalComposition(new CompositionEntry(comp));
        setMaximumDistance(dist);
        setMaxFormulaUnitSize(size);
        
        // Run finder 
        List<CompositionEntry> findings = findAllCompounds();
        
        // Print results
        System.out.format("\tFound %d compositions:\n", findings.size());
        for (int f=0; f<findings.size(); f++) {
            System.out.format("%20s", findings.get(f).toString());
            if ((f % 3) == 2) {
                System.out.println();
            }
        }
        System.out.println();
        
        // Format output
        CompositionDataset output = new CompositionDataset();
        output.addEntries(findings);
        return output;
    }
}
