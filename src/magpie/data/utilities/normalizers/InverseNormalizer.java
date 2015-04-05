package magpie.data.utilities.normalizers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Invert the attributes. Here, I'm defining 1 / 0 = 0 to avoid numerical problems.
 * If you're using this on attributes that could be zero, don't blame the author.
 * 
 * <p>This class was originally used to normalize data that lies on an exponential
 * distribution.
 * 
 * <usage><p><b>Usage</b>: &lt;scale&gt;
 * <br><pr><i>scale</i>: Typical scale of distribution</usage>
 * 
 * @author Logan Ward
 */
public class InverseNormalizer extends BaseDatasetNormalizer {
    /** Scale of data */
    private double Scale = 1;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() != 1) {
            throw new Exception(printUsage());
        }
        try {
            Scale = Double.parseDouble(Options.get(0).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }

    /**
     * Define scale of attributes / class. Inverse will be Scale / x.
     * @param s Desired scale factor 
     */
    public void setScale(double s) {
        this.Scale = s;
    }
    
    @Override
    protected void trainOnMeasuredClass(Dataset Data) {
        // Nothing to do
    }

    @Override
    protected void trainOnAttributes(Dataset Data) {
        // Nothing to do
    }

    @Override
    protected void normalizeAttributes(Dataset Data) {
        for (BaseEntry entry : Data.getEntries()) {
            double[] attr = entry.getAttributes();
            for (int i=0; i<attr.length; i++) {
                attr[i] = attr[i] == 0 ? 0 : Scale / attr[i];
            }
            entry.setAttributes(attr);
        }
    }

    @Override
    protected void restoreAttributes(Dataset Data) {
        normalizeAttributes(Data); // Same thing
    }

    @Override
    protected void normalizeClassVariable(Dataset Data) {
        for (BaseEntry entry : Data.getEntries()) {
            if (entry.hasMeasurement()) {
                double x = entry.getMeasuredClass();
                x = x == 0 ? 0.0 : Scale / x;
                entry.setMeasuredClass(x);
            }
            if (entry.hasPrediction()) {
                double x = entry.getPredictedClass();
                x = x == 0 ? 0.0 : Scale / x;
                entry.setPredictedClass(x);
            }
        }
    }

    @Override
    protected void restoreClassVariable(Dataset Data) {
        normalizeClassVariable(Data);
    }
}
