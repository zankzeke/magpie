
package magpie.attributes.evaluators;

import java.util.List;
import magpie.data.Dataset;
import magpie.optimization.algorithms.OptimizationHelper;

/**
 * Ranks entries based on the area under the <a href="http://en.wikipedia.org/wiki/Receiver_operating_characteristic">
 * Receiver Operating Characteristic Curve</a>. This measure is best used when 
 * testing the ability of attributes to partition data between two different 
 * classes. In fact, it can only be used if the dataset in question has two exactly classes.
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * @author Logan Ward
 * @version 0.1
 */
public class AreaUnderROCEvaluator extends BaseAttributeEvaluator {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        /* Nothing to set */
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    

    @Override
    protected boolean positiveIsBetter() {
        return true;
    }

    @Override
    protected double[] evaluateAttributes_internal(Dataset Data) {
        if (Data.NClasses() != 2)
            throw new Error("Dataset must have exactly two classes");
        
        // Allocate needed arrays
        double[] class_array = Data.getMeasuredClassArray();
        int[] dist = Data.getDistributionCount();
        double[] output = new double[Data.NEntries()];
        
        // Calculate AUC for each feature (see MATLAB's rankfeatures)
        for (int i=0; i<Data.NAttributes(); i++) {
            double[] feature = Data.getSingleAttributeArray(i);
            int[] rank = OptimizationHelper.sortAndGetRanks(feature, true);
            double auc=0; int type0found=0;
            for (int j=0; j<Data.NEntries(); j++) 
                if (class_array[rank[j]] == 0) 
                    type0found++;
                else
                    auc+=type0found;
            auc = auc / dist[0] / dist[1] - 0.5;
            auc = Math.abs(auc) + 0.5;
            output[i] = auc;
        }
        return output;
    }
}
