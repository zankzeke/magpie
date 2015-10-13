package magpie.data.utilities.normalizers;

import java.util.*;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * Use multiple strategies to normalize dataset.
 *
 * <usage><p>
 * <b>Usage</b>: -norm &lt;method 1&gt; [&lt;options 1...&gt;] [-norm &lt;method
 * 2&gt; [&lt;options 2...&gt;] ] &lt;...&gt;
 * <br><pr><i>method</i>: Name of another normalizer method
 * <br><pr><i>options</i>: Any options for that method</usage>
 *
 * @author Logan Ward
 */
public class MultiNormalizer extends BaseDatasetNormalizer {

    /**
     * List of normalizers to use
     */
    private List<BaseDatasetNormalizer> Normalizers = new LinkedList<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.isEmpty()
                || !Options.get(0).toString().equalsIgnoreCase("-norm")) {
            throw new Exception(printUsage());
        }

        // Read options
        int pos = 2;
        String SubMethod;
        List SubOptions = new LinkedList();
        while (pos < Normalizers.size()) {
            SubMethod = Options.get(pos++).toString();
            while (!Options.get(pos).toString().equalsIgnoreCase("-norm")) {
                SubOptions.add(Options.get(pos++));
                if (pos == Options.size()) {
                    break;
                }
            }

            // Add in the new method
            BaseDatasetNormalizer norm = (BaseDatasetNormalizer) CommandHandler.instantiateClass("data.utilities.normalizers." + SubMethod,
                    SubOptions);
            addNormalizer(norm);
        }
    }

    @Override
    public String printUsage() {
        return "Usage: -norm <method 1> <options 1...> -norm <...>";
    }

    @Override
    protected void trainOnAttributes(Dataset Data) {
        for (BaseDatasetNormalizer Normalizer : Normalizers) {
            Normalizer.trainOnAttributes(Data);
            Normalizer.normalizeAttributes(Data);
        }
        
        restoreAttributes(Data);
    }

    @Override
    protected void trainOnMeasuredClass(Dataset Data) {
        for (BaseDatasetNormalizer Normalizer : Normalizers) {
            Normalizer.trainOnMeasuredClass(Data);
            Normalizer.normalizeClassVariable(Data);
        }
        
        restoreClassVariable(Data);
    }

    /**
     * Add a new normalizer to this multi normalizer.
     *
     * @param norm New normalizer
     */
    public void addNormalizer(BaseDatasetNormalizer norm) {
        Normalizers.add(norm);
    }

    @Override
    protected void normalizeAttributes(Dataset Data) {
        for (int i = 0; i < Normalizers.size(); i++) {
            Normalizers.get(i).normalizeAttributes(Data);
        }
    }

    @Override
    protected void normalizeClassVariable(Dataset Data) {
        for (int i = 0; i < Normalizers.size(); i++) {
            Normalizers.get(i).normalizeClassVariable(Data);
        }
    }

    @Override
    protected void restoreAttributes(Dataset Data) {
        for (int i = Normalizers.size() - 1; i >= 0; i--) {
            Normalizers.get(i).restoreAttributes(Data);
        }
    }
    
    @Override
    protected void restoreClassVariable(Dataset Data) {
        for (int i = Normalizers.size() - 1; i >= 0; i--) {
            Normalizers.get(i).restoreClassVariable(Data);
        }
    }

}
