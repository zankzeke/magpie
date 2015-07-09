package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;

/**
 * Generate new crystalline compound by substituting elements onto all sites 
 * of a known prototype. 
 * 
 * <usage><p><b>Usage</b>: $&lt;prototypes&gt; &lt;elements...&gt;
 * <br><pr><i>prototypes</i>: {@linkplain CrystalStructureDataset} containing
 * prototype structures (as entries).
 * <br><pr><i>elements</i>: List of elements to substitute</usage>
 * @author Logan Ward
 */
public class CombinatorialSubstitutionGenerator extends BaseEntryGenerator {
    /** List of elements to use (id is Z-1) */
    final protected List<Integer> Elements = new ArrayList<>();
    /** List of prototype structures */
    final protected List<AtomicStructureEntry> Prototypes = new ArrayList<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Set list of elements to use for substitutions.
     * @param elements List of element abbreviations
     */
    public void setElements(List<Integer> elements) {
        elements.clear();
        Elements.addAll(elements);
    }

    /**
     * Define the list of prototype structures to use to create dataset.
     * @param prototypes List containing prototype structures
     */
    public void setPrototypes(List<AtomicStructureEntry> prototypes) {
        Prototypes.clear();
        Prototypes.addAll(prototypes);
    }
   
    @Override
    public List<BaseEntry> generateEntries() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
