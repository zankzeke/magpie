package magpie.data.materials;

/**
 * Holds collections of atomic-scale structures and their computed properties. 
 * Intended to be used to generate attributes which can be used to distinguish
 * between structures that are only slightly-distorted versions of each other.
 * In contrast, {@linkplain CrystalStructureDataset} can be used to generate 
 * attributes that predict properties of crystalline compounds with different 
 * class of crystal structures. That said, both store entries that are based on 
 * atomic-scale structures, so they both use {@linkplain CrystalStructureEntry}.
 * 
 * <p>This class was originally created to recreate the method developed by
 * <a href="http://arxiv.org/abs/1403.799"5>Seko et al.</a> to create 
 * interatomic potentials using machine learning.
 * 
 * <p><usage><b>Usage</b>: *No Options*</usage>
 * 
 * @author Logan Ward
 */
public class AtomicStructureDataset extends CrystalStructureDataset {
    /** Whether to compute attributes based on composition */
    protected boolean useCompositionAttributes = false;
    /** Whether to compute crystal-structure-based attributes */
    protected boolean useCrystalStructureAttributes = false;

    @Override
    protected void calculateAttributes() {
        throw new UnsupportedOperationException("Not yet supported");
    }
}
