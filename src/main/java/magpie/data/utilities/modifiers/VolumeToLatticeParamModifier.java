package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;

/**
 * Add a new property to a CompositionDataset that represents a cubic lattice parameter. 
 * User specifies how many atoms in the unit cell, and the conversion is simply:<br>
 * <code>(volume/pa * atoms/cell) ^ (1/3)</code>. 
 * 
 * <p>The dataset must have "volume_pa" as a property for this to work. The target property
 *  will be changed to "lat_param" after this operation completes. 
 * 
 * <usage><p><b>Usage</b>: &lt;atoms/cell>
 * <br><pr><i>atoms/cell</i>: Number of atoms in unit cell</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class VolumeToLatticeParamModifier extends BaseDatasetModifier {
    /** Number of atoms per cell in the unit cell */
    int perCell;

    @Override
    public void setOptions(List Options) throws Exception {
        try {
            perCell = Integer.parseInt(Options.get(0).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <atoms per unit cell>";
    }

    /**
     * Set number of atoms in unit cell. Used when calcualting "cubic" lattice parameter
     * @param perCell Number of atoms per unit cell
     */
    public void setPerCell(int perCell) {
        this.perCell = perCell;
    }
    
    

    @Override
    protected void modifyDataset(Dataset Data) {
        if (! (Data instanceof CompositionDataset)) 
            throw new Error("Dataset must be a CompostionDataset.");
        CompositionDataset Ptr = (CompositionDataset) Data;
        
        // Modify the Dataset
        int volumeID = Ptr.getPropertyIndex("volume_pa");
        if (volumeID == -1)
            throw new Error("Dataset is missing \"volume_pa\" property");
        if (Ptr.getPropertyIndex("lat_param") != -1)
            throw new Error("Dataset already has a \"lat_param\" property.");
        Ptr.addProperty("lat_param");
        
        
        // Add the lattice parameter for each entry
        for (int i=0; i<Ptr.NEntries(); i++) {
            double measured, predicted;
            // Get the measured and/or predicted values
            if (Ptr.getEntry(i).hasMeasuredProperty(volumeID))
                measured = convertVolumeToLatticeParam(Ptr.getEntry(i).getMeasuredProperty(volumeID));
            else measured = Double.NaN;
            if (Ptr.getEntry(i).hasPredictedProperty(volumeID))
                predicted = convertVolumeToLatticeParam(Ptr.getEntry(i).getPredictedProperty(volumeID));
            else predicted = Double.NaN;
            
            Ptr.getEntry(i).addProperty(measured, predicted);
        }
        
        // Set the target class to lattice parameter
        Ptr.setTargetProperty("lat_param", true);
    }
    
    /**
     * Calculate the equivalent cubic lattice parameter corresponding to a volume per
     *  atom, given the number of atoms in the unit cell
     * @param volume Volume per atom
     * @return (volume/pa * atoms/cell) ^ (1/3)
     */
    protected double convertVolumeToLatticeParam(double volume) {
        return Math.pow(volume * (double) perCell, 1.0/3.0);
    }
}
