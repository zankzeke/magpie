
package magpie.models.classification;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import magpie.data.*;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.PrototypeEntry;
import magpie.csp.diagramdata.OnTheFlyPhaseDiagramStatistics;
import magpie.csp.diagramdata.PhaseDiagramStatistics;
import magpie.utility.interfaces.Savable;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Predict whether instance of a prototype structure will be a member of one of 
 *  several mutually-exclusive classes. For example, predict whether it will have
 *  a negative or positive formation energy. Uses the cumulant-expansion method
 *  originally demonstrated by <a href="http://www.nature.com/nmat/journal/v5/n8/full/nmat1691.html">Fisher <i>et al</i></a>,
 *  which is described in more detail in the documentation {@linkplain PhaseDiagramStatistics}.
 * 
 * <p>Requires that dataset be a {@linkplain PrototypeDataset}.
 * 
 * <usage><p><b>Usage</b>: &lt;filename&gt; &lt;order&gt;
 * <br><pr><i>filename</i>: Path to a file that contains all known compounds.
 * <br><pr><i>order</i>: Number sites in prototype</usage>
 * 
 * <p><b><u>Implemented Print Commands</u></b>
 * 
 * <print><p><b>cumulants [&lt;toPrint&gt;]</b> - Print out the most important cumulant functions for each class
 * <br><pr><i>toPrint</i>: Number of top cumulants to print (default 10)</print>
 * 
 * <p><b><u>Implemented Save Formats:</b></u>
 * 
 * <save><p><b>cumulants</b> - Print the value of the cumulant between each variable and each class
 * <br>Writes a file for each possible value of the class variable</save>
 * 
 * @author Logan Ward
 */
public class CumulantExpansionClassifier extends BaseClassifier implements Savable {
    /** Probability that the desired compound is stable */
    private double[] ClassProbability;
    /** Cumulants functions between an entry being each class and each condition */
    private double[][][] Cumulants;
    /** Number of constituents in prototype */
    private int NComponents = 3;
    /** Desired number of composition bins. */
    private int[] NBins = new int[]{30,120};
    /** Data about what compositions have compounds in all phase diagrams */
    private PhaseDiagramStatistics PhaseDiagramStats;

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
    public void setOptions(List<Object> Options) throws Exception {
        String filename;
        try {
            filename = Options.get(0).toString();
            NComponents = Integer.parseInt(Options.get(1).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        defineKnownCompounds(filename);
    }

    @Override
    public String printUsage() {
        return "Usage: <known compound filename> <number of constituents in prototype>";
    }
    
    /**
     * Define compositions of all known compounds. This data forms the basis of all inferences made by this model
     * @param filename Path to file containing known compounds
     */
    public void defineKnownCompounds(String filename) {
        PhaseDiagramStats = new OnTheFlyPhaseDiagramStatistics();
        // PhaseDiagramStats = new OnTheFlyPhaseDiagramStatistics();
        PhaseDiagramStats.importKnownCompounds(filename, NComponents, NBins);
        resetModel();
    }

    /**
     * Define number of desired bins of common compositions. Default is 30 bins
     * of binary compositions, 100
     *
     * @param NBins Number of compositions to store for each number of
     * compounds. x[0] is for binaries, x[1] is for ternaries, ...
     */
    public void setNBins(int[] NBins) {
        this.NBins = NBins;
        resetModel();
    }

    /**
     * Define the phase diagram statistics object used to make predictions. 
     * 
     * <p>NOTE: Also sets {@linkplain #NComponents}.
     * @param PhaseDiagramStats 
     */
    public void setPhaseDiagramStats(PhaseDiagramStatistics PhaseDiagramStats) {
        this.PhaseDiagramStats = PhaseDiagramStats;
        NComponents = PhaseDiagramStats.NComponents();
    }
    
    

    @Override
    protected String printModel_protected() {
        return "Cumulant expansion based on " + PhaseDiagramStats.NCompounds() 
                + "\n\tsorted into " + PhaseDiagramStats.NCompositions() + " bins.";
    }
    
    @Override
    protected void train_protected(Dataset TrainData) {
        if (!(TrainData instanceof PrototypeDataset)) {
            throw new Error("Data must be a PrototypeDataset");
        }
        PrototypeDataset data = (PrototypeDataset) TrainData;
        // Get cumulants for each class
        ClassProbability = TrainData.getDistribution();
        Cumulants = new double[ClassProbability.length][][];
        for (int c = 0; c < TrainData.NClasses(); c++) {
            boolean[] isClass = new boolean[TrainData.NEntries()];
            for (int e = 0; e < TrainData.NEntries(); e++) {
                isClass[e] = TrainData.getEntry(e).getMeasuredClass() == c;
            }
            Cumulants[c] = PhaseDiagramStats.getCumulants(data, isClass);
        }
    }

    @Override
    public void run_protected(Dataset TrainData) {
        if (!(TrainData instanceof PrototypeDataset)) {
            throw new Error("Data must be a PrototypeDataset");
        }
        
        // Calculate probability for each entry being in each class
        double[] classProb = new double[TrainData.NClasses()];
        for (BaseEntry Ptr : TrainData.getEntries()) {
            PrototypeEntry E = (PrototypeEntry) Ptr;
            for (int c = 0; c < TrainData.NClasses(); c++) {
                classProb[c] = PhaseDiagramStats.evaluateProbability(E, Cumulants[c], ClassProbability[c]);
            }
            // Normalize so that sum(classProb) == 1
            double sum = StatUtils.sum(classProb);
            for (int c = 0; c < classProb.length; c++) {
                classProb[c] /= sum;
            }
            // Store result
            E.setClassProbabilities(classProb);
        }
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty())
            return super.printCommand(Command); 
        String Action = Command.get(0).toLowerCase();
        switch (Action) {
            case "cumulants": {
                int toPrint = 10;
                try {
                    if (Command.size() == 2)
                        toPrint = Integer.parseInt(Command.get(1));
                } catch (NumberFormatException e) {
                    throw new Exception("Usage: culumants [<toPrint>]");
                }
                String output = "";
                for (int i=0; i<NClasses; i++) {
                    output += "Cumulants for Class #" + i + "\n";
                    output += "Factor\tg(class="+i+",factor)";
                    output += PhaseDiagramStats.printCumulants(Cumulants[i], toPrint);
                    output += "\n";
                }
                return output;
            }
            default: return super.printCommand(Command);
        }
    }

    @Override
    public String saveCommand(String Basename, String Command) throws Exception {
        switch (Command.toLowerCase()) {
            case "cumulants": {
                for (int i=0; i<NClasses; i++) {
                    String filename = Basename + i + ".cumulants";
                    try (PrintWriter fp = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
                        fp.print(PhaseDiagramStats.printCumulants(Cumulants[i], -1));
                    }
                }
                return Basename + "#.cumulants";
            }
            default: 
				throw new Exception("Save format not supported: " + Command);
        }
    }
    
    
}
