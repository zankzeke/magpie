package magpie.attributes.generators.composition;

import java.io.IOException;
import java.util.*;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import magpie.utility.tools.OxidationStateGuesser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Attributes derived from the oxidation states of elements in a material.
 * Based on work by Deml <i> et al.</i>:
 * 
 * <p><center><a href="http://journals.aps.org/prb/abstract/10.1103/PhysRevB.93.085142">
 * Deml <i>et al.</i>. PRB. 93 (2016), 085142</a>
 * 
 * <p>These attributes are based on the formal charges of materials determined
 * using the {@linkplain OxidationStateGuesser}. Currently implemented attributes:
 * <ol>
 * <li>Statistics of formal charges (min, max, range, mean, variance)
 * <li>Cumulative ionization energies / electron affinities
 * <li>Difference in electronegativity between cation and anion.
 * </ol>
 * 
 * <p>For materials that the algorithm fails to find an charge states, 
 * NaN is set for all attributes.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 * @see http://journals.aps.org/prb/abstract/10.1103/PhysRevB.93.085142
 */
public class ChargeDependentAttributeGenerator extends BaseAttributeGenerator
        implements Citable {
    /** Tool used to compute electronegativity */
    protected OxidationStateGuesser ChargeGuesser = null;
    /** Ionization energies of each element */
    protected double[][] IonizationEnergies;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "No options";
    }

    @Override
    public void addAttributes(Dataset data) {
        if (! (data instanceof CompositionDataset)) {
            throw new Error("Data must implement composition dataset");
        }
        
        // Create attribute names, add to dataset
        List<String> newNames = new LinkedList<>();
        newNames.add("min_Charge");
        newNames.add("max_Charge");
        newNames.add("maxdiff_Charge");
        newNames.add("mean_Charge");
        newNames.add("var_Charge");
        newNames.add("CumulativeIonizationEnergy");
        newNames.add("CumulativeElectronAffinity");
        newNames.add("AnionCationElectronegativtyDiff");
        data.addAttributes(newNames);
        
        // Load in electronegativity / affinity data
        CompositionDataset compDataset = (CompositionDataset) data;
        double[] electronegativity, affinity;
        try {
            electronegativity = compDataset.getPropertyLookupTable("Electronegativity");
            affinity = compDataset.getPropertyLookupTable("ElectronAffinity");
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Instatntiate the oxidation state guesser
        if (ChargeGuesser == null) {
            ChargeGuesser = new OxidationStateGuesser();
            try {
                ChargeGuesser.setElectronegativity(electronegativity);
                ChargeGuesser.setOxidationStates(compDataset.getOxidationStates());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        // Read in ionization energies
        if (IonizationEnergies == null) {
            try {
                LookupData.readIonizationEnergies(compDataset.getDataDirectory() + "/IonizationEnergies.table");
                IonizationEnergies = LookupData.IonizationEnergies;
            } catch (IOException i) {
                throw new RuntimeException(i);
            }
        }
        
        // Create storage for missing ionization energies
        Map<Integer, Set<Integer>> missingIonizationData = new HashMap<>();
        
        // Compute the attributes for each entry
        double[] attrs = new double[newNames.size()];
        for (BaseEntry ptr : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) ptr;
            double[] fracs = entry.getFractions();
            int[] elems = entry.getElements();
            
            // Compute charge states
            List<int[]> possibleStates = ChargeGuesser.getPossibleStates(entry);
            
            // If no possible states, set attributes to NaN
            if (possibleStates.isEmpty()) {
                Arrays.fill(attrs, Double.NaN);
                ptr.addAttributes(attrs);
                continue;
            }
            
            // Check that we have data for all ionization energies
            int[] chargesInt = possibleStates.get(0); 
            boolean anyMissing = false;
            for (int i=0; i<elems.length; i++) {
                if (IonizationEnergies[elems[i]].length < chargesInt[i]) {
                    if (missingIonizationData.containsKey(elems[i])) {
                        missingIonizationData.get(elems[i]).add(chargesInt[i]);
                    } else {
                        Set<Integer> temp = new TreeSet<>();
                        temp.add(chargesInt[i]);
                        missingIonizationData.put(elems[i], temp);
                    }
                    anyMissing = true;
                    break;
                }
            }
            
            // Convert charge states to double array
            double[] charges = new double[chargesInt.length];
            for (int i=0; i<charges.length; i++) {
                charges[i] = (double) chargesInt[i];
            }
            
            // Compute attributes relating to charge states
            int pos = 0;
            attrs[pos++] = StatUtils.min(charges);
            attrs[pos++] = StatUtils.max(charges);
            attrs[pos] = attrs[pos-1] - attrs[pos-2];
            attrs[++pos] = 0;
            for (int i=0; i<fracs.length; i++) {
                attrs[pos] += fracs[i] * Math.abs(charges[i]);
            }
            attrs[++pos] = 0;
            for (int i=0; i<fracs.length; i++) {
                attrs[pos] += fracs[i] * Math.abs(Math.abs(charges[i]) - attrs[pos-1]);
            }
            pos++;
            
            // Compute attributes relating to ionization / affinity
            if (anyMissing) {
                Arrays.fill(attrs, pos, attrs.length, Double.NaN);
                ptr.addAttributes(attrs);
                continue;
            }
            double cationFrac = 0, anionFrac = 0;
            double cationIonizationSum = 0, anionAffinitySum = 0;
            double meanCationEN = 0, meanAnionEN = 0;
            for (int e=0; e<charges.length; e++) {
                if (charges[e] < 0) {
                    anionFrac += fracs[e];
                    meanAnionEN += electronegativity[elems[e]] * fracs[e];
                    anionAffinitySum -= charges[e] * affinity[elems[e]] * fracs[e];
                } else {
                    cationFrac += fracs[e];
                    meanCationEN += electronegativity[elems[e]] * fracs[e];
                    for (int c=0; c<charges[e]; c++) {
                        cationIonizationSum += IonizationEnergies[elems[e]][c] * fracs[e];
                    }
                }
            }
            meanAnionEN /= anionFrac;
            meanCationEN /= cationFrac;
            anionAffinitySum /= anionFrac;
            cationIonizationSum /= cationFrac;
            
            attrs[pos++] = cationIonizationSum;
            attrs[pos++] = anionAffinitySum;
            attrs[pos++] = meanAnionEN - meanCationEN;
            
            // Add attributes to entry
            entry.addAttributes(attrs);
        }
        
        // Print out summary of missing data
        if (! missingIonizationData.isEmpty()) {
            System.err.println("WARNING: Missing ionization energy data for");
            for (Map.Entry<Integer, Set<Integer>> element : 
                    missingIonizationData.entrySet()) {
                String elem = LookupData.ElementNames[element.getKey()];
                System.err.print("\t" + elem + ":");
                for (int state : element.getValue()) {
                    System.err.print(" +" + state);
                }
                System.err.println();
            }
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Add in description
        output += "(8) Minimum, maximum, range, mean, and mean absolute deviation "
                + "in oxidation state. Electronegativity difference between anions "
                + "and cations. Cumulative ionization energies for cations and "
                + "electron affinitity times charge state for anions.";
        
        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String,Citation>> output = new LinkedList<>();
        Citation citation = new Citation(this.getClass(),
                "Article",
                new String[]{"A. Deml", "et al."},
                "Predicting density functional theory total energies and enthalpies of formation of metal-nonmetal compounds by linear regression",
                "http://link.aps.org/doi/10.1103/PhysRevB.89.094104",
                null
            );
        output.add(new ImmutablePair<>("Used these attributes to predict "
                + "total energy of ionic compounds.", citation));
        return output;
    }
    
}
