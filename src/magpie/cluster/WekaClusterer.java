package magpie.cluster;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.user.CommandHandler;
import magpie.utility.WekaUtility;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.Clusterer;
import weka.core.Instances;
import weka.core.OptionHandler;

/**
 * Use Weka to cluster data.
 * 
 * <usage><p><b>Usage</b>: &lt;method> [&lt;options...>]
 * <br><pr><i>method</i>: Method used to cluster data. Name of a Weka Clusterer ("?" for available methods)
 * <br><pr><i>options...</i>: Options for the clusterer</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class WekaClusterer extends BaseClusterer {
    /** Clusterer used internally */
    AbstractClusterer Clusterer = null;

    public WekaClusterer() {
        Clusterer = instantiateClusterer("SimpleKMeans", null);
    }

    @Override
    public WekaClusterer clone() {
        WekaClusterer x = (WekaClusterer) super.clone(); 
        try {
            WekaUtility.importWekaHome();
            x.Clusterer = (AbstractClusterer) AbstractClusterer.makeCopy((Clusterer) Clusterer);
        } catch (Exception e) {
            throw new Error(e);
        }
        return x;
    }
        
    /**
     * Generate a new instance of a Weka-based clusterer.
     * @param Name Name of clusterer
     * @param Options List of options
     * @return New instance, as requested
     */
    protected static AbstractClusterer instantiateClusterer(String Name, String[] Options) {
        if (! Name.startsWith("weka.clusterers.")) {
            Name = "weka.clusterers." + Name;
        }
        try {
			WekaUtility.importWekaHome();
            return (AbstractClusterer) AbstractClusterer.forName(Name, Options);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        if (Options.length == 0) return;
        String Method, MethodOptions[] = null;
        try {
            Method = Options[0];
            if (Method.contains("?")) {
                System.out.println("Available Clusterers:");
                System.out.println(WekaUtility.printImplmentingClasses(weka.clusterers.Clusterer.class, true));
                return;
            }
            if (Options.length > 1)
                MethodOptions = Arrays.copyOfRange(Options, 1, Options.length);
            Clusterer = instantiateClusterer(Method, MethodOptions);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <method> [<options...>]";
    }

    @Override
    protected int NClusters_protected() {
        try {
            return Clusterer.numberOfClusters();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected void train_protected(Dataset Data) {
        try {
            Instances wekaData = Data.transferToWeka(false, false);
            Clusterer.buildClusterer(wekaData);
            Data.restoreAttributes(wekaData);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected int[] label_protected(Dataset Data) {
        try {
            Instances wekaData = Data.transferToWeka(false, false);
            int[] output = new int[Data.NEntries()];
            for (int i=0; i<output.length; i++)
                output[i] = Clusterer.clusterInstance(wekaData.get(i));
            Data.restoreAttributes(wekaData);
            return output;
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected List<String> getClustererDetails(boolean htmlFormat) {
        List<String> output = new LinkedList<>();
        output.add("Clusterer name:    " + Clusterer.getClass().getSimpleName());
        
        String options = "";
        OptionHandler ptr = (OptionHandler) Clusterer;
        for (String option : ptr.getOptions()) {
            options += " " + option;
        }
        
        output.add("Clusterer options:" + options);
        
        return output;
    }
    
    
}
