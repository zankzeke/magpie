package magpie.utility.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.optimization.algorithms.OptimizationHelper;
import magpie.utility.CartesianSumGenerator;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;

/**
 * Given a composition, guess the most-likely oxidation states for each element.
 * 
 * <p>Works by finding all combinations non-zero oxidation states for each element,
 * computing which are the most reasonable, and finding which of those have the 
 * minimum value of
 * 
 * <center>sum<sub>i,j</sub>[ (&Chi;<sub>i</sub> - &Chi;<sub>j</sub>) 
 * (c<sub>i</sub> - c<sub>j</sub>) ] for i &lt; j</center>
 * 
 * where &Chi;<sub>i</sub> is the electronegativity and c<sub>i</sub> is the oxidation. This 
 * biases the selection towards the more electronegative elements being more negatively
 * charged.
 * 
 * <p><b><u>Implemented Command</u></b>
 * 
 * <command><p><b>run &lt;composition&gt;</b> -
 * Find all ionic compounds possible at this composition 
 * <br><pr><i>composition</i>: Composition
 * <br>Prints a list of charges to the screen where the first is the most favored
 * based on the electronegative elements having the greatest charge.
 * </command>
 * 
 * <usage><p><b>Usage</b>: [&lt;oxidation states&gt;] [&lt;en&gt;]
 * <br><pr><i>lookup table</i>: Path oxidation states lookup table
 * <br><pr><i>en</i>: Path to electronegativity lookup table
 * <br>Lookup tables must files where each line is contains either the allowed 
 * oxidation states or electronegativity for each element. Lines must be ordered 
 * by atomic number (H, He, ...). By default, looks in "./Lookup Data/".
 * </usage>
 * 
 * @author Logan Ward
 */
