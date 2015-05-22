package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.GCLPCalculator;

/**
 * Use GCLP compute the stability with respect to other phases. 
 * 
 * <usage><p><b>Usage</b>: $&lt;data&gt; &lt;energy name&gt; &lt;stability name&gt;
 * <br><pr><i>data</i>: {@linkplain CompositionDataset} containing energies of known compounds as the measured class variable
 * <br><pr><i>energy name</i>: Name of property storing energies of new compounds
 * <br><pr><i>stability name</i>: Name of property in which to store computed stablities</usage>
 * 
 * @author Logan Ward
 */
public class StabilityCalculationModifier extends BaseDatasetModifier {
    /** Tool used to perform GCLP */
    private GCLPCalculator GCLPCalculator = new GCLPCalculator();
    /** Name of property containing energy */
    private String EnergyName;
    /** Name of new property containing stability */
    private String StabilityName;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            if (Options.size() != 3) {
                throw new Exception();
            }
            setCompounds((CompositionDataset) Options.get(0));
            setEnergyName(Options.get(1).toString());
            setStabilityName(Options.get(2).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<known data> <energy name> <stability name>";
    }
    
    /** 
     * Define the data used to compute stability. This dataset must contain 
     * the energy of each compound as the measured class variable.
     * @param data Data containing the energies of all known phases
     */
    public void setCompounds(CompositionDataset data) {
        GCLPCalculator = new GCLPCalculator();
        GCLPCalculator.addPhases(data);
    }

    /**
     * Define the name of the property containing energy data
     * @param energyName Name of property
     */
    public void setEnergyName(String energyName) {
        this.EnergyName = energyName;
    }

    /**
     * Set the name of the property in which to store stability data
     * @param stabilityName Name of property to store result
     */
    public void setStabilityName(String stabilityName) {
        this.StabilityName = stabilityName;
    }

    @Override
    protected void modifyDataset(Dataset ptr) {
        // Make sure this dataset stores composition data
        if (! (ptr instanceof CompositionDataset)) {
            throw new Error("Dataset in not a CompositionDataset");
        }
        CompositionDataset data = (CompositionDataset) ptr;
        
        // Check that energy property exists
        int energyProp = data.getPropertyIndex(EnergyName);
        if (energyProp == -1) {
            throw new Error("No such prorperty: " + EnergyName);
        }
        
        // Get the index of the stability property (if it exists)
        int stabProp = data.getPropertyIndex(StabilityName);
        boolean wasAdded = false;
        if (stabProp == -1) {
            data.addProperty(StabilityName);
            wasAdded = true;
            stabProp = data.getPropertyIndex(StabilityName);
        }
        
        // Do GCLP for every entry
        for (BaseEntry entryPtr : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) entryPtr;
            
            // Perform GCLP
            double hullEnergy;
            try {
                GCLPCalculator.doGCLP(entry);
                hullEnergy = GCLPCalculator.getGroundStateEnergy();
            } catch (Exception e) {
                throw new Error(e);
            }
            
            // Add property if needed
            if (wasAdded) {
                entry.addProperty();
            }
            
            // Compute stability
            if (entry.hasMeasuredProperty(energyProp)) {
                entry.setMeasuredProperty(stabProp, 
                        entry.getMeasuredProperty(energyProp) - hullEnergy);
            }
            if (entry.hasPredictedProperty(energyProp)) {
                entry.setPredictedProperty(stabProp, 
                        entry.getPredictedProperty(energyProp) - hullEnergy);
            }
        }
    }
    
}
