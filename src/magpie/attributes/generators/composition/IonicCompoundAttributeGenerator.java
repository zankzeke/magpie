package magpie.attributes.generators.composition;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.utility.tools.OxidationStateGuesser;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Attributes suitable for modeling ionic compounds. Based on work by Ann Deml:
 * 
 * <p><center>Deml, <i>et al.</i> (?). Journal. Volume (Year), Page
 * 
 * <p>These attributes are based on the formal charges of 
 * 
 * <p>Currently implemented attributes:
 * <ol>
 * <li>Statistics of formal charges (min, max, range, mean, variance)
 * <li>Cumulative ionization energies / electron affinities
 * <li>Difference in electronegativity between cation and anion.
 * </ol>
 * 
 * @author Logan Ward
 */
public class IonicCompoundAttributeGenerator extends BaseAttributeGenerator {
    /** Tool used to compute electronegativity */
    protected OxidationStateGuesser ChargeGuesser = null;
    /** Ionization energies of each element */
    protected double[][] IonizationEnergies;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                throw new Error(e);
            }
        }
        
        // Read in ionization energies
        if (IonizationEnergies == null) {
            try {
                LookupData.readIonizationEnergies(compDataset.DataDirectory + "/IonizationEnergies.table");
                IonizationEnergies = LookupData.IonizationEnergies;
            } catch (IOException i) {
                throw new Error(i);
            }
        }
        
        // Compute the attributes for each entry
        double[] attrs = new double[newNames.size()];
        for (BaseEntry ptr : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) ptr;
            
            // Compute charge states
            List<int[]> possibleStates = ChargeGuesser.getPossibleStates(entry);
            
            // If no possible states, set attributes to NaN
            if (possibleStates.isEmpty()) {
                Arrays.fill(attrs, Double.NaN);
                ptr.addAttributes(attrs);
                continue;
            }
            
            // Convert charge states to double array
            int[] chargesInt = possibleStates.get(0); 
            double[] charges = new double[chargesInt.length];
            for (int i=0; i<charges.length; i++) {
                charges[i] = (double) chargesInt[i];
            }
            
            double[] fracs = entry.getFractions();
            int[] elems = entry.getElements();
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
                attrs[pos] += fracs[i] * Math.abs(charges[i] - attrs[pos-1]);
            }
            pos++;
            
            // Compute attributes relating to ionization / affinity
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
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        // Add in description
        output += "(8) Minimum, maximum, range, mean, and mean absolute deviation "
                + "in oxidation state. Electronegativity difference between anions "
                + "and cations. Cumulative ionization energies for cations and "
                + " electron affinitity times charge state for anions.";
        
        return output;
    }
    
}