public class OxidationStateGuesser implements Commandable, Options,
        java.io.Serializable {
    /** Allowed oxidation states for each element */
    private int[][] OxidationStates;
    /** Electronegativity for each element */
    private double[] Electronegativity;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String oxFile = "./Lookup Data/OxidationStates.table";
        String enFile = "./Lookup Data/Electronegativity.table";
        if (Options.size() > 0) {
            oxFile = Options.get(0).toString();
        }
        if (Options.size() > 1) {
            enFile = Options.get(1).toString();
        }
        if (Options.size() > 2) {
            throw new Exception(printUsage());
        }
        setOxidationStates(oxFile);
        setElectronegativity(enFile);
    }

    @Override
    public String printUsage() {
        return "Usage: <oxidation state file> <electronegativity file>";
    }
    
    /**
     * Set the lookup table for electronegativity values.
     * @param values Electronegativity for each element (ordered by Z)
     */
    public void setElectronegativity(double[] values) {
        this.Electronegativity = values;
    }
    
    /**
     * Set the lookup table for electronegativity values.
     * @param path Path to file containing electronegativity for each element, on lines
     * ordered by Z.
     * @throws java.io.IOException
     */
    public void setElectronegativity(String path) throws IOException {
        List<Double> temp = new ArrayList<>(100);
        BufferedReader input = new BufferedReader(new FileReader(path));
        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            double x;
            try {
                x = Double.parseDouble(line);
            } catch (Exception e) {
                x = Double.NaN;
            }
            temp.add(x);
        }
        Electronegativity = new double[temp.size()];
        for (int i=0; i<temp.size(); i++) {
            Electronegativity[i] = temp.get(i);
        }
    }

    /**
     * Set the oxidation states
     * @param states Allowed oxidation states. Each row contains the allowed oxidation
     * states for each element (ordered by Z).
     */
    public void setOxidationStates(int[][] states) {
        this.OxidationStates = states.clone();
    }
    
    /**
     * Set the oxidation states
     * @param states Allowed oxidation states. Each row contains the allowed oxidation
     * states for each element (ordered by Z).
     */
    public void setOxidationStates(double[][] states) {
        OxidationStates = new int[states.length][];
        for (int i=0; i<states.length; i++) {
            if (states[i] == null) continue;
            OxidationStates[i] = new int[states[i].length];
            for (int j=0; j<states[i].length; j++) {
                OxidationStates[i][j] = (int) states[i][j];
            }
        }
    }

    /**
     * Set the oxidation states
     * @param path Path to lookup file. Each line must contain the allowed oxidation
     * states for each element (ordered by Z).
     * @throws java.lang.Exception
     */
    public void setOxidationStates(String path) throws Exception {
        BufferedReader input = new BufferedReader(new FileReader(path));
        List<int[]> states = new ArrayList<>(100);
        while (true) {
            String line = input.readLine();
            if (line == null) {
                break;
            }
            String[] words = line.split("[ ,\t]");
            if (words.length == 1 && words[0].length() == 0) {
                states.add(new int[0]);
                continue;
            }
            int[] temp = new int[words.length];
            for (int i=0; i<words.length; i++) {
                temp[i] = Integer.parseInt(words[i]);
            }
            states.add(temp);
        }
        OxidationStates = states.toArray(new int[0][]);
    }
    
    /**
     * Get list of possible charge states.
     * @param comp Composition of allegedly ionic compound
     * @return List of possible oxidation states, same order as 
     * {@linkplain CompositionEntry#getElements() }. Listed such that most 
     * likely is first.
     * @see OxidationStateGuesser
     */
    public List<int[]> getPossibleStates(CompositionEntry comp) {
        // Get oxidation states of each element
        int[] elems = comp.getElements();
        if (elems.length == 1) {
            return new LinkedList<>();
        }
        List<Collection<Integer>> states = new ArrayList<>(elems.length);
        for (int e=0; e<elems.length; e++) {
            if (OxidationStates[elems[e]] == null) {
                return new LinkedList<>();
            }
            List<Integer> temp = new ArrayList<>(OxidationStates[elems[e]].length);
            for (int s : OxidationStates[elems[e]]) {
                temp.add(s);
            }
            if (temp.isEmpty()) {
                return new LinkedList<>();
            }
            states.add(temp);
        }
        
        // Generate all combinations of those charge states, only store the ones 
        //  that are charge balanced
        double[] fracs = comp.getFractions();
        List<int[]> possibleStates = new LinkedList<>();
        for (List<Integer> state : new CartesianSumGenerator<>(states)) {
            double charge = 0;
            for (int i=0; i<fracs.length; i++) {
                charge += fracs[i] * state.get(i);
            }
            if (Math.abs(charge) < 1e-6) {
                int[] temp = new int[state.size()];
                for (int i=0; i<temp.length; i++) {
                    temp[i] = state.get(i);
                }
                possibleStates.add(temp);
            }
        }
        
        // Check if there are less than 2
        if (possibleStates.size() < 2) {
            return possibleStates;
        }
        
        // Order them by electronegativity rank
        double[] rankVal = new double[possibleStates.size()];
        for (int s=0; s<possibleStates.size(); s++) {
            rankVal[s] = 0.0;
            int[] thisStates = possibleStates.get(s);
            for (int i=0; i<possibleStates.get(s).length; i++) {
                for (int j=i+1; j<possibleStates.get(s).length; j++) {
                    rankVal[s] += (Electronegativity[elems[i]]
                            - Electronegativity[elems[j]]) 
                            * (thisStates[i] - thisStates[j]);
                }
            }
        }
        int[] ranks = OptimizationHelper.sortAndGetRanks(rankVal, false);
        List<int[]> output = new ArrayList<>(ranks.length);
        for (int rank : ranks) {
            output.add(possibleStates.get(rank));
        }
        return output;
    }


    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("Only runs one command: run");
        }
        
        String action = Command.get(0).toString();
        if (! action.equalsIgnoreCase("run")) {
            throw new Exception("Only runs one command: run");
        }
        
        // Parse user request
        String compString = Command.get(1).toString();
        CompositionEntry comp = new CompositionEntry(compString);
        
        // Run the search
        List<int[]> posStates = getPossibleStates(comp);
        
        // Print results
        if (posStates.isEmpty()) {
            System.out.println("No feasible oxidation states.");
        } else if (posStates.size() == 1) {
            System.out.println("Only possible state: " 
                    + printStates(comp.getElements(), posStates.get(0)));
        } else {
            System.out.println("Best state:   " 
                    + printStates(comp.getElements(), posStates.get(0)));
            System.out.println("Other states: "
                    + printStates(comp.getElements(), posStates.get(1)));
            for (int i=2; i<posStates.size(); i++) {
                System.out.println("              "
                    + printStates(comp.getElements(), posStates.get(i)));
            }
        }
        return null;
    }
    
    /**
     * Print out the elements and charges in a nice way.
     * @param elems Element indices (Z-1)
     * @param charges Oxidation state of each element
     * @return Human-readable string of oxidation state guesses
     */
    protected String printStates(int[] elems, int[] charges) {
        String output = "";
        for (int i=0; i<elems.length; i++) {
            output += String.format("%s (%s%d)  ", LookupData.ElementNames[elems[i]],
                    charges[i] > 0 ? "+" : "",
                    charges[i]);
        }
        return output;
    }
}
