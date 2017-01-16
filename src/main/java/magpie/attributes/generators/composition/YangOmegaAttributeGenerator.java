package magpie.attributes.generators.composition;

import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Compute the &Omega; and &delta; parameters developed by <a href="http://dx.doi.org/10.1016/j.matchemphys.2011.11.021">
 * Yang and Zhang</a>. These parameters are based on the liquid formation enthalpy and atomic sizes of elements,
 * respectively, and were originally developed to predict whether a metal alloy will form a solid solution
 * or bulk metallic glass.
 * <p>
 * <p>&Omega; is derived from the melting temperature, ideal mixing entropy, and regular solution solution
 * interaction parameter (&Omega;<sub>i,j</sub>) predicted by the Miedema model for binary liquids. Specifically,
 * it is computed using the relationship:</p>
 * <p>
 * <p><code>&Omega;=T<sub>m</sub>&Delta;S<sub>mix</sub>/|&Delta;H<sub>mix</sub>|</code></p>
 * <p>
 * <p>where T<sub>m</sub> is the composition-weighted average of the melting temperature, &Delta;S<sub>mix</sub> is
 * the ideal solution entropy, and &Delta;H<sub>mix</sub> is the mixing enthalpy. The mixing enthalpy is computed
 * using the Miedema mixing enthalpies tabulated by <a href="https://www.jstage.jst.go.jp/article/matertrans/46/12/46_12_2817/_article">
 * Takeuchi and Inoue</a>, where:</p>
 * <p>
 * <p><code>&Delta;H<sub>mix</sub>=&Epsilion;&Omega;<sub>i,j</sub>c<sub>i</sub>c<sub>j</sub></subj></code></p>
 * <p>
 * <p>and &Omega;<sub>i,j</sub> = 4 * &Delta;H<sub>mix</sub>.</p>
 * <p>
 * <p>&delta; is related to the polydispersity of atomic sizes, and is computed using the relationship:</p>
 * <p>
 * <p><math>&delta;=(&Epsilon;c<sub>i</sub>(1-r<sub>i</sub>/r<sub>average</sub>)<sup>2</sup>)<sup>1/2</sup></math></p>
 * <p>
 * <p>where r<sub>i</sub> is the atomic size. Here, we use the atomic radii compiled by
 * <a href="http://openurl.ingenta.com/content/xref?genre=article&issn=0950-6608&volume=55&issue=4&spage=218">
 * Miracle <i>et al.</i></a> rather than those compiled by Kittel, as in the original work</p>
 * <p>
 * <usage><p><b>Usage</b>: *No options*</p></usage>
 */
public class YangOmegaAttributeGenerator extends BaseAttributeGenerator implements Citable {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // No options
        if (!Options.isEmpty()) {
            throw new IllegalArgumentException(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Check that data is a CompositionDataset
        if (!(data instanceof CompositionDataset)) {
            throw new IllegalArgumentException("Data must be a CompositionDataset");
        }

        // Create the attribute names
        List<String> newNames = new ArrayList<>();
        newNames.add("Yang_Omega");
        newNames.add("Yang_delta");
        data.addAttributes(newNames);

        // Get the radii
        double[] radii = ((CompositionDataset) data).getPropertyLookupTable("MiracleRadius");
        double[] meltingT = ((CompositionDataset) data).getPropertyLookupTable("MeltingT");
        double[][] miedema = ((CompositionDataset) data).getPairPropertyLookupTable("MiedemaLiquidDeltaHf");

        // Compute the attributes
        for (BaseEntry entryPtr : data.getEntries()) {
            // Cast to CompositionEntry
            CompositionEntry entry = (CompositionEntry) entryPtr;
            double[] fracs = entry.getFractions();
            int[] elems = entry.getElements();

            // Compute the formation enthalpy
            double averageTm = entry.getMean(meltingT);

            // Compute the ideal entropy
            double entropy = 0.0;
            for (double frac : fracs) {
                entropy += frac > 0 ? frac * Math.log(frac) : 0.0;
            }
            entropy *= 8.314 / 1000;

            // Compute the enthalpy
            double enthalpy = 0.0;
            for (int e1 = 0; e1 < elems.length; e1++) {
                for (int e2 = e1 + 1; e2 < elems.length; e2++) {
                    enthalpy += LookupData.readPairTable(miedema, elems[e1], elems[e2]) * fracs[e1] * fracs[e2];
                }
            }
            enthalpy *= 4;

            // Compute omega
            entry.addAttribute(Math.abs(averageTm * entropy / enthalpy));

            // Compute delta
            double deltaSquared = 0.0;
            double averageR = entry.getMean(radii);
            for (int e1 = 0; e1 < elems.length; e1++) {
                deltaSquared += fracs[e1] * Math.pow(1 - radii[elems[e1]] / averageR, 2);
            }
            entry.addAttribute(Math.sqrt(deltaSquared));
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");

        // Add in description
        output += "(2) Descriptors based on the liquid formation enthalpy and atomic size polydispersity that were " +
                "originally designed to predict the formation of metallic glasses and solid solution alloys.";

        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = new LinkedList<>();

        output.add(new ImmutablePair<>(
                "Developed these descriptors and applied them to predicting high entropy alloys and bulk metallic glasses.",
                new Citation(getClass(),
                        "Article",
                        new String[]{"X. Yang", "Y. Zhang"},
                        "Prediction of high-entropy stabilized solid-solution in multi-component alloys",
                        "http://linkinghub.elsevier.com/retrieve/pii/S0254058411009357",
                        null
                )
        ));

        return output;
    }
}
