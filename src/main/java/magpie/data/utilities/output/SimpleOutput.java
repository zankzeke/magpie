package magpie.data.utilities.output;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.user.CommandHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Print the name and measured class values of each attribute. Optionally, you 
 * can print only the top entries them in ranked order.
 * 
 * <p>Note: {@linkplain #printEntries(java.util.Collection, java.io.OutputStream) }
 * doesn't actually do anything if entries are ranked. This is because you 
 * need the entire dataset to do a ranking. So, iterative printing will not
 * be supported.
 * 
 * <usage><p><b>Usage</b>: [&lt;ranker&gt; &lt;# to print&gt; &lt;maximize|minimize&gt; &lt;measured|predicted&gt; [&lt;ranker options...&gt;]]
 * <pr><br><i>ranker</i>: Optional: Name of {@linkplain BaseEntryRanker} used
 * to rank entries before printing
 * <pr><br><i># to print</i>: Optional: Number of top entries to print. Can be "all"
 * <pr><br><i>maximum|minimum</i>: Optional: Whether to used measured or predicted class when ranking
 * <pr><br><i>measured|predicted</i>: Optional: Whether to used measured or predicted class when ranking
 * <pr><br><i>ranker options...</i>: Optional: Options for the entry ranker</usage>
 * @author Logan Ward
 */
public class SimpleOutput extends BaseDatasetOutput {
    /** Tool used to rank entries before printing them. Optional */
    protected BaseEntryRanker Ranker = null;
    /** Number of entries to print, if ranking. If -1, prints all */
    protected int NToPrint = -1;
    /** Number of digits in {@linkplain #NToPrint}. Used to make printing align */
    protected int NDigits;
    /** Width of largest entry */
    protected int EntryLength;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        BaseEntryRanker ranker;
        int nToPrint;
        
        // Option #1: Print everything in dataset order
        try {
            if (Options.isEmpty()) {
                ranker = null;
                nToPrint = -1;
            } else {
                // Option #2: Print only a certain number of ranked entries
                String rankerName = Options.get(0).toString();
                if (Options.get(1).toString().equalsIgnoreCase("all")) {
                    nToPrint = -1;
                } else {
                    nToPrint = Integer.parseInt(Options.get(1).toString());
                }
                
                // Get whether to use measured or predicted class
                boolean maximize;
                String temp = Options.get(2).toString().toLowerCase();
                if (temp.startsWith("max")) {
                    maximize = true;
                } else if (temp.startsWith("min")) {
                    maximize = false;
                } else {
                    throw new Exception();
                }

                // Get whether to use measured or predicted class
                boolean useMeasured;
                temp = Options.get(3).toString().toLowerCase();
                if (temp.startsWith("meas")) {
                    useMeasured = true;
                } else if (temp.startsWith("pred")) {
                    useMeasured = false;
                } else {
                    throw new Exception();
                }

                // Get options
                List<Object> rankerOptions = Options.subList(4, Options.size());

                // Instantiate ranker
                ranker = (BaseEntryRanker) CommandHandler.instantiateClass(
                        "optimization.rankers." + rankerName,
                        rankerOptions);
                ranker.setUseMeasured(useMeasured);
                ranker.setMaximizeFunction(maximize);
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        // Set options
        setRanker(ranker);
        setNToPrint(nToPrint);
    }

    @Override
    public String printUsage() {
        return "Usage: [<ranker> <# to print> <maximize|minimize> <measured|predicted> [<ranker options...>]]";
    }

    /**
     * Define tool used to rank entries before printing. Set to null to print
     * data in the same order as the input dataset
     * @param ranker Desired ranker. Can be null
     */
    public void setRanker(BaseEntryRanker ranker) {
        this.Ranker = ranker;
    }

    /**
     * Define number of top entries to print. Only considered if the entries are
     * being ranker. Provide a negative number to print everything
     * @param nToPrint Number of top entries to print
     */
    public void setNToPrint(int nToPrint) {
        this.NToPrint = nToPrint;
    }

    @Override
    public void printHeader(Dataset data, OutputStream output) {
        // Make a print writer
        PrintWriter fp = new PrintWriter(output, true);
        
        // Perform ranking, if desired
        List<BaseEntry> entriesToPrint;
        if (Ranker != null) {
            // Get the ranks
            int[] ranks = Ranker.rankEntries(data);
            
            // Get the top entries
            entriesToPrint = new ArrayList<>(NToPrint > 0 ? NToPrint : data.NEntries());
            for (int rank : ArrayUtils.subarray(ranks, 0,
                    NToPrint < 0 ? data.NEntries() : NToPrint)) {
                entriesToPrint.add(data.getEntry(rank));
            }
        } else {
            entriesToPrint = data.getEntries();
        }
        
        // Get the length of the longest name
        EntryLength = 5; // Size of the word "Entry"
        for (BaseEntry entry : entriesToPrint) {
            EntryLength = Math.max(EntryLength, entry.toString().length());
        }
        
        // If ranking, get the number of digits in the largest rank
        NDigits = Integer.toString(NToPrint < 0 ? data.NEntries() : NToPrint).length();
        
        // Print the header
        if (Ranker != null) {
            fp.format(String.format("%%%ds  ", Math.max(4, NDigits)), "Rank");
        }
        fp.format(String.format("%%-%ds   Measured  Predicted", EntryLength), "Entry");
        fp.println();
        
        // If ranking, secretly print the entries
        if (Ranker != null) {
            actuallyPrintEntries(entriesToPrint, output);
        }
    }

    @Override
    public void printEntries(Collection<BaseEntry> entries, OutputStream output) {
        if (Ranker == null) {
            actuallyPrintEntries(entries, output);
        }
    }

    /**
     * Hidden method to print entries. This makes it possible for printEntries
     * to act differently if a ranker is used.
     * @param entries Entries to print
     * @param output Output
     */
    protected void actuallyPrintEntries(Collection<BaseEntry> entries, OutputStream output) {
        // Make print writer
        PrintWriter fp = new PrintWriter(output, true);
        
        // Tally of int count
        int count = 0;
        for (BaseEntry entry : entries) {
            // Print rank, if necessary
            if (Ranker != null) {
                fp.format(String.format("%%%dd  ", Math.max(4, NDigits)), 1 + count++);
            }
            
           // Print the entry name
           fp.format(String.format("%%%ds", EntryLength), entry.toString());
           
           // Print measured class, or None
            fp.format("  %9s", entry.hasMeasurement() ? String.format("%9g", entry.getMeasuredClass()) : "None");
           
           // Print predicted class, or None
            fp.format("  %9s", entry.hasPrediction() ? String.format("%9g", entry.getPredictedClass()) : "None");
           
           fp.println();
        }
    }
    
    @Override
    public void printEnd(OutputStream output) {
        // Nothing
    }
    
}
