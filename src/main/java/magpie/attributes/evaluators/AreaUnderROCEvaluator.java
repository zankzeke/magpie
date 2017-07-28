
package magpie.attributes.evaluators;

import java.util.Comparator;
import java.util.List;
import magpie.data.Dataset;
import magpie.utility.UtilityOperations;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Ranks entries based on the area under the <a href="http://en.wikipedia.org/wiki/Receiver_operating_characteristic">
 * Receiver Operating Characteristic Curve</a>. This measure is best used when 
 * testing the ability of attributes to partition data between two different 
 * classes. In the event that more than one class is present, 
 * it will report the ability to distinguish whether an entry is a certain, 
 * user-specified class.
 * 
 * 
 * <usage><p><b>Usage</b>: [&lt;class&gt;]
 * <pr><br><i>class</i>: Name of class for which the ability to distinguish is being assessed (default: class #0)</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class AreaUnderROCEvaluator extends BaseAttributeEvaluator {
    /** Class for which the ability to distinguish is being assesed */
    private String TargetClass = null;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() == 1) {
            
        } else if (! Options.isEmpty()) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <class>";
    }

    /**
     * Define the class for which the ability to classify is being assessed.
     * @param className Name of class
     */
    public void setTargetClass(String className) {
        this.TargetClass = className;
    }

    @Override
    protected Comparator<Double> compare() {
        return new Comparator<Double>() {

            @Override
            public int compare(Double o1, Double o2) {
                return o2.compareTo(o1);
            }
        };
    }    

    @Override
    protected double[] evaluateAttributes_internal(Dataset Data) {
        if (Data.NClasses() == 1) {
            throw new Error("Class variable must have multiple classes");
        }
        // Get class ID
        int classID = 0;
        if (TargetClass != null) {
            classID = ArrayUtils.indexOf(Data.getClassNames(), TargetClass);
            if (classID == -1) {
                throw new Error("No such class: " + TargetClass);
            }
        }
        
        // Allocate needed arrays
        double[] class_array = Data.getMeasuredClassArray();
        int[] dist = Data.getDistributionCount();
        double[] output = new double[Data.NEntries()];
        
        // Calculate AUC for each feature (see MATLAB's rankfeatures)
        for (int i=0; i<Data.NAttributes(); i++) {
            double[] feature = Data.getSingleAttributeArray(i);
            int[] rank = UtilityOperations.sortAndGetRanks(feature, true);
            double auc=0; int type0found=0;
            for (int j=0; j<Data.NEntries(); j++) 
                if (class_array[rank[j]] == classID) 
                    type0found++;
                else
                    auc+=type0found;
            auc = auc / dist[classID] / (class_array.length - dist[classID]) - 0.5;
            auc = Math.abs(auc) + 0.5;
            output[i] = auc;
        }
        return output;
    }
}
