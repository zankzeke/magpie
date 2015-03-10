package magpie.data.utilities.splitters;

import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;

/**
 * This class splits data into multiple properties based on the attributes of each entry. User 
 * supplies the intervals, which are first sorting into ascending order. Then, the data is 
 * partitioned into bins with these points as edges.
 * 
 * <usage><p><b>Usage</b>: &lt;attribute> &lt;bin edges...>
 * <br><pr><i>attribute</i>: Name of attribute on which to split dataset
 * <br><pr><i>bin edges...</i>: Values on which to split data (i.e. "1" to split into (-Inf, 1] and (1, Inf])</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class AttributeIntervalSplitter extends BaseDatasetSplitter {
    /** Name of attribute used for split */
    protected String AttributeName;
    /** ID of attribute used for split */
    protected int AttributeID = -1;
    /** Values to use as bin edges */
    protected double[] BinEdges = null;

    @Override
    public AttributeIntervalSplitter clone() {
        AttributeIntervalSplitter x = 
                (AttributeIntervalSplitter) super.clone();
        x.BinEdges = BinEdges.clone();
        return x;
    }

    @Override
    public int[] label(Dataset D) {
        int[] output = new int[D.NEntries()];
        for (int i=0; i<output.length; i++) {
            double x = D.getEntry(i).getAttribute(AttributeID);
            output[i] = 0;
            for (int j=0; j<BinEdges.length; j++)
                if (x >= BinEdges[j]) output[i] = j+1;
        }
        return output;
    }

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        if (Options.length < 2)
            throw new Exception(printUsage());
        AttributeName = Options[0];
        BinEdges = new double[Options.length - 1];
        try {
            for (int i=0; i<BinEdges.length; i++)
                BinEdges[i] = Double.valueOf(Options[i+1]);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <attribute> <bin edges...>";
    }

    @Override
    public void train(Dataset TrainingSet) {
        AttributeID = TrainingSet.getAttributeIndex(AttributeName);
        if (AttributeID == -1) {
            throw new Error("Dataset does not contain feature: "+AttributeName);
        }
    }
}
