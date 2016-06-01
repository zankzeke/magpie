package magpie.attributes.generators.composition;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Generate attributes based on fraction of electrons in valence shell of 
 * constituent elements.
 * 
 * <p>Creates 4 attributes: [Composition-weighted mean # of electrons 
 * in the {s,p,d,f} ] / [Mean # Valence Electrons]
 * 
 * <p>Originally presented by: 
 * <a href="http://journals.aps.org/prb/abstract/10.1103/PhysRevB.89.094104">
 * Meredig <i>et al.</i> <u>Physical Review B</u> (2015)</a>
 * 
 * <usage><p><b>Usage</b>: *No options*</usage> 
 * @author Logan Ward
 */
public class ValenceShellAttributeGenerator extends BaseAttributeGenerator
        implements Citable {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check if this is an composition dataset
        if (! (data instanceof CompositionDataset)) {
            throw new Exception("Data isn't a CompositionDataset");
        }
        CompositionDataset ptr = (CompositionDataset) data;
        
        // Load in the number of electrons in each shell
        Character[] shell = new Character[]{'s', 'p', 'd', 'f'};
        double[][] n_valance = new double[4][];
        for (int i = 0; i < 4; i++) {
            n_valance[i] = ptr.getPropertyLookupTable("N" + shell[i] + "Valence");
        }

        // Generate attribute names
        List<String> newNames = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            newNames.add("frac_" + shell[i] + "Valence");
        }
        ptr.addAttributes(newNames);
        
        // Compute attributes
        for (int i = 0; i < ptr.NEntries(); i++) {
            double[] total_e = new double[4];
            double sum_e = 0.0;
            // First, get the average number of electrons in each shell
            for (int j = 0; j < 4; j++) {
                total_e[j] = ptr.getEntry(i).getMean(n_valance[j]);
                sum_e += total_e[j];
            }

            // Convert to fractions
            for (int j = 0; j < 4; j++) {
                total_e[j] /= sum_e;
            }

            // Add to entry
            ptr.getEntry(i).addAttributes(total_e);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        return getClass().getName() + (htmlFormat ? " " : ": ") + " (4) " 
                + "Composition-weighted average number of {s,p,d,f} valence electrons "
                + "of each constituent element divided by average number of total "
                + "valence electrons";
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
                List<Pair<String,Citation>> output = new LinkedList<>();
        Citation citation = new Citation(this.getClass(),
                "Article",
                new String[]{"B. Meredig", "A. Agrawal", "et al."},
                "Combinatorial screening for new materials in unconstrained composition space with machine learning",
                "http://link.aps.org/doi/10.1103/PhysRevB.89.094104",
                null
            );
        output.add(new ImmutablePair<>("Introduced using fraction of electrion in "
                + "each valence shell as an attribute", citation));
        return output;

    }
    
    
}
