
package magpie.data.materials;

import java.util.*;
import magpie.data.BaseEntry;

/**
 * Development version of {@linkplain CompositionDataset}. 
 * 
 * @author Logan Ward
 */
public class CompositionDatasetExperimental extends CompositionDataset {
    /** Elemental properties used when computing radius ratios */
    private List<String> RatioElementalProperties = new LinkedList<String>();

    @Override
    public void calculateAttributes() {
        // Add in "ratio" attributes
        generateRatioAttributes();
        
        // Do the rest
        super.calculateAttributes(); 
    }
    
    /**
     * Computes the ratio attributes for all properties defined in 
     *  {@linkplain #RatioElementalProperties}. Several different attributes
     * 
     * <ol>
     * <li><b>Maximum ratio</b>: Maximum value of the property for all elements 
     *  in the entry divided by the minimum
     * <li><b>Mean ratio</b>: Computed using: sum_i[sum_{i>j} x_i * x_j *  max(f_i,f_j) / min(f_i,f_j)]
     * / sum_i[sum_{i>j} x_i * x_j ]
     * <li><b>Majority ratio</b>: Ratio between the maximum and minimum of most and
     *  second-most prevalent elements in alloy. If some elements have equal amounts,
     *  computes the average of all possible arrangements
     * </li>
     * 
     */
    public void generateRatioAttributes() {
        // Find lookup properties that have positive (non-zero) values for all entries
        RatioElementalProperties.clear();
        for (String prop : ElementalProperties) {
			double[] lookup;
			try {
				lookup = getPropertyLookupTable(prop);
			} catch (Exception e) {
				throw new Error("Failed to get data for property: " + prop);
			}
            boolean isGood = true;
            for (double value : lookup) {
                if (Double.isNaN(value) || value <= 0) {
                    isGood = false;
                    break;
                }
            }
            if (isGood) RatioElementalProperties.add(prop);
        }
        
        // Add in attribute names
        for (String prop : RatioElementalProperties) {
            AttributeName.add("maxRatio_" + prop);
            AttributeName.add("meanRatio_" + prop);
            AttributeName.add("modeRatio_" + prop);
        }
        
        
        for (BaseEntry ptr : Entries) {
            CompositionEntry entry = (CompositionEntry) ptr;
            
            // Pre-allocate array storing radius ratio attributes
            double[] attributes = new double[RatioElementalProperties.size() * 3];
            
            // Special case: Only 1 element in composition (ratios are all 1)
            if (entry.getElements().length == 1) {
                Arrays.fill(attributes, 1.0);
                entry.addAttributes(attributes);
                continue;
            }
            
            // Find the most and second-most prevalent elements
            int[] elem = entry.getElements();
            double[] frac = entry.getFractions();
            double[] tempFrac = frac.clone(); Arrays.sort(tempFrac);
            double firstFrac = tempFrac[0], secondFrac = -1;
            for (int i=1; i < frac.length; i++) {
                if (tempFrac[i] < firstFrac - 1e-4) {
                    secondFrac = tempFrac[i];
                    break;
                }
            }
            if (secondFrac == -1) { // In case all have equal values
                secondFrac = firstFrac;
            }
            List<Integer> firstElems = new LinkedList<>(),
                    secondElems = new LinkedList<>();
            
            for (int i=0; i<elem.length; i++) {
                if (Math.abs(firstFrac - frac[i]) < 1e-4) firstElems.add(elem[i]);
                if (Math.abs(secondFrac - frac[i]) < 1e-4) secondElems.add(elem[i]);
            }
            int numElemComb = 0;
            for (Integer elemA : firstElems) {
                for (Integer elemB : secondElems) {
                    if (!Objects.equals(elemA, elemB)) numElemComb++;
                }
            }
            
            // Otherwise, for each property
            int count = 0;
            for (String property : RatioElementalProperties) {
				double[] lookup;
				try {
					lookup = getPropertyLookupTable(property);
				} catch (Exception e) {
					throw new Error("Failed to load data for property: " + property);
				}
                
                // Get maximum ratio 
                attributes[count++] = entry.getMaximum(lookup) / entry.getMinimum(lookup);
                
                // Get "mean" ratio
                double num = 0.0, denom = 0.0;
                for (int i=0; i<elem.length; i++) {
                    for (int j=i+1; j<elem.length; j++) {
                        num += lookup[elem[i]] > lookup[elem[j]] ? 
                                frac[i] * frac[j] * lookup[elem[i]] / lookup[elem[j]] :
                                frac[i] * frac[j] * lookup[elem[j]] / lookup[elem[i]];
                        denom += frac[i] * frac[j];
                    }
                }
                attributes[count++] = num / denom;
                
                // Get the "mode" ratio
                num = 0.0;
                for (Integer elemA : firstElems) {
                    for (Integer elemB : secondElems) {
                        if (!Objects.equals(elemA, elemB)) {
                            num += lookup[elemA] > lookup[elemB] ? 
                                    lookup[elemA] / lookup[elemB] :
                                    lookup[elemB] / lookup[elemA];
                        }
                    }
                }
                attributes[count++] = num / (double) numElemComb;
            }
            entry.addAttributes(attributes);
        }
    }
    
}
